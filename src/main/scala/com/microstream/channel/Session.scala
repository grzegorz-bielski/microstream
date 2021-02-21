package com.microstream.channel

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

import com.microstream.CborSerializable
import akka.actor.typed.receptionist.Receptionist
import akka.pattern.StatusReply

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
