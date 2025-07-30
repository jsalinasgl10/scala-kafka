package kafka.json

import kafka.entities.Player
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

object JsonFormat {

  import DefaultJsonProtocol._

  implicit val playerJsonFormat: RootJsonFormat[Player] = jsonFormat10(Player)
}
