package kafka.consumer

import com.typesafe.scalalogging.LazyLogging
import kafka.config.Config.TopicGroup
import kafka.loader.Loader
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import org.apache.pekko.Done
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.kafka.ConsumerMessage.{CommittableMessage, CommittableOffset}
import org.apache.pekko.kafka.ProducerMessage.Envelope
import org.apache.pekko.kafka.scaladsl.Consumer.DrainingControl
import org.apache.pekko.kafka.scaladsl.{Committer, Consumer, Producer}
import org.apache.pekko.kafka._

import scala.concurrent.{ExecutionContext, Future}

trait KafkaConsumer[T] extends LazyLogging {

  protected[this] def broker: String
  protected[this] def topics: TopicGroup
  protected[this] def loader: Loader[T]

  protected[this] def control: DrainingControl[Done]

  protected[this] def receive(message: CommittableMessage[String, String])(
      implicit ec: ExecutionContext): Future[Envelope[String, String, CommittableOffset]] = {
    logger.debug("Received message: {}", message.record.value())
    loader
      .process(message.record.value())
      .map[Envelope[String, String, CommittableOffset]] { _ =>
        ProducerMessage.passThrough(message.committableOffset)
      }
      .recover {
        case _ =>
          ProducerMessage.single(new ProducerRecord(topics.error, message.record.key, message.record.value),
                                 message.committableOffset)
      }
  }

  def stop()(implicit ec: ExecutionContext): Future[Done] = control.drainAndShutdown

}

final class DefaultKafkaConsumer[T](val broker: String, val topics: TopicGroup, val loader: Loader[T])(
    implicit ec: ExecutionContext,
    system: ActorSystem)
    extends KafkaConsumer[T]
    with LazyLogging {

  private[this] val paralellism = topics.paralellism.getOrElse(1)

  private[this] val sourceSettings = ConsumerSettings
    .create(system, new StringDeserializer, new StringDeserializer)
    .withBootstrapServers(broker)
    .withGroupId(topics.groupId)
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  private[this] val flowSettings = ProducerSettings
    .create(system, new StringSerializer, new StringSerializer)
    .withBootstrapServers(broker)

  override protected[this] val control: DrainingControl[Done] =
    Consumer
      .committableSource(sourceSettings, Subscriptions.topics(topics.source))
      .mapAsync(paralellism)(receive)
      .via(Producer.flexiFlow(flowSettings))
      .map(_.passThrough)
      .toMat(Committer.sink(CommitterSettings(system)))(DrainingControl.apply)
      .run
}

object KafkaConsumer {
  def apply[T](broker: String, topics: TopicGroup, loader: Loader[T])(implicit ec: ExecutionContext,
                                                                      system: ActorSystem) =
    new DefaultKafkaConsumer(broker, topics, loader)(ec, system)
}
