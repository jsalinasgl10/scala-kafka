package kafka.cassandra

final case class CassandraConfig(keyspace: String,
                                 contactPoints: List[String],
                                 username: Option[String],
                                 password: Option[String],
                                 defaultIdempotence: Option[Boolean],
                                 fetchSize: Option[Int])
