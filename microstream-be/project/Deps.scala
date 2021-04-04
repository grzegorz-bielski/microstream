import sbt._

object Deps {
  val akkaVersion = "2.6.12"
  val akkaHttpVersion = "10.2.3"
  val slickVersion = "3.3.3"
  val circeVersion = "0.14.0-M3"
  val akkaProjectionVersion = "1.1.0"
  val akkaManagementVersion = "1.0.10"

  // core
  val akkaActorTyped = "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion
  val akkaActorTypedTestkit = "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test
  val akkStream = "com.typesafe.akka" %% "akka-stream" % akkaVersion
  val akkaStreamTyped = "com.typesafe.akka" %% "akka-stream-typed" % akkaVersion
  val logback = "ch.qos.logback" % "logback-classic" % "1.2.3"
  val scalatest = "org.scalatest" %% "scalatest" % "3.1.0" % Test
  val jackson = "com.typesafe.akka" %% "akka-serialization-jackson" % akkaVersion
  val circe = "io.circe" %% "circe-core" % circeVersion
  val circeGeneric = "io.circe" %% "circe-generic" % circeVersion
  val circeParser = "io.circe" %% "circe-parser" % circeVersion
  val typesafeConfig = "com.typesafe" % "config" % "1.4.1"
  val akkaClusterBootstrap = "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % akkaManagementVersion
  val akkaClusterHttp = "com.lightbend.akka.management" %% "akka-management-cluster-http" % akkaManagementVersion
  val akkaDiscoveryK8s = "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % akkaManagementVersion

  // http
  val akkaHttp = "com.typesafe.akka" %% "akka-http" % akkaHttpVersion
  val akkaCirce = "de.heikoseeberger" %% "akka-http-circe" % "1.35.3"
  val akkaHttpCors = "ch.megard" %% "akka-http-cors" % "1.1.1"

  // persistance
  val akkaPersistenceTyped = "com.typesafe.akka" %% "akka-persistence-typed" % akkaVersion
  val akkaPersistenceTestkit =
    "com.typesafe.akka" %% "akka-persistence-testkit" % akkaVersion % Test
  val akkVersion = "com.typesafe.akka" %% "akka-cluster-sharding-typed" % akkaVersion
  val akkaPersistanceJdbc = "com.lightbend.akka" %% "akka-persistence-jdbc" % "5.0.0"
  val akkaPersistanceQuery = "com.typesafe.akka" %% "akka-persistence-query" % akkaVersion
  val akkaProjectionES =
    "com.lightbend.akka" %% "akka-projection-eventsourced" % akkaProjectionVersion
  val akkProjectionSlick = "com.lightbend.akka" %% "akka-projection-slick" % akkaProjectionVersion
  val akkaProjectionTestkit =
    "com.lightbend.akka" %% "akka-projection-testkit" % akkaProjectionVersion % Test
  val slick = "com.typesafe.slick" %% "slick" % slickVersion
  val slick光 = "com.typesafe.slick" %% "slick-hikaricp" % slickVersion
  val flyway = "org.flywaydb" % "flyway-core" % "7.1.1"
  val postgresDriver = "org.postgresql" % "postgresql" % "42.2.18"

  // build-time
  val slickCodeGen = "com.typesafe.slick" %% "slick-codegen" % slickVersion

}
