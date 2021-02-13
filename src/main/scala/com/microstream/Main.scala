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
import akka.actor.typed.scaladsl.ActorContext
import akka.cluster.typed.Cluster
import com.microstream.channel.ChannelStore
import com.microstream.channel.Channel

import com.typesafe.config.ConfigFactory
import org.flywaydb.core.Flyway

lazy val config = ConfigFactory.load

// https://github.com/akka/akka-sample-cluster-docker-compose-scala/blob/master/src/main/resources/application.conf
// https://www.freecodecamp.org/news/how-to-make-a-simple-application-with-akka-cluster-506e20a725cf/

@main def start() =
  val clusterName = config.getString("clustering.cluster.name")

  println(config.toString())

  ActorSystem(RootBehavior(), clusterName)

def RootBehavior() = Behaviors.setup[Nothing] { context =>
  given ActorContext[Nothing] = context
  given ActorSystem[Nothing] = context.system

  val cluster = Cluster(context.system)
  val roles = cluster.selfMember.roles

  context.log.info(s"Starting a new node with $roles role(s)")

  ChannelStore.initSharding(ChannelNode.Role)

  roles collect {
    case ChannelNode.Role => ChannelNode.migrate()
    case HttpNode.Role    => HttpNode.startServer()
  }
  
  if (isSeedNode()) {
    context.spawn(ClusterObserver(), "cluster-observer")
  }

  Behaviors.empty
}

object HttpNode:
  val Role = "http"

  def startServer()(using ctx: ActorContext[Nothing], system: ActorSystem[_]) =
    given ExecutionContextExecutor = system.executionContext

    Http()
      .newServerAt("localhost", 8080)
      .bind(RootController(ctx))
      .onComplete {
        case Success(binding) => 
          import binding.localAddress.{getHostString => host, getPort => port}

          system.log.info("Http server online at http://{}:{}/", host, port)
        case Failure(err) => 
          system.log.error("Failed to start the Http server, terminating the actor system", err)
          system.terminate()
      }

object ChannelNode:
  val Role = "channel"  

  def migrate() =      
    val (url, user, password) = (
      config.getString("slick.db.url"), 
      config.getString("slick.db.user"),
      config.getString("slick.db.password")
    )

    Flyway
      .configure()
      .dataSource(url, user, password)
      .load()
      .migrate()

def isSeedNode() =
  val clusterPort = config.getInt ("clustering.port")
  val defaultPort = config.getInt ("clustering.defaultPort")

  clusterPort == defaultPort
