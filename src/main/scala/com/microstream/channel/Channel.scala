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
    case CreationFailed
    case Opened(replyTo: ActorRef[Session.Message])

  type Protocol = Message
  private type PrivateProtocol = Message | ChannelStore.Summary | InternalMessage

  type Sessions = Vector[ActorRef[Session.Message]]

  def apply(channelId: Channel.Id): Behavior[Protocol] = Behaviors.setup[PrivateProtocol] { context => 
    val sharding = ClusterSharding(context.system)
    given Timeout = Timeout(3.seconds)

    def channelRef = sharding.entityRefFor(ChannelStore.EntityKey, channelId)

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
        context.askWithStatus(channelRef, (r: ChannelStore.SummaryReceiver) => ChannelStore.Command.Open(name, r)) {
          case Success(r) => InternalMessage.Opened(sender)
          case Failure(e) => InternalMessage.CreationFailed
        }
        Behaviors.same
      case InternalMessage.Opened(session) => connected(Vector(session))
      case _                               => Behaviors.same 
    }

    empty()
  }.narrow

// a persistent sharded store for a given channel
private[channel] object ChannelStore:
  val EntityKey = EntityTypeKey[Command]("channel-store")

  type SummaryReceiver = ActorRef[StatusReply[Summary]]

  enum Command extends CborSerializable:
    case Open(name: String, replyTo: SummaryReceiver)
    case Customize(name: String, replyTo: SummaryReceiver)
    case Message(id: String, content: String, replyTo: SummaryReceiver)
  object Command:
    type All = Command.Open & Command.Customize & Command.Message

  enum Event extends CborSerializable:
    case Opened(channelId: Channel.Id, name: String)
    case Customized(channelId: Channel.Id, name: String)
    case Messaged(channelId: Channel.Id, msgId: String, content: String)

  case class Summary(state: State)

  sealed trait State extends CborSerializable:
    def onCommand(channelId: Channel.Id, command: Command): ReplyEffect[Event, State]
    def onEvent(event: Event): State
    val successReply = (s: State) => StatusReply.Success(Summary(s))

  private object State:
    case object Empty extends State:
      def onCommand(channelId: Channel.Id, command: Command) = command match
        case Command.Open(name, replyTo) => Effect.persist(Event.Opened(channelId, name)).thenReply(replyTo)(successReply)
        case other: Command.All          => Effect.reply(other.replyTo)(StatusReply.Error(s"The channel is not open"))
      def onEvent(event: Event) = event match
        case Event.Opened(_, name) => State.Open(name, Stream.empty)
        case _                     => this

    case class Open(name: String, msgs: Stream[Event.Messaged]) extends State:
      def onCommand(channelId: Channel.Id, command: Command): ReplyEffect[Event, State] = command match
        case Command.Open(name, replyTo)           => Effect.reply(replyTo)(StatusReply.Error(s"The `$name` channel has already been opened"))
        case Command.Customize(name, replyTo)      => Effect.persist(Event.Customized(channelId, name)).thenReply(replyTo)(successReply)
        case Command.Message(id, content, replyTo) => Effect.persist(Event.Messaged(channelId, id, content)).thenReply(replyTo)(successReply)

      def onEvent(event: Event): State = event match 
        case Event.Customized(_, name) => copy(name = name)
        case msg: Event.Messaged       => copy(msgs = msgs :+ msg)
        case _                         => this

  def init(system: ActorSystem[_]) = 
    ClusterSharding(system)
      .init(Entity(EntityKey)(ctx => ChannelStore(Channel.Id(ctx.entityId))))

  def apply(channelId: Channel.Id) =
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, State](
        persistenceId = PersistenceId(EntityKey.name, channelId.unwrap),
        emptyState = State.Empty,
        commandHandler = (state, command) => state.onCommand(channelId, command),
        eventHandler = (state, event) => state.onEvent(event)
      )
      .withRetention(
        RetentionCriteria.snapshotEvery(
          numberOfEvents = 100, 
          keepNSnapshots = 3
        )
      )
      .onPersistFailure(
        SupervisorStrategy.restartWithBackoff(
          minBackoff = 200.millis, 
          maxBackoff = 5.seconds, 
          randomFactor = 0.1
        )
      )
