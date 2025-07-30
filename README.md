## Kafka and Cassandra DB integration sample

This project provides a Scala implementation to read from a Kafka topic storing some sample data in a Cassandra database.

Docker compose is used to configure and locally run instances of corresponding Kafka/Cassandra clusters. 
[App configuration file](docker-compose/docker-compose.yml) for reference.

### Kafka

The following command could be used to run a Kafka docker container and initialize the project sample data on startup.

`docker-compose -f ./docker-compose/docker-compose.yml up init-kafka`

Content of the file `player-data-full-2025_june_edited.csv` will be loaded in a new topic `players`. This file contains 18K footballers data used by the popular video game FIFA/FC 25
(credits to: https://www.kaggle.com/datasets/aniss7/fifa-player-data-from-sofifa-2025-06-03. The file has been modified by the author of this repository)

### Cassandra

The following command could be used to run the Cassandra docker container creating the players table on startup.

`docker-compose -f ./docker-compose/docker-compose.yml up init-cassandra`

### Scala application

Built using the [Apache Pekko framework](https://pekko.apache.org/docs/pekko/current/) (fork of Akka 2.6) to consume from Kafka and also provide a simple REST API to retrieve the sample data processed.

Integration with Cassandra database is done using the [Java Datastax driver](https://github.com/apache/cassandra-java-driver) implementation.

Application config file could be found [here](src/main/resources/application.conf)

```
cassandra {
    contact-points = ["localhost:9042"]
    keyspace = kafka
    username = "cassandra"
    password = "cassandra"
    default-consistency-level = "LOCAL_ONE"
    default-idempotence = true
    fetch-size = 100
}
```
```
kafka {
    broker = "localhost:9092"
    topics = [
    {
    source = "player"
    error  = "playerError"
    group-id = "players"
    consumers = 5
    paralellism = 10
    }
    ]
}
```
Five consumers are configured by default for the `player` topic. Note apart from this topic, another one is defined: `playerError`. That is because the strategy adopted when consuming a message from Kafka is publishing the message in this error topic if the processing fails for any reason.

[SBT](https://www.scala-sbt.org) can be used to run the application. 

`scala-kafka $ sbt run`

`Server online at at http://127.0.0.1:18000/`

### REST API samples

```json
curl 'http://localhost:18000/players/6'
{"club":"Real Madrid","dob":"1998-12-20","foot":"Right","id":"6","name":"Kylian Mbappé Lottin","nationality":"France","number":9,"overall":90,"position":"ST","value":"€160M"}
```

```json
curl 'http://localhost:18000/players?name=Declan%20Rice'
[{"club":"Arsenal","dob":"1999-01-14","foot":"Right","id":"27","name":"Declan Rice","nationality":"England","number":41,"overall":87,"position":"LCM","value":"€85M"}]
```