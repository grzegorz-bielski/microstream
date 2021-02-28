ThisBuild / scalaVersion := "2.13.4"

import com.typesafe.config.ConfigFactory

lazy val akkaVersion = "2.6.12"
lazy val akkaHttpVersion = "10.2.3"
lazy val slickVersion = "3.3.3"
lazy val circeVersion = "0.14.0-M3"
lazy val akkaProjectionVersion = "1.1.0"

Compile / run / fork := true
// Compile / sourceGenerators <+=

lazy val root = (project in file("."))
  .settings(
    name := "microstream",
    version := "1.0",
    libraryDependencies ++= Seq(
      // core
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream-typed" % akkaVersion,
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "org.scalatest" %% "scalatest" % "3.1.0" % Test,
      "com.typesafe.akka" %% "akka-serialization-jackson" % akkaVersion,
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      // http
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "de.heikoseeberger" %% "akka-http-circe" % "1.35.3",
      // persistance
      "com.typesafe.akka" %% "akka-persistence-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-persistence-testkit" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-cluster-sharding-typed" % akkaVersion,
      "com.lightbend.akka" %% "akka-persistence-jdbc" % "5.0.0",
      "com.typesafe.akka" %% "akka-persistence-query" % akkaVersion,
      "com.lightbend.akka" %% "akka-projection-eventsourced" % akkaProjectionVersion,
      "com.lightbend.akka" %% "akka-projection-slick" % akkaProjectionVersion,
      "com.lightbend.akka" %% "akka-projection-testkit" % akkaProjectionVersion % Test,
      "com.typesafe.slick" %% "slick" % slickVersion,
      "com.typesafe.slick" %% "slick-hikaricp" % slickVersion,
      "com.typesafe.slick" %% "slick-codegen" % slickVersion,
      "org.flywaydb" % "flyway-core" % "7.1.1",
      "org.postgresql" % "postgresql" % "42.2.18"
    )
  )

lazy val c = ConfigFactory.load("base")

enablePlugins(FlywayPlugin)

flywayUrl := c.getString("slick.db.url")
flywayUser := c.getString("slick.db.user")
flywayPassword := c.getString("slick.db.password")
flywayLocations += "db/migration"

// lazy val slickGen = TaskKey[File]("auto generate slick tables from the existing DB schema")
// lazy val slickGenTask = {
//   val dir = sourceManaged.value
//   val classPath = (Compile / dependencyClasspath).value
//   val runner = (Compile / runner).value
//   val log = streams.value.log

//   val outputDir = (dir / "slick").getPath
//   val pkg = "generated"

//   val c = ConfigFactory.load("base")
//   val url = c.getString("slick.db.url")
//   val user = c.getString("slick.db.user")
//   val password = c.getString("slick.db.password")
//   val driver = c.getString("slick.db.driver")
//   val profile = c.getString("slick.profile").init

//   runner
//     .run(
//       "slick.codegen.SourceCodeGenerator",
//       classPath.files,
//       Array(profile, driver, url, outputDir, pkg),
//       log
//     )
//     .failed foreach (sys error _.getMessage)

//   file(outputDir + s"/$pkg/Tables.scala")
// }
