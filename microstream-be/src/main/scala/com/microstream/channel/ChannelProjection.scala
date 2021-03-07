package com.microstream.channel

import akka.projection.eventsourced.scaladsl.EventSourcedProvider
import akka.stream.typed.scaladsl.ActorSink
import akka.actor.typed.ActorSystem
import akka.persistence.jdbc.query.scaladsl.JdbcReadJournal
import akka.projection.slick.SlickProjection
import akka.projection.ProjectionId
import slick.basic.DatabaseConfig
import slick.driver.JdbcProfile
import scala.reflect.ClassTag
import akka.cluster.sharding.typed.scaladsl.ShardedDaemonProcess
import akka.projection.ProjectionBehavior
import akka.cluster.sharding.typed.ShardedDaemonProcessSettings
import akka.projection.slick.SlickHandler
import akka.projection.eventsourced.EventEnvelope
import akka.Done
import slick.dbio.DBIO
import org.slf4j.LoggerFactory
import scala.concurrent.ExecutionContext
import com.microstream.channel.ChannelStore.Event.Customized
import com.microstream.channel.ChannelStore.Event.Opened
import slick.jdbc.PostgresProfile

object ChannelProjection {
  val name = "ChannelProjection"

  private val logger = LoggerFactory.getLogger(getClass)

  def init(dbConfig: DatabaseConfig[PostgresProfile], repo: ChannelRepository)(implicit
      system: ActorSystem[_]
  ) = {
    implicit val ec: ExecutionContext = system.executionContext

    ShardedDaemonProcess(system).init(
      name,
      ChannelStore.tags.size,
      index => {
        val tag = ChannelStore.tags(index)
        val sourceProvider =
          EventSourcedProvider
            .eventsByTag[ChannelStore.Event](system, JdbcReadJournal.Identifier, tag)

        ProjectionBehavior(
          SlickProjection.exactlyOnce(
            ProjectionId(name, tag),
            sourceProvider,
            databaseConfig = dbConfig,
            handler = () => eventHandler(repo)
          )
        )
      },
      ShardedDaemonProcessSettings(system),
      Some(ProjectionBehavior.Stop)
    )
  }

  private def eventHandler(repo: ChannelRepository)(implicit ec: ExecutionContext) =
    SlickHandler[EventEnvelope[ChannelStore.Event]] { envelope =>
      envelope.event match {
        case Customized(_, name) => repo.updateChannel(name)
        case Opened(_, name)     => repo.updateChannel(name)
        case _                   => DBIO.successful(Done)
      }
    }
}
