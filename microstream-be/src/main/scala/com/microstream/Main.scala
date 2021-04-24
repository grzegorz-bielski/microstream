package com.microstream

import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import scala.concurrent.ExecutionContextExecutor
import scala.util.{Success, Failure}
import akka.actor.typed.scaladsl.ActorContext
import akka.cluster.typed.Cluster
import com.typesafe.config.{ConfigFactory, Config}
import org.flywaydb.core.Flyway
import com.microstream.channel.ChannelGuardian
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Routers
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement

object Root extends App {
  lazy val config = ConfigFactory.load

  val clusterName = config.getString("clustering.cluster.name")

  ActorSystem[Nothing](RootGuardian(config), clusterName)
}

object RootGuardian {
  def apply(c: Config) = Behaviors.setup[Nothing] { implicit context =>
    val cluster = Cluster(context.system)
    val roles = cluster.selfMember.roles

    context.log.info(s"Starting a new node with $roles role(s)")

    if (roles.exists(_ == ChannelNode.Role)) ChannelNode(c)
    if (roles.exists(_ == HttpNode.Role)) HttpNode(c)
    if (isSeedNode(c)) context.spawn(ClusterObserver(), "cluster-observer")
    if (roles.exists(_ == K8sExtensions.Role)) K8sExtensions()

    def isSeedNode(c: Config) = {
      val port = c.getString("clustering.port")
      val defaultPort = c.getString("clustering.defaultPort")

      context.log.info(s"Port: $port, Default port: $defaultPort")

      port == defaultPort
    }

    Behaviors.empty
  }

}

/** `HttpNode` is responsible for handling HTTP requests and WS connections through the `Session` actor.
  * Actual work is delegated to the `ChannelGuardian` actors through cluster aware routers in a round-robin fashion
  */
object HttpNode {
  val Role = "http"

  def apply(c: Config)(implicit ctx: ActorContext[_]) = {
    implicit val system: ActorSystem[_] = ctx.system
    implicit val ex: ExecutionContextExecutor = system.executionContext
    implicit val r: ActorRef[ChannelGuardian.Message] =
      ctx.spawn(
        Routers.group(ChannelGuardian.Key).withRoundRobinRouting,
        "channel-guardian-group"
      )

    Http()
      .newServerAt("0.0.0.0", 8080)
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

/** `ChannelNode` is responsible for managing the state of `Channel` actors through `ChannelGuardian`(s)
  * which are registered through receptionist and available in the whole cluster
  */
object ChannelNode {
  val Role = "channel"

  def apply(c: Config)(implicit ctx: ActorContext[_]) = {
    ChannelNode.migrate(c)

    val channelGuardian = ctx.spawn(ChannelGuardian(Role), "channel-guardian")
    ctx.system.receptionist ! Receptionist.Register(ChannelGuardian.Key, channelGuardian)
  }

  private def migrate(c: Config) = {
    // todo: migrate to pureconfig
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

/** Setup needed to form a cluster inside K8s.
  * Also requires RBAC roles and bindings:
  * https://doc.akka.io/docs/akka-management/current/kubernetes-deployment/forming-a-cluster.html#role-based-access-control
  */
object K8sExtensions {
  val Role = "k8s"

  def apply()(implicit ctx: ActorContext[_]) = {
    val system = ctx.system

    AkkaManagement(system).start()
    ClusterBootstrap(system).start()
  }
}
