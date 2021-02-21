package com.microstream.channel

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

import com.microstream.CborSerializable
import akka.actor.typed.receptionist.Receptionist
import akka.pattern.StatusReply

object SessionGuardian {
  trait Message extends CborSerializable
  object Message {
    case class OpenSession(id: String, replyTo: ActorRef[StatusReply[Summary]]) extends Message
  }

  private trait PrivateMessage extends Message
  private object PrivateMessage {
    case class ListingResponse(
        listing: Receptionist.Listing,
        id: Channel.Id,
        replyTo: ActorRef[StatusReply[Summary]]
    ) extends PrivateMessage
  }

  case class Summary(ref: ActorRef[Session.Message]) extends CborSerializable

  def apply() = Behaviors.setup[Message] { context =>
    Behaviors.receiveMessage {
      case Message.OpenSession(id, replyTo) =>
        val channelId = Channel.Id(id)
        val key = Channel.Key(channelId)

        val adapter =
          context.messageAdapter[Receptionist.Listing](
            PrivateMessage.ListingResponse(_, channelId, replyTo)
          )

        context.system.receptionist ! Receptionist.Find(key, adapter)

        Behaviors.same

      case PrivateMessage.ListingResponse(listing, channelId, replyTo) =>
        val key = Channel.Key(channelId)

        listing.serviceInstances[Channel.Message](key).headOption match {
          case None => replyTo ! StatusReply.Error("Channel $channelId not found")
          case Some(channel) =>
            val session = context.spawn(Session(channel), s"session: $channelId")
            replyTo ! StatusReply.Success(Summary(session))
        }

        Behaviors.same
    }
  }
}

object Session {
  trait SocketMsg extends CborSerializable
  object SocketMsg {
    case class Message(txt: String) extends SocketMsg
    case object Complete extends SocketMsg
    case object Fail extends SocketMsg
  }

  trait Message extends CborSerializable
  object Message {
    case class FromSocket(txt: String) extends Message
    case class FromChannel(txt: String) extends Message
    case class Connected(socket: ActorRef[SocketMsg]) extends Message
    case object Close extends Message
  }

  def apply(channel: ActorRef[Channel.Message]) = Behaviors.setup[Session.Message] { context =>
    context.log.info("Started a new session")

    def connected(socket: ActorRef[SocketMsg]) = Behaviors.receiveMessage[Session.Message] {
      case Message.FromChannel(content) =>
        socket ! SocketMsg.Message(content)

        Behaviors.same

      case Message.FromSocket(content) =>
        channel ! Channel.Message.Post(content)

        Behaviors.same

      case Message.Close => Behaviors.stopped

    }

    Behaviors.receiveMessage {
      case Message.Close             => Behaviors.stopped
      case Message.Connected(socket) => connected(socket)
      case _                         => Behaviors.unhandled
    }
  }
}
