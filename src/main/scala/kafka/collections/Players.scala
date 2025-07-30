package kafka.collections

import com.datastax.oss.driver.api.core.cql.{PreparedStatement, Row}
import kafka.cassandra.CassandraSession
import kafka.collections.Players.Statements
import kafka.entities.Player

import scala.concurrent.{ExecutionContext, Future}

trait Players {
  def find(id: String)(implicit ec: ExecutionContext): Future[Option[Player]]
  def findByFilters(name: String)(implicit ec: ExecutionContext): Future[Iterable[Player]]
}

class DefaultPlayers(session: CassandraSession, statements: Statements)(implicit ec: ExecutionContext) extends Players {
  override def find(id: String)(implicit ec: ExecutionContext): Future[Option[Player]] = {
    session
      .findOne(statements.searchById.bind().setString("id", id))
      .map(_.fold(Option.empty[Player])(row => Some(fromDb(row))))
  }

  override def findByFilters(name: String)(implicit ec: ExecutionContext): Future[Iterable[Player]] =
    session.find(statements.searchByName.bind().setString("name", name)).map(_.map(fromDb))

  private[this] def fromDb(row: Row): Player =
    Player(
      id = row.getString("id"),
      name = row.getString("name"),
      dob = row.getString("dob"),
      nationality = Option(row.getString("nationality")),
      overall = row.getInt("overall"),
      club = row.getString("club"),
      value = row.getString("value"),
      foot = row.getString("foot"),
      number = row.getInt("number"),
      position = row.getString("position")
    )

}

object Players {
  final case class Statements(searchById: PreparedStatement, searchByName: PreparedStatement)

  def apply(session: CassandraSession)(implicit ec: ExecutionContext): Future[Players] = {

    val prepareSearchById   = session.prepare("SELECT * FROM players WHERE id = :id")
    val prepareSearchByName = session.prepare("SELECT * FROM players WHERE name = :name")

    for {
      searchById   <- prepareSearchById
      searchByName <- prepareSearchByName
    } yield new DefaultPlayers(session, Statements(searchById, searchByName))

  }
}
