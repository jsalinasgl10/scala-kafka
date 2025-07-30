package kafka.loader

import com.datastax.oss.driver.api.core.cql.PreparedStatement
import com.datastax.oss.driver.api.querybuilder.QueryBuilder._
import kafka.cassandra.CassandraSession
import kafka.entities.Player
import kafka.loader.PlayerLoader.PlayerStatements
import kafka.parser.CsvParser
import org.apache.pekko.http.scaladsl.model.DateTime

import scala.concurrent.{ExecutionContext, Future}

final case class PlayerLoader(session: CassandraSession, statements: PlayerStatements) extends Loader[Player] {

  override def store(entity: Player)(implicit ec: ExecutionContext): Future[Unit] = {
    val stmt = statements.insertPlayer
      .bind()
      .setString("id", entity.id)
      .setString("name", entity.name)
      .setString("dob", entity.dob)
      .setInt("overall", entity.overall)
      .setString("nationality", entity.nationality.getOrElse("unknown"))
      .setString("club", entity.club)
      .setString("value", entity.value)
      .setString("foot", entity.foot)
      .setInt("number", entity.number)
      .setString("position", entity.position)
      .setQueryTimestamp(DateTime.now.clicks)

    session.execute(stmt)
  }

  override def parse(line: String): Player = {
    val fields = CsvParser.parse(line)
    Player(
      id = fields.head,
      name = fields(1),
      dob = fields(2),
      nationality = Option(fields(3)),
      overall = fields(4).toInt,
      club = fields(5),
      value = fields(6),
      foot = fields(7),
      number = fields(8).toInt,
      position = fields(9)
    )
  }
}

object PlayerLoader {
  final case class PlayerStatements(insertPlayer: PreparedStatement)

  def apply(session: CassandraSession)(implicit ec: ExecutionContext): Future[PlayerLoader] = {

    val prepareInsertPlayer = session.prepare(
      insertInto("players")
        .value("id", bindMarker("id"))
        .value("name", bindMarker("name"))
        .value("dob", bindMarker("dob"))
        .value("club", bindMarker("club"))
        .value("foot", bindMarker("foot"))
        .value("nationality", bindMarker("nationality"))
        .value("number", bindMarker("number"))
        .value("overall", bindMarker("overall"))
        .value("position", bindMarker("position"))
        .value("value", bindMarker("value"))
        .usingTimestamp(bindMarker("timestamp"))
        .build())

    prepareInsertPlayer.map(insertPlayer => PlayerLoader(session, PlayerStatements(insertPlayer)))
  }

}
