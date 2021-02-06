package com.microstream

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.ContentTypes
import scala.concurrent.ExecutionContextExecutor
import scala.util.{Success, Failure}

@main def start() = 
  given system: ActorSystem[Nothing] = ActorSystem(rootGuardian(), "root-guardian")
  given ExecutionContextExecutor = system.executionContext

  Http()
    .newServerAt("localhost", 8080)
    .bind(routes())
    .onComplete {
      case Success(binding) => 
        import binding.localAddress.{getHostString => host, getPort => port}

        system.log.info("Server online at http://{}:{}/", host, port)
      case Failure(err) => 
        system.log.error("Failed to start the server, terminating the actor system", err)
        system.terminate()
    }



def routes() = 
  import akka.http.scaladsl.server.Directives._

  val msg = "hi"

  path("session") {
      get {
        complete(
          HttpEntity(
            ContentTypes.`application/json`,
            s"""{ "msg": "$msg"}""""
          )
        )
      }
    }
  

def rootGuardian(): Behavior[Nothing] = Behaviors.setup[Nothing] { context =>
  context.log.info("started")

  Behaviors.empty
}