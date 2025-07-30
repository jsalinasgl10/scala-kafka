package kafka

import kafka.collections.Players
import kafka.entities.Player
import kafka.json.JsonFormat._
import kafka.resources.PlayerResources
import kafka.views.DefaultPlayersView
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.http.scaladsl.testkit.Specs2RouteTest
import org.mockito.Mockito.when
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import spray.json.DefaultJsonProtocol._

import scala.concurrent.Future

class PlayerResourcesSpec extends Specification with Specs2RouteTest with Mockito {

  val player: Player = Player(id = "id",
                              name = "name",
                              dob = "dob",
                              nationality = Some("nationality"),
                              overall = 99,
                              club = "club",
                              value = "value",
                              foot = "foot",
                              number = 10,
                              position = "position")

  val mockPlayers: Players = mock[Players]
  when(mockPlayers.find(player.id)).thenReturn(Future.successful(Some(player)))
  when(mockPlayers.findByFilters(player.name)).thenReturn(Future.successful(Some(player)))

  val basePath      = "/players"
  val routes: Route = PlayerResources(new DefaultPlayersView(mockPlayers))

  basePath should {
    s"GET player by id" in {
      Get(s"$basePath/${player.id}") ~> routes ~> check {
        status shouldEqual StatusCodes.OK
        entityAs[Player].id shouldEqual player.id
      }
    }
    s"GET player by param name" in {
      Get(s"$basePath?name=${player.name}") ~> routes ~> check {
        status shouldEqual StatusCodes.OK
        entityAs[Iterable[Player]].head.name shouldEqual player.name
      }
    }
  }
}
