package com.microstream

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.SpawnProtocol
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.ContentTypes
import scala.concurrent.ExecutionContextExecutor
import scala.util.{Success, Failure}
import akka.stream.typed.scaladsl

import com.microstream.channel.Routes

@main def start() = 
  given system: ActorSystem[SpawnProtocol.Command] = ActorSystem(rootGuardian(), "root-guardian")
  given ExecutionContextExecutor = system.executionContext

  migrate()

  Http()
    .newServerAt("localhost", 8080)
    .bind(Routes())
    .onComplete {
      case Success(binding) => 
        import binding.localAddress.{getHostString => host, getPort => port}

        system.log.info("Server online at http://{}:{}/", host, port)
      case Failure(err) => 
        system.log.error("Failed to start the server, terminating the actor system", err)
        system.terminate()
    }

def rootGuardian() = Behaviors.setup[SpawnProtocol.Command] { context =>
  context.log.info("started")

  SpawnProtocol()
}


def migrate() = 
    import com.typesafe.config.ConfigFactory
    import org.flywaydb.core.Flyway

     val conf = ConfigFactory.load
     val (url, user, password) = (
       conf.getString("slick.db.url"), 
       conf.getString("slick.db.user"),
       conf.getString("slick.db.password")
     )

     Flyway
        .configure()
        .dataSource(url, user, password)
        .load()
        .migrate()
