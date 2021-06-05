import com.typesafe.config.{ConfigFactory, Config}

enablePlugins(
  FlywayPlugin,
  JavaAppPackaging,
  AshScriptPlugin
)

ThisBuild / scalaVersion := "2.13.5"
ThisBuild / version := "0.0.1"

val akkaVersion = "2.6.12"
val akkaHttpVersion = "10.2.3"
val slickVersion = "3.3.3"
val circeVersion = "0.14.0-M3"
val akkaProjectionVersion = "1.1.0"
val akkaManagementVersion = "1.0.10"

lazy val root = (project in file("."))
  .settings(
    libraryDependencies ++= Seq(
      // akka
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-serialization-jackson" % akkaVersion,
      // akka stream
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream-typed" % akkaVersion,
      // akka cluster
      "com.typesafe.akka" %% "akka-cluster-sharding-typed" % akkaVersion,
      "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % akkaManagementVersion,
      "com.lightbend.akka.management" %% "akka-management-cluster-http" % akkaManagementVersion,
      "com.typesafe.akka" %% "akka-discovery" % akkaVersion,
      "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % akkaManagementVersion,
      // akka http
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "de.heikoseeberger" %% "akka-http-circe" % "1.35.3",
      "ch.megard" %% "akka-http-cors" % "1.1.1",
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion, // transitive dep
      // akka state persistence
      "com.typesafe.akka" %% "akka-persistence-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-persistence-testkit" % akkaVersion % Test,
      "com.lightbend.akka" %% "akka-persistence-jdbc" % "5.0.0",
      "com.typesafe.akka" %% "akka-persistence-query" % akkaVersion,
      "com.lightbend.akka" %% "akka-projection-eventsourced" % akkaProjectionVersion,
      "com.lightbend.akka" %% "akka-projection-slick" % akkaProjectionVersion,
      "com.lightbend.akka" %% "akka-projection-testkit" % akkaProjectionVersion % Test,
      // db
      "com.typesafe.slick" %% "slick" % slickVersion,
      "com.typesafe.slick" %% "slick-hikaricp" % slickVersion,
      "com.typesafe.slick" %% "slick-codegen" % slickVersion,
      "org.flywaydb" % "flyway-core" % "7.1.1",
      "org.postgresql" % "postgresql" % "42.2.18",
      // misc
      "com.typesafe" % "config" % "1.4.1",
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "io.circe" %% "circe-core" % circeVersion,
      "org.scalatest" %% "scalatest" % "3.1.0" % Test
    )
    // uncomment to code-gen on `compile`
    // (Compile / sourceGenerators) += slickGen
  )

// DB tasks

lazy val conf = settingKey[Config]("base app config")

conf := {
  val resourceDir = (Compile / resourceDirectory).value
  val appConfig = ConfigFactory.parseFile(resourceDir / "base.conf")
  ConfigFactory.load(appConfig)
}

flywayUrl := conf.value.getString("slick.db.jdbcUrl")
flywayUser := conf.value.getString("slick.db.user")
flywayPassword := conf.value.getString("slick.db.password")
flywayLocations += "db/migration"

lazy val slickGen = taskKey[Seq[File]]("auto generate slick tables from the existing DB schema")

slickGen := {
  val dir = (Compile / scalaSource).value
  val classPath = (Compile / dependencyClasspath).value
  val run = (Compile / runner).value.run _
  val log = streams.value.log
  val c = conf.value

  val outputDir = dir
  val lib = "slick"
  val pkg = "generated"

  val url = c.getString("slick.db.jdbcUrl")
  val user = c.getString("slick.db.user")
  val password = c.getString("slick.db.password")
  val driver = c.getString("slick.db.driver")
  val profile = c.getString("slick.profile").init

  // todo: use Options for PKs: https://stackoverflow.com/questions/22275022/customizing-slick-generator
  run(
    "slick.codegen.SourceCodeGenerator",
    classPath.files,
    Seq(profile, driver, url, outputDir.getPath, lib ++ "." ++ pkg, user, password),
    log
  ).failed foreach (sys error _.getMessage)

  Seq(outputDir / lib / pkg / "Tables.scala")
}

/// packaging
packageName := "microstream-be"
dockerBaseImage := "openjdk:8-jre-alpine"
dockerExposedPorts ++= Seq(
  // todo: take it from config instead
  2552, // clustering
  8080, // http,
  8558, // akka management
  5432 // db
)
dockerAliases := Seq(
  DockerAlias(
    registryHost = Some("localhost:5000"),
    username = None,
    name = packageName.value,
    tag = Some("latest")
  )
)
