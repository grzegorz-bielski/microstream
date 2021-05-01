package com.microstream.channel

import com.microstream.CborSerializable
import scala.concurrent.duration._
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import akka.actor.typed.scaladsl.Behaviors
import akka.pattern.StatusReply
import akka.util.Timeout
import scala.util.{Try, Success, Failure}
import akka.actor.typed.receptionist.Receptionist
import java.util.UUID
import slick.jdbc.JdbcBackend.Database
import slick.basic.DatabaseConfig
import akka.actor.typed.ActorSystem
import com.microstream.channel.ChannelGuardian.Message.GetChannels
import slick.generated.Tables
import scala.concurrent.ExecutionContext
import slick.jdbc.PostgresProfile
import akka.actor.TypedActor
import akka.actor.typed.PostStop
import akka.actor.typed.Terminated
import akka.actor.typed.receptionist.ServiceKey

/** `ChannelGuardian` is an aggregate root responsible for managing
  * the `Channel`s and its adjacent `ChannelStore`s.
  */
object ChannelGuardian {
  val Key = ServiceKey[Message]("channel-guardian")

  sealed trait Message extends CborSerializable
  object Message {
    case class GetChannels(replyTo: ActorRef[StatusReply[GetChannelsPayload]]) extends Message
    case class CreateChannel(dto: CreateChannelDto, replyTo: ActorRef[StatusReply[Channel.Id]])
        extends Message
    case class JoinChannel(
        channelId: Channel.Id,
        replyTo: ActorRef[StatusReply[ChannelJoinedPayload]]
    ) extends Message

  }

  case class ChannelJoinedPayload(ref: ActorRef[Session.Message]) extends CborSerializable
  case class GetChannelsPayload(channels: Seq[ChannelQueryDto]) extends CborSerializable

  private trait PrivateMessage extends Message
  private object PrivateMessage {
    case class ChannelCreationSuccess(id: Channel.Id, replyTo: ActorRef[StatusReply[Channel.Id]])
        extends PrivateMessage
    case class ChannelCreationFailure(
        e: Throwable,
        id: Channel.Id,
        replyTo: ActorRef[StatusReply[Channel.Id]]
    ) extends PrivateMessage

    case class ChannelJoiningSuccess(id: Channel.Id, replyTo: ActorRef[Session.Message])
        extends PrivateMessage
    case class ChannelJoiningFailure(id: Channel.Id, msg: String) extends PrivateMessage
    case class GetRegisteredChannel(
        listing: Receptionist.Listing,
        msg: Message.JoinChannel
    ) extends PrivateMessage
  }

  def apply(role: String) = Behaviors.setup[Message] { context =>
    implicit val t: Timeout = Timeout(3.seconds)
    implicit val system: ActorSystem[_] = context.system
    implicit val ec: ExecutionContext = system.executionContext

    val readDbConfig = DatabaseConfig
      .forConfig[PostgresProfile]("readDb.slick", context.system.settings.config)

    val projectionDbConfig = DatabaseConfig
      .forConfig[PostgresProfile]("projectionDb.slick", context.system.settings.config)

    val readDb = readDbConfig.db

    val readRepo = new ChannelRepository(readDbConfig)
    val sharding = ClusterSharding(system)

    ChannelStore.initSharding(role)
    ChannelProjection.init(projectionDbConfig, readRepo)

    def storeRef(id: Channel.Id) =
      sharding.entityRefFor(ChannelStore.EntityKey, id)

    def spawnSession(id: Channel.Id, channel: ActorRef[Channel.Message]) = {
      val uuid = UUID.randomUUID.toString // this might not be unique enough

      context.spawn(Session(channel), s"session-$id-$uuid")
    }

    def handleInternal(m: PrivateMessage): Behavior[Message] = m match {
      case PrivateMessage.ChannelCreationFailure(e, id, replyTo) =>
        context.log.warn("Failed to open a new channel: {}, error: {}", id, e.getStackTrace())
        replyTo ! StatusReply.Error(e.toString)

        Behaviors.same

      case PrivateMessage.ChannelCreationSuccess(id, replyTo) =>
        val key = Channel.Key(id)
        val ref = context.spawn(Channel(storeRef(id)), s"channel-$id")

        context.system.receptionist ! Receptionist.Register(key, ref)
        replyTo ! StatusReply.Success(id)

        context.log.info("Opened a new channel: {}", id)

        Behaviors.same

      case PrivateMessage.GetRegisteredChannel(listing, msg) =>
        val id = msg.channelId
        val key = Channel.Key(id)

        listing.serviceInstances[Channel.Message](key).headOption match {
          // channel actor is registered
          case Some(channel) =>
            val session = spawnSession(id, channel)

            channel ! Channel.Message.Join(session)
            msg.replyTo ! StatusReply.Success(ChannelJoinedPayload(session))
            context.self ! PrivateMessage.ChannelJoiningSuccess(id, session)

          // channel actor is not registered
          case None =>
            context.askWithStatus(storeRef(id), ChannelStore.Command.IsOpen(_)) {
              // channel store is opened, but the channel actor is not registered
              case Success(true) =>
                val key = Channel.Key(id)

                val channel = context.spawn(Channel(storeRef(id)), s"channel-$id")
                val session = spawnSession(id, channel)

                channel ! Channel.Message.Join(session)
                msg.replyTo ! StatusReply.Success(ChannelJoinedPayload(session))
                context.system.receptionist ! Receptionist.Register(key, channel)

                PrivateMessage.ChannelJoiningSuccess(id, session)

              case Success(false) => // closed for opened channels>??
                PrivateMessage.ChannelJoiningFailure(id, "Channel store is not opened")
              case Failure(e) =>
                PrivateMessage.ChannelJoiningFailure(id, s"Unknown error: ${e.toString}")
            }
        }

        Behaviors.same

      case PrivateMessage.ChannelJoiningFailure(id, msg) =>
        context.log.error(s"Joining channel $id has failed. $msg")
        Behaviors.same

      case PrivateMessage.ChannelJoiningSuccess(id, session) =>
        context.log.info(s"A new session $session has been established with the channel $id")
        Behaviors.same
    }

    Behaviors
      .receiveMessage[Message] {
        case m: PrivateMessage => handleInternal(m)

        case Message.GetChannels(replyTo) =>
          // todo: move general CRUD up a layer / create orthogonal service
          readDb.run(readRepo.getChannels).onComplete {
            case Failure(e) => replyTo ! StatusReply.Error(e.toString)
            case Success(channels) =>
              replyTo ! StatusReply.Success(
                GetChannelsPayload(channels.map(c => ChannelQueryDto(c.id, c.name, c.createdAt)))
              )
          }

          Behaviors.same

        case Message.CreateChannel(dto, replyTo) =>
          val id = Channel.Id.generate(dto.name)

          context.askWithStatus(storeRef(id), ChannelStore.Command.Open(dto.name, _)) {
            case Success(_) => PrivateMessage.ChannelCreationSuccess(id, replyTo)
            case Failure(e) => PrivateMessage.ChannelCreationFailure(e, id, replyTo)
          }

          Behaviors.same

        case msg: Message.JoinChannel =>
          val adapter =
            context.messageAdapter[Receptionist.Listing](
              PrivateMessage.GetRegisteredChannel(_, msg)
            )

          context.system.receptionist ! Receptionist.Find(Channel.Key(msg.channelId), adapter)

          Behaviors.same
      }
      .receiveSignal { case (ctx, PostStop) =>
        ctx.log.warn("ChannelGuardian has been stopped")
        readDb.close()

        Behaviors.same
      }
  }
}
