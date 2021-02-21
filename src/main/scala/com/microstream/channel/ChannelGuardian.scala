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

object ChannelGuardian {
  sealed trait Message extends CborSerializable
  object Message {
    case class CreateChannel(dto: CreateChannelDto, replyTo: ActorRef[StatusReply[Channel.Id]])
        extends Message
    case class JoinChannel(
        channelId: Channel.Id,
        replyTo: ActorRef[StatusReply[ChannelJoinedPayload]]
    ) extends Message

  }

  case class ChannelJoinedPayload(ref: ActorRef[Session.Message]) extends CborSerializable

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
    case class ChannelJoiningFailure(id: Channel.Id) extends PrivateMessage
    case class GetRegisteredChannel(
        listing: Receptionist.Listing,
        msg: Message.JoinChannel
    ) extends PrivateMessage
  }

  def apply(role: String) = Behaviors.setup[Message] { context =>
    implicit val t: Timeout = Timeout(3.seconds)

    ChannelStore.initSharding(role)(context.system)
    val sharding = ClusterSharding(context.system)

    def storeRef(id: Channel.Id) =
      sharding.entityRefFor(ChannelStore.EntityKey, id)

    def spawnSession(id: Channel.Id, channel: ActorRef[Channel.Message]) = {
      val uuid = UUID.randomUUID.toString // this might not be unique enough

      context.spawn(Session(channel), s"session-$id-$uuid")
    }

    def handleInternal(m: PrivateMessage): Behavior[Message] = m match {

      case PrivateMessage.ChannelCreationFailure(e, id, replyTo) =>
        context.log.warn("Failed to open a new channel: {}", id)
        replyTo ! StatusReply.Error(e)

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

              // channel store is not opened
              case Success(false) => PrivateMessage.ChannelJoiningFailure(id)
              case Failure(e)     => PrivateMessage.ChannelJoiningFailure(id)
            }
        }

        Behaviors.same

      case PrivateMessage.ChannelJoiningFailure(id) =>
        context.log.error(s"Joining channel $id has failed")
        Behaviors.same

      case PrivateMessage.ChannelJoiningSuccess(id, session) =>
        context.log.info(s"A new session $session has been established with the channel $id")
        Behaviors.same
    }

    Behaviors.receiveMessage {
      case m: PrivateMessage => handleInternal(m)

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
  }
}
