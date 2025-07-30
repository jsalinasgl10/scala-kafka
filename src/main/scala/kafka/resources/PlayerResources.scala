package kafka.resources

import kafka.json.JsonFormat._
import kafka.views.PlayersView
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import org.apache.pekko.http.scaladsl.model._
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.Route
import spray.json.DefaultJsonProtocol._

import scala.concurrent.ExecutionContext

object PlayerResources {
  def apply(view: PlayersView)(implicit ec: ExecutionContext): Route = {

    val parameters = parameter("name")

    get {
      pathPrefix("players") {
        (pathEndOrSingleSlash & parameters) { name =>
          complete(view.findByFilters(name))
        } ~
          pathPrefix(Segment) { id =>
            complete(view.find(id))
          }
      }
    }
  }
}
