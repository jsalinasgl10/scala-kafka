package kafka

import com.datastax.oss.driver.api.core.cql.PreparedStatement
import kafka.cassandra.CassandraSession
import kafka.entities.Player
import kafka.loader.PlayerLoader
import kafka.loader.PlayerLoader.PlayerStatements
import org.apache.pekko.actor.ActorSystem
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

class PlayerLoaderSpec extends Specification with Mockito {

  implicit val system: ActorSystem                = ActorSystem(getClass.getSimpleName)
  implicit val executionEnv: ExecutionEnv         = ExecutionEnv.fromExecutionContext(system.dispatcher)
  implicit val executionContext: ExecutionContext = executionEnv.executionContext
  implicit val ec: ExecutionContextExecutor       = scala.concurrent.ExecutionContext.global

  val message = "id;name;dob;nationality;99;club;value;foot;10;position"

  private[this] val player = Player(id = "id",
                                    name = "name",
                                    dob = "dob",
                                    nationality = Some("nationality"),
                                    overall = 99,
                                    club = "club",
                                    value = "value",
                                    foot = "foot",
                                    number = 10,
                                    position = "position")

  val loader: PlayerLoader = PlayerLoader(mock[CassandraSession], PlayerStatements(mock[PreparedStatement]))

  "PlayerLoader" should {
    "parse player message" in {
      loader.parse(message) shouldEqual player
    }
  }

}
