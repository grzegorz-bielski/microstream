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

import com.typesafe.config.{ConfigFactory, Config}
import org.flywaydb.core.Flyway
import com.microstream.channel.ChannelGuardian

object Root extends App {
  lazy val config = ConfigFactory.load

  val clusterName = config.getString("clustering.cluster.name")

  ActorSystem[Nothing](RootGuardian(config), clusterName)
}

object RootGuardian {
  def apply(c: Config) = Behaviors.setup[Nothing] { context =>
    val cluster = Cluster(context.system)
    val roles = cluster.selfMember.roles

    context.log.info(s"Starting a new node with $roles role(s)")

    implicit val as: ActorSystem[Nothing] = context.system

    implicit val cg: ActorRef[ChannelGuardian.Message] =
      context.spawn(ChannelGuardian(ChannelNode.Role), "channel-guardian")

    roles collect {
      case ChannelNode.Role => ChannelNode.migrate(c)
      case HttpNode.Role    => HttpNode.startServer()
    }

    if (isSeedNode(c)) {
      context.spawn(ClusterObserver(), "cluster-observer")
    }

    Behaviors.empty
  }

  def isSeedNode(c: Config) = {
    val clusterPort = c.getInt("clustering.port")
    val defaultPort = c.getInt("clustering.defaultPort")

    clusterPort == defaultPort
  }

}

object HttpNode {
  val Role = "http"

  def startServer()(implicit
      system: ActorSystem[_],
      chanGuardian: ActorRef[ChannelGuardian.Message]
  ) = {
    implicit val ex: ExecutionContextExecutor = system.executionContext

    Http()
      .newServerAt("localhost", 8080)
      .bind(RootController())
      .onComplete {
        case Success(binding) =>
          import binding.localAddress.{getHostString => host, getPort => port}

          system.log.info("Http server online at http://{}:{}/", host, port)
        case Failure(err) =>
          system.log.error(
            "Failed to start the Http server, terminating the actor system",
            err
          )
          system.terminate()
      }
  }
}

object ChannelNode {
  val Role = "channel"

  def migrate(c: Config) = {
    val (url, user, password) = (
      c.getString("slick.db.jdbcUrl"),
      c.getString("slick.db.user"),
      c.getString("slick.db.password")
    )

    Flyway
      .configure()
      .dataSource(url, user, password)
      .load()
      .migrate()
  }
}
