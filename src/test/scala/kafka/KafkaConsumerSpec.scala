package kafka

import kafka.KafkaConsumerSpec.{KafkaConsumerConfigSpec, MockedKafkaConsumer}
import kafka.config.Config.{Kafka, TopicGroup}
import kafka.consumer.KafkaConsumer
import kafka.entities.Player
import kafka.loader.Loader
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.pekko.Done
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.kafka.CommitterSettings
import org.apache.pekko.kafka.ConsumerMessage.{CommittableMessage, CommittableOffset}
import org.apache.pekko.kafka.ProducerMessage.{Envelope, Message, PassThroughMessage, Results}
import org.apache.pekko.kafka.scaladsl.Committer
import org.apache.pekko.kafka.scaladsl.Consumer.DrainingControl
import org.apache.pekko.kafka.testkit.scaladsl.ConsumerControlFactory
import org.apache.pekko.kafka.testkit.{ConsumerResultFactory, ProducerResultFactory}
import org.apache.pekko.stream.scaladsl.{Flow, Keep, Source}
import org.mockito.Mockito.when
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

final class KafkaConsumerSpec extends Specification {

  implicit val system: ActorSystem                = ActorSystem(getClass.getSimpleName)
  implicit val executionEnv: ExecutionEnv         = ExecutionEnv.fromExecutionContext(system.dispatcher)
  implicit val executionContext: ExecutionContext = executionEnv.executionContext
  implicit val ec: ExecutionContextExecutor       = scala.concurrent.ExecutionContext.global

  val message = "id;name;dob;nationality;rating;club;value;foot;number;position"

  val config = new KafkaConsumerConfigSpec[Player](message)

  "KafkaConsumer for Player" should {
    "process csv message" in {
      new MockedKafkaConsumer(config.server.broker, config.server.topics.head, config.mockedLoader, config.messages).control.streamCompletion
        .map(_.toString) must be("Done")
        .await(1, 5.seconds)
    }
    "process csv failing message" in {
      new MockedKafkaConsumer(config.server.broker, config.server.topics.head, config.mockedLoader, config.messages).control.streamCompletion
        .map(_.toString) must be("Done")
        .await(1, 5.seconds)
    }
  }
}

object KafkaConsumerSpec {

  class KafkaConsumerConfigSpec[T](val csv: String) extends Mockito {

    val topics: TopicGroup =
      TopicGroup(source = "topic", error = "topicError", groupId = "group", consumers = None, paralellism = None)
    val server: Kafka = Kafka(broker = "localhost:9092", topics = Iterable(topics))

    val messages: collection.immutable.Iterable[CommittableMessage[String, String]] =
      collection.immutable.Iterable(
        ConsumerResultFactory.committableMessage(
          new ConsumerRecord(topics.source, 0, 1, "key", csv),
          ConsumerResultFactory.committableOffset(topics.groupId, topics.source, 0, 1, csv)))

    val mockedLoader: Loader[T]      = mock[Loader[T]]
    val mockedLoaderError: Loader[T] = mock[Loader[T]]

    when(mockedLoader.process(anyString)(any[ExecutionContext]()))
      .thenReturn(Future.successful(Done))
    when(mockedLoaderError.process(anyString)(any[ExecutionContext]()))
      .thenReturn(Future.failed(new Exception("exception")))
  }

  class MockedKafkaConsumer[T](val broker: String,
                               val topics: TopicGroup,
                               val loader: Loader[T],
                               messages: collection.immutable.Iterable[CommittableMessage[String, String]])(
      implicit ec: ExecutionContext,
      system: ActorSystem)
      extends KafkaConsumer[T] {

    private[this] val mockedKafkaConsumer = Source(messages).viaMat(ConsumerControlFactory.controlFlow())(Keep.right)

    private[this] val mockedKafkaProducer = Flow[Envelope[String, String, CommittableOffset]]
      .map[Results[String, String, CommittableOffset]] {
        case msg: Message[String, String, CommittableOffset] =>
          ProducerResultFactory.result(msg)
        case pass: PassThroughMessage[String, String, CommittableOffset] =>
          ProducerResultFactory.passThroughResult(pass.passThrough)
        case other =>
          throw new Exception(s"excluded: $other")
      }

    override val control: DrainingControl[Done] =
      mockedKafkaConsumer
        .mapAsync(topics.paralellism.getOrElse(1))(receive)
        .via(mockedKafkaProducer)
        .map(_.passThrough)
        .toMat(Committer.sink(CommitterSettings(system)))(DrainingControl.apply)
        .run()
  }
}
