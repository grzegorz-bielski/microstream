package com.microstream.channel

import com.microstream.CborSerializable
import scala.concurrent.duration._

import akka.persistence.typed.scaladsl.{ Effect, ReplyEffect, EventSourcedBehavior, RetentionCriteria}
import akka.cluster.sharding.typed.scaladsl.{ EntityTypeKey, ClusterSharding, Entity, EntityRef }
import akka.actor.typed.{ ActorSystem, ActorRef, Behavior, SupervisorStrategy }
import akka.persistence.typed.PersistenceId
import akka.actor.typed.scaladsl.Behaviors
import akka.pattern.StatusReply
import akka.util.Timeout
import scala.util.{ Try, Success, Failure }

// https://github.com/johanandren/chat-with-akka-http-websockets/blob/master/src/main/scala/chat/Server.scala
class Channel(using system: ActorSystem[_]):
  import Channel._

  val sharding = ClusterSharding(system)

  ChannelStore.init(system)
  // todo: channel guardian / receptionsts to keep track of open channels
  def spawn(channelId: Channel.Id): Behavior[Protocol] = Behaviors.setup[PrivateProtocol] { context => 
    given Timeout = Timeout(3.seconds)

    def connected(sessions: Sessions): Behavior[PrivateProtocol] = Behaviors.receiveMessage {
      case Message.Join(session)          => connected(sessions :+ session)
      case Message.Post(content, replyTo) =>
        // don't wait for ack
        sessions.foreach { session => session ! Session.Message.Incoming(content)}
        Behaviors.same
      case _                              => Behaviors.same 
    }

    def empty(): Behavior[PrivateProtocol] = Behaviors.receiveMessage {
      case Message.Create(name, sender)    => 
        context.askWithStatus(channelRef(channelId), (r: ChannelStore.SummaryReceiver) => ChannelStore.Command.Open(name, r)) {
          case Success(r) => 
            context.log.info("A `{}` channel was created", name) 
            InternalMessage.Opened(sender)
          case Failure(e) =>
            context.log.error("Failed to create channel {}. Received {}", name, e) 
            InternalMessage.ChannelCreationFailed
        }
        Behaviors.same
      case InternalMessage.Opened(session) => connected(Vector(session))
      case _                               => Behaviors.same 
    }

    empty()
  }.narrow

  private def channelRef(channelId: Channel.Id) = 
    sharding.entityRefFor(ChannelStore.EntityKey, channelId.unwrap)


object Channel:
  opaque type Id = String
  object Id:
    def apply(str: String): Id = str
  extension (id: Id)
    def unwrap: String = id

  enum Message extends CborSerializable:
    case Create(name: String, replyTo: ActorRef[Session.Message])
    case Join(replyTo: ActorRef[Session.Message])
    case Post(content: String, replyTo: ActorRef[Session.Message])

  private enum InternalMessage extends CborSerializable:
    case ChannelCreationFailed
    case Opened(replyTo: ActorRef[Session.Message])

  type Protocol = Message
  private type PrivateProtocol = Message | ChannelStore.Summary | InternalMessage

  type Sessions = Vector[ActorRef[Session.Message]]
