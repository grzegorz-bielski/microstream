name := "microstream"
version := "1.0"
scalaVersion := "3.0.0-M3"

lazy val akkaVersion = "2.6.12"
lazy val akkaHttpVersion = "10.2.3"

// scalacOptions ++= { if (isDotty.value) Seq("-source:3.0-migration") else Nil }

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-sharding-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.scalatest" %% "scalatest" % "3.1.0" % Test
).map(_.withDottyCompat(scalaVersion.value))
