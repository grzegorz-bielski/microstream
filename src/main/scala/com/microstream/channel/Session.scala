package com.microstream.channel

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

import com.microstream.CborSerializable
import akka.actor.typed.receptionist.Receptionist

object SessionGuardian {
  trait Message extends CborSerializable
  object Message {
    case class OpenSession(id: String, replyTo: ActorRef[Summary]) extends Message
  }

  private trait PrivateMessage extends Message
  private object PrivateMessage {
    case class ListingResponse(
        listing: Receptionist.Listing,
        id: Channel.Id,
        replyTo: ActorRef[Summary]
    ) extends PrivateMessage
  }

  trait Summary extends CborSerializable
  object Summary {
    case class Joined(session: ActorRef[Session.Message]) extends Summary
    case class ChannelNotFound() extends Summary
  }

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
          case None => replyTo ! Summary.ChannelNotFound()
          case Some(channel) =>
            val session = context.spawn(Session(channel), s"session: $channelId")
            replyTo ! Summary.Joined(session)
        }

        Behaviors.same
    }
  }
}

object Session {
  trait WebSocketMsg extends CborSerializable
  object WebSocketMsg {
    case class Message(txt: String) extends WebSocketMsg
    case object Complete extends WebSocketMsg
    case object Fail extends WebSocketMsg
  }

  trait Message extends CborSerializable
  object Message {
    case class FromSocket(txt: String) extends Message
    case class FromChannel(txt: String) extends Message
    case class Connected(socket: ActorRef[WebSocketMsg]) extends Message
    case object Close extends Message
  }

  def apply(channel: ActorRef[Channel.Message]) = Behaviors.setup[Session.Message] { context =>
    context.log.info("Started a new session")

    def connected(socket: ActorRef[WebSocketMsg]) = Behaviors.receiveMessage[Session.Message] {
      case Message.FromChannel(content) =>
        socket ! WebSocketMsg.Message(content)

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
