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

object ChannelProjection {
  val name = "ChannelProjection"

  private val logger = LoggerFactory.getLogger(getClass)

  def init()(implicit
      system: ActorSystem[_]
  ) = {

    ShardedDaemonProcess(system).init(
      name,
      ChannelStore.tags.size,
      index => ProjectionBehavior(createProjectionFor(index, ???)),
      ShardedDaemonProcessSettings(system),
      Some(ProjectionBehavior.Stop)
    )
  }

  private def createProjectionFor[P <: JdbcProfile: ClassTag](
      tagIndex: Int,
      dbConfig: DatabaseConfig[P]
  )(implicit
      system: ActorSystem[_]
  ) = {
    val tag = ChannelStore.tags(tagIndex)
    val sourceProvider =
      EventSourcedProvider.eventsByTag[ChannelStore.Event](system, JdbcReadJournal.Identifier, tag)

    SlickProjection.exactlyOnce(
      ProjectionId(name, tag),
      sourceProvider,
      databaseConfig = dbConfig,
      handler = () => eventHandler
    )
  }

  private val eventHandler =
    SlickHandler[EventEnvelope[ChannelStore.Event]] { envelope =>
      envelope.event match {
        case e =>
          logger.info("New event received {}", e.toString)

          DBIO.successful(Done)
      }

    }
}

class ChannelRepository {
  import slick.generated.Tables.Channel
}
