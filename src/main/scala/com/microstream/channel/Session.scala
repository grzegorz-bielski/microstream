package com.microstream.channel

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

import com.microstream.CborSerializable

object Session {
  trait WebSocketMsg extends CborSerializable
  object WebSocketMsg {
    case class Message(txt: String) extends WebSocketMsg
    case object Complete extends WebSocketMsg
    case object Fail extends WebSocketMsg
  }

  trait Message extends CborSerializable
  object Message {
    case class Incoming(txt: String) extends Message
    case class Connected(socket: ActorRef[WebSocketMsg]) extends Message
    case object Close extends Message
  }

  def apply(channelId: Channel.Id) = Behaviors.setup[Session.Message] {
    context =>
      context.log.info("Started a new session")

      // def connected(socketRef: ActorRef[WebSocketMsg]) = Behaviors.receiveMessage[Session.Message] {

      // }

      Behaviors.receiveMessage {
        case Message.Close => Behaviors.stopped
        // case Message.Connected  =>
        case _ => Behaviors.same
      }

    // TODO: handle the connected state
    // send incoming msgs to socketRef from channel
    // send WebSocketMsg msg to channel

  }

  // def connected()
}
