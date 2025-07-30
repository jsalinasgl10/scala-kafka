package kafka.config

import kafka.cassandra.CassandraConfig
import kafka.config.Config.Kafka
import pureconfig.ConfigSource
import pureconfig.generic.auto._

final case class Config(interface: String, port: Int, kafka: Kafka, cassandra: CassandraConfig)

object Config {
  final case class Kafka(broker: String, topics: Iterable[TopicGroup])

  final case class TopicGroup(source: String,
                              error: String,
                              groupId: String,
                              consumers: Option[Int],
                              paralellism: Option[Int])

  def apply(): Config = Config("config")

  def apply(namespace: String): Config = ConfigSource.default.at(namespace).loadOrThrow[Config]
}
