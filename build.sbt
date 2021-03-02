ThisBuild / scalaVersion := "2.13.4"

import com.typesafe.config.{ConfigFactory, Config}

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
      Deps.akkaActorTyped,
      Deps.akkaActorTypedTestkit,
      Deps.akkStream,
      Deps.akkaStreamTyped,
      Deps.logback,
      Deps.scalatest,
      Deps.jackson,
      Deps.circe,
      Deps.circeGeneric,
      Deps.circeParser,
      Deps.akkaHttp,
      Deps.akkaCirce,
      Deps.akkaPersistenceTyped,
      Deps.akkaPersistenceTestkit,
      Deps.akkVersion,
      Deps.akkaPersistanceJdbc,
      Deps.akkaPersistanceQuery,
      Deps.akkaProjectionES,
      Deps.akkProjectionSlick,
      Deps.akkaProjectionTestkit,
      Deps.slick,
      Deps.slick光,
      Deps.slickCodeGen,
      Deps.flyway,
      Deps.postgresDriver
    ),
    (Compile / sourceGenerators) += slickGen
  )

lazy val conf = settingKey[Config]("base app config")

conf := {
  val resourceDir = (Compile / resourceDirectory).value
  val appConfig = ConfigFactory.parseFile(resourceDir / "base.conf")
  ConfigFactory.load(appConfig)
}

enablePlugins(FlywayPlugin)

flywayUrl := conf.value.getString("slick.db.url")
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

  val url = c.getString("slick.db.url")
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
