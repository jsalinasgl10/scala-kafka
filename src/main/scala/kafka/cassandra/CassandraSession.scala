package kafka.cassandra

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.config.{DefaultDriverOption, DriverConfigLoader}
import com.datastax.oss.driver.api.core.cql.{PreparedStatement, Row, SimpleStatement, Statement}
import com.typesafe.scalalogging.LazyLogging
import kafka.error.AppExceptions.CassandraException

import scala.compat.java8.FutureConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

trait CassandraSession extends LazyLogging {

  protected[this] def session: CqlSession

  def prepare(statement: SimpleStatement): Future[PreparedStatement] =
    session.prepareAsync(statement).toScala

  def prepare(query: String): Future[PreparedStatement] =
    session.prepareAsync(query).toScala

  def execute[A <: Statement[A]](statement: Statement[A])(implicit ec: ExecutionContext): Future[Unit] = {
    session.executeAsync(statement).toScala.map { rs =>
      if (!rs.wasApplied)
        throw CassandraException(s"Executing statement $statement was not applied")
      ()
    }

  }

  def find[A <: Statement[A]](statement: Statement[A])(implicit ec: ExecutionContext): Future[Iterable[Row]] =
    session.executeAsync(statement).toScala.map { res =>
      res.currentPage().asScala
    }

  def findOne[A <: Statement[A]](statement: Statement[A])(implicit ec: ExecutionContext): Future[Option[Row]] = {
    session.executeAsync(statement).toScala.map { resultSet =>
      Option(resultSet.one())
    }
  }

  def close()(implicit ec: ExecutionContext): Future[Unit] =
    session.closeAsync().toScala.map(_ => ())
}

class DefaultCassandraSession(val session: CqlSession) extends CassandraSession

object CassandraSession extends LazyLogging {

  def apply(cluster: CassandraConfig): CassandraSession = {

    val builder = CqlSession.builder()

    builder.withKeyspace(cluster.keyspace)
    for {
      username <- cluster.username
      password <- cluster.password
    } yield {
      builder.withAuthCredentials(username, password)
    }

    val configBuilder = DriverConfigLoader.programmaticBuilder()

    cluster.defaultIdempotence.map(configBuilder.withBoolean(DefaultDriverOption.REQUEST_DEFAULT_IDEMPOTENCE, _))
    cluster.fetchSize.map(configBuilder.withInt(DefaultDriverOption.REQUEST_PAGE_SIZE, _))

    builder.withConfigLoader(configBuilder.build())

    val session = builder.build()

    val profile = session.getContext.getConfig.getDefaultProfile
    logger.info(
      "New cassandra session opened -> protocol-version: {} default-idempotence: {} consistency-level: {} fetch-size: {}",
      session.getContext.getProtocolVersion.toString,
      profile.getBoolean(DefaultDriverOption.REQUEST_DEFAULT_IDEMPOTENCE),
      profile.getString(DefaultDriverOption.REQUEST_CONSISTENCY),
      profile.getInt(DefaultDriverOption.REQUEST_PAGE_SIZE)
    )

    new DefaultCassandraSession(session)

  }
}
