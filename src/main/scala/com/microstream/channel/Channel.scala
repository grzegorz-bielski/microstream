package com.microstream.channel

import com.microstream.CborSerializable
import scala.concurrent.duration._

import akka.persistence.typed.scaladsl.{
  Effect,
  ReplyEffect,
  EventSourcedBehavior,
  RetentionCriteria
}
import akka.cluster.sharding.typed.scaladsl.{
  EntityTypeKey,
  ClusterSharding,
  Entity,
  EntityRef
}
import akka.actor.typed.{ActorSystem, ActorRef, Behavior, SupervisorStrategy}
import akka.persistence.typed.PersistenceId
import scala.concurrent.ExecutionContextExecutor
import akka.actor.typed.scaladsl.Behaviors
import akka.pattern.StatusReply
import akka.util.Timeout
import scala.util.{Try, Success, Failure}

class ChannelService(implicit system: ActorSystem[_]) {
  val sharding = ClusterSharding(system)
  // given Timeout = Timeout.create(system.settings.config.getDuration("app.channel-service.ask-timeout"))
  implicit val t: Timeout = Timeout(3.seconds)
  implicit val ec: ExecutionContextExecutor = system.executionContext

  def createChannel(input: CreateChannelDto) = {
    val channelId = Channel.Id.generate(input.name)

    println(("name", input.name))
    println(("channelId", channelId))

    sharding
      .entityRefFor(ChannelStore.EntityKey, channelId)
      .askWithStatus(ChannelStore.Command.Open(input.name, _))
      .map(r =>
        println(("got", r.toString()))
      ) // it's not ever called :thinking
  }

  // https://www.baeldung.com/jackson-serialize-enums

  /*
        @​domdorn, you could always keep some state adjacent to the state managed by EventSourceBehavior
        (which does have the constraint of only changing state in the event handler).
        Adjacent state could be in another actor or hosting the event sourced behavior in a class which contains the state/EventSourcedBehavior itself.
        It would be volatile, but I think that's what you're after anyway.
   */
}

class Channel(implicit system: ActorSystem[_]) {
  import Channel._

  val sharding = ClusterSharding(system)

  // -- joining (ws)
  // check if channel store exists by sharding id
  //    - exists -> is in the memory ? join : (spawn new & join)
  //    - doesn't exists -> exception - the channel has to be created first

  // -- creating (rest)
  // check if channel store exists by sharding id
  //    -- exists -> exception, the channel is already present
  //    -- nope -> create new channel, derive sharding id from provided name

  // ChannelStore.init(system)
  // todo: channel guardian / receptionsts to keep track of open channels
  def spawn(channelId: Channel.Id): Behavior[Protocol] = Behaviors
    .setup[Protocol] { context =>
      implicit val t: Timeout = Timeout(3.seconds)

      def connected(sessions: Sessions): Behavior[Protocol] =
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

      def empty(): Behavior[Protocol] = Behaviors.receiveMessage {
        case Message.Create(name, sender) =>
          context.askWithStatus(
            channelRef(channelId),
            (r: ChannelStore.SummaryReceiver) =>
              ChannelStore.Command.Open(name, r)
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

  private def channelRef(channelId: Channel.Id) =
    sharding.entityRefFor(ChannelStore.EntityKey, channelId)
}
object Channel {
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
    case class Create(name: String, replyTo: ActorRef[Session.Message])
        extends Message
    case class Join(replyTo: ActorRef[Session.Message]) extends Message
    case class Post(content: String, replyTo: ActorRef[Session.Message])
        extends Message
  }

  sealed trait InternalMessage extends Message
  object InternalMessage extends CborSerializable {
    case class ChannelCreationFailed() extends InternalMessage
    case class Opened(replyTo: ActorRef[Session.Message])
        extends InternalMessage
  }

  type Protocol = Message
  private type PrivateProtocol = InternalMessage
  // Message | ChannelStore.Summary | InternalMessage

  type Sessions = Vector[ActorRef[Session.Message]]
}
