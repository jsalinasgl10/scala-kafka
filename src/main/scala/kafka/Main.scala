package kafka

import com.typesafe.scalalogging.LazyLogging
import config.Config
import kafka.cassandra.CassandraSession
import kafka.collections.Players
import kafka.consumer.KafkaConsumer
import kafka.loader.PlayerLoader
import kafka.resources.PlayerResources
import kafka.views.DefaultPlayersView
import org.apache.pekko.Done
import org.apache.pekko.actor.{ActorSystem, CoordinatedShutdown}
import org.apache.pekko.http.scaladsl.Http

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object Main extends LazyLogging {

  def main(args: Array[String]): Unit = {

    implicit val system: ActorSystem = ActorSystem("scala-kafka-server")
    import system.dispatcher

    val config = Config()

    //Initializing cassandra session
    val cassandraSession = CassandraSession(config.cassandra)

    //Initializing Cassandra loader and DB handler for players
    for {
      repository <- Players(cassandraSession)
      loader     <- PlayerLoader(cassandraSession)
    } yield {
      //Initializing consumers for players topic
      val playersTopic = config.kafka.topics.head
      val numConsumers = playersTopic.consumers.getOrElse(1)
      logger.info("Creating {} consumers for topic: {}", numConsumers, playersTopic.source)
      val consumers = 1 to numConsumers map { _ =>
        KafkaConsumer(config.kafka.broker, playersTopic, loader)
      }

      //to release resources on app shutdown
      CoordinatedShutdown(system).addTask(CoordinatedShutdown.PhaseServiceStop, "stop-resources") { () =>
        Future {
          logger.info("Shutting down kafka consumers")
          consumers.foreach(_.stop())
          logger.info("Releasing cassandra session")
          cassandraSession.close()
          Done
        }
      }

      val resources = PlayerResources(new DefaultPlayersView(repository))

      Http(system).newServerAt(config.interface, config.port).bindFlow(resources)

      logger.info(s"Server online at at http://{}:{}/", config.interface, config.port)

      Await.result(system.whenTerminated, Duration.Inf)

    }
  }
}
