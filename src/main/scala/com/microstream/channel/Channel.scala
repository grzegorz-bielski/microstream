package com.microstream.channel

import com.microstream.CborSerializable
import scala.concurrent.duration._

import akka.persistence.typed.scaladsl.{
  Effect,
  ReplyEffect,
  EventSourcedBehavior,
  RetentionCriteria
}
import akka.cluster.sharding.typed.scaladsl.{EntityTypeKey, ClusterSharding, Entity, EntityRef}
import akka.actor.typed.{ActorSystem, ActorRef, Behavior, SupervisorStrategy}
import akka.persistence.typed.PersistenceId
import scala.concurrent.ExecutionContextExecutor
import akka.actor.typed.scaladsl.Behaviors
import akka.pattern.StatusReply
import akka.util.Timeout
import scala.util.{Try, Success, Failure}
import com.microstream.AppError
import akka.actor.typed.receptionist.ServiceKey
import akka.actor.typed.receptionist.Receptionist

object ChannelGuardian {
  sealed trait Message extends CborSerializable
  object Message {
    case class CreateChannel(dto: CreateChannelDto) extends Message
    case class JoinChannel(channelId: Channel.Id, replyTo: ActorRef[Session.Message])
        extends Message

    case class OpenSession(id: String) extends Message
  }

  private trait PrivateMessage extends Message
  private object PrivateMessage {
    case class ChannelCreationSuccess(id: Channel.Id) extends PrivateMessage
    case class ChannelCreationFailure(id: Channel.Id) extends PrivateMessage
    case class ChannelJoiningSuccess(id: Channel.Id) extends PrivateMessage
    case class ChannelJoiningFailure(id: Channel.Id) extends PrivateMessage
    case class ListingResponse(listing: Receptionist.Listing, msg: Message.JoinChannel)
        extends PrivateMessage
  }

  def apply(role: String) = Behaviors.setup[Message] { context =>
    implicit val t: Timeout = Timeout(3.seconds)

    ChannelStore.initSharding(role)(context.system)
    val sharding = ClusterSharding(context.system)

    def storeRef(id: Channel.Id) =
      sharding.entityRefFor(ChannelStore.EntityKey, id)

    def handleInternal(m: PrivateMessage): Behavior[Message] = m match {
      case PrivateMessage.ChannelCreationSuccess(id) =>
        val ref = context.spawn(Channel(storeRef(id)), s"channel-$id")
        val key = Channel.Key(id)

        context.system.receptionist ! Receptionist.Register(key, ref)

        Behaviors.same

      case PrivateMessage.ListingResponse(listing, msg) =>
        val key = Channel.Key(msg.channelId)
        val ref = listing.serviceInstances[Channel.Message](key).head

        ref ! Channel.Message.Join(msg.replyTo)

        Behaviors.same
    }

    Behaviors.receiveMessage {
      case m: PrivateMessage => handleInternal(m)

      case Message.OpenSession(id) =>
        context.spawn(Session(Channel.Id(id)), s"session: $id")

        Behaviors.same

      case Message.CreateChannel(dto) =>
        val id = Channel.Id.generate(dto.name)

        context.askWithStatus(
          storeRef(id),
          ChannelStore.Command.Open(dto.name, _)
        ) {
          case Success(_) => PrivateMessage.ChannelCreationSuccess(id)
          case Failure(_) => PrivateMessage.ChannelCreationFailure(id)
        }

        Behaviors.same

      case msg: Message.JoinChannel =>
        val adapter =
          context.messageAdapter[Receptionist.Listing](PrivateMessage.ListingResponse(_, msg))

        context.system.receptionist ! Receptionist.Find(Channel.Key(msg.channelId), adapter)

        Behaviors.same
    }
  }
}

object Channel {
  type Key = ServiceKey[Message]
  def Key(id: Channel.Id) = ServiceKey[Message](s"channel-$id")

  def apply(storeRef: => EntityRef[ChannelStore.Command]) =
    Behaviors
      .setup[Message] { context =>
        implicit val t: Timeout = Timeout(3.seconds)

        def connected(sessions: Sessions): Behavior[Message] =
          Behaviors.receiveMessage {
            case Message.Join(session)          => connected(sessions :+ session)
            case Message.Post(content, replyTo) =>
              // don't wait for ack
              sessions.foreach { session =>
                session ! Session.Message.Incoming(content)
              }
              Behaviors.same
            case _ => Behaviors.same
          }

        def empty(): Behavior[Message] = Behaviors.receiveMessage {
          case Message.Create(name, sender) =>
            context.askWithStatus(
              storeRef,
              ChannelStore.Command.Open(name, _)
            ) {
              case Success(r) =>
                context.log.info("A `{}` channel was created", name)
                InternalMessage.Opened(sender)
              case Failure(e) =>
                context.log.error(
                  "Failed to create channel {}. Received {}",
                  name,
                  e
                )
                InternalMessage.ChannelCreationFailed()
            }
            Behaviors.same
          case InternalMessage.Opened(session) => connected(Vector(session))
          case _                               => Behaviors.same
        }

        empty()
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
    case class Create(name: String, replyTo: ActorRef[Session.Message]) extends Message
    case class Join(replyTo: ActorRef[Session.Message]) extends Message
    case class Post(content: String, replyTo: ActorRef[Session.Message]) extends Message
  }

  private trait InternalMessage extends Message
  private object InternalMessage extends CborSerializable {
    case class ChannelCreationFailed() extends InternalMessage
    case class Opened(replyTo: ActorRef[Session.Message]) extends InternalMessage
  }

  type Sessions = Vector[ActorRef[Session.Message]]
}
