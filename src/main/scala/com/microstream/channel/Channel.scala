package com.microstream.channel

import com.microstream.CborSerializable
import akka.cluster.sharding.typed.scaladsl.EntityRef
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout
import scala.util.{Try, Success, Failure}
import akka.actor.typed.receptionist.ServiceKey

object Channel {
  type Key = ServiceKey[Message]
  def Key(id: Channel.Id) = ServiceKey[Message](s"channel-$id")

  def apply(storeRef: => EntityRef[ChannelStore.Command]) =
    Behaviors.setup[Message] { context =>
      def connected(sessions: Sessions): Behavior[Message] =
        Behaviors.receiveMessage {
          case Message.Join(session) =>
            connected(sessions :+ session)

          case Message.Post(content) =>
            sessions.foreach { s => s ! Session.Message.FromChannel(content) }

            Behaviors.same
        }

      connected(Sessions.empty)
    }

  type Id = String
  object Id {
    def apply(str: String): Id = str
    def generate(str: String): Id = {
      import java.util.Base64
      import java.nio.charset.StandardCharsets

      Base64.getEncoder.encodeToString(str.getBytes(StandardCharsets.UTF_8))
    }
  }

  sealed trait Message extends CborSerializable
  object Message {
    case class Join(replyTo: ActorRef[Session.Message]) extends Message
    case class Post(content: String) extends Message
  }

  type Sessions = Vector[ActorRef[Session.Message]]
  object Sessions {
    val empty: Sessions = Vector.empty
  }
}
