package kafka.views

import kafka.collections.Players
import kafka.entities.Player

import scala.concurrent.{ExecutionContext, Future}

trait PlayersView {
  def find(id: String)(implicit ec: ExecutionContext): Future[Option[Player]]
  def findByFilters(name: String)(implicit ec: ExecutionContext): Future[Iterable[Player]]
}

class DefaultPlayersView(collection: Players) extends PlayersView {
  override def find(id: String)(implicit ec: ExecutionContext): Future[Option[Player]] =
    collection.find(id)

  override def findByFilters(name: String)(implicit ec: ExecutionContext): Future[Iterable[Player]] =
    collection.findByFilters(name)
}
