name := "microstream"
version := "1.0"
scalaVersion := "3.0.0-M3"

lazy val akkaVersion = "2.6.12"
lazy val akkaHttpVersion = "10.2.3"
lazy val slickVersion = "3.3.3"

// scalacOptions ++= { if (isDotty.value) Seq("-source:3.0-migration") else Nil }

libraryDependencies ++= Seq(
  // core
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-typed" % akkaVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.scalatest" %% "scalatest" % "3.1.0" % Test,
  // http
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "de.heikoseeberger" %% "akka-http-circe" % "1.31.0",
  // db
  "com.typesafe.akka" %% "akka-persistence-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-serialization-jackson" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-sharding-typed" % akkaVersion,
  "com.lightbend.akka" %% "akka-persistence-jdbc" % "5.0.0",
  "com.typesafe.akka" %% "akka-persistence-query" % akkaVersion,
  "com.typesafe.slick" %% "slick" % slickVersion,
  "com.typesafe.slick" %% "slick-hikaricp" % slickVersion,
  "org.flywaydb" % "flyway-core" % "7.1.1",
  "org.postgresql" % "postgresql" % "42.2.18"
).map(_.withDottyCompat(scalaVersion.value))
