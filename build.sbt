import scala.collection.Seq

ThisBuild / scalaVersion := "2.13.16"
ThisBuild / resolvers += DefaultMavenRepository

lazy val pekkoVersion = "1.2.0-M1"
lazy val pekkoHttpVersion = "1.2.0"
lazy val akkaKafkaVersion = "3.0.0"
lazy val cassandraDriverVersion = "4.19.0"
lazy val specs2Version = "4.21.0"

lazy val root = (project in file("."))
  .settings(
    name := "scala-kafka",
    version := "1.0",
    libraryDependencies ++= Seq(
      "org.apache.pekko" %% "pekko-actor" % pekkoVersion,
      "org.apache.pekko" %% "pekko-slf4j" % pekkoVersion,
      "org.apache.pekko" %% "pekko-stream" % pekkoVersion,
      "org.apache.pekko" %% "pekko-http" % pekkoHttpVersion,
      "org.apache.pekko" %% "pekko-testkit" % pekkoVersion,
      "org.apache.pekko" %% "pekko-connectors-kafka" % "1.1.0" excludeAll
        ExclusionRule(organization = "org.slf4j", name = "slf4j-api"),
      "org.apache.pekko" %% "pekko-stream-testkit" % pekkoVersion % Test,
      "org.apache.pekko" %% "pekko-connectors-kafka-testkit" % "1.1.0" % Test,
      "org.apache.pekko" %% "pekko-http-spray-json" % pekkoHttpVersion,
      "org.apache.pekko" %% "pekko-http-testkit" % pekkoHttpVersion % Test,

      "com.github.tototoshi" %% "scala-csv" % "2.0.0",
      "org.scala-lang.modules" %% "scala-java8-compat" % "1.0.2",
      "com.github.pureconfig" %% "pureconfig" % "0.17.9",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
      "ch.qos.logback" % "logback-classic" % "1.5.18",

      "org.apache.cassandra" % "java-driver-core" % cassandraDriverVersion,
      "org.apache.cassandra" % "java-driver-query-builder" % cassandraDriverVersion,

      "org.specs2" %% "specs2-core" % specs2Version,
      "org.specs2" %% "specs2-mock" % specs2Version))
