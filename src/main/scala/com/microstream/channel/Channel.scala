package com.microstream.channel

import com.microstream.CborSerializable
import scala.concurrent.duration._

import akka.persistence.typed.scaladsl.{ Effect, ReplyEffect, EventSourcedBehavior, RetentionCriteria}
import akka.cluster.sharding.typed.scaladsl.{ EntityTypeKey, ClusterSharding, Entity }
import akka.actor.typed.{ ActorSystem, ActorRef, SupervisorStrategy }
import akka.persistence.typed.PersistenceId
import akka.pattern.StatusReply

// https://github.com/johanandren/chat-with-akka-http-websockets/blob/master/src/main/scala/chat/Server.scala
object Channel:
  opaque type Id = String
  object Id:
    def apply(str: String): Id = str
  extension (id: Id)
    def unwrap: String = id

  enum Message extends CborSerializable:

    case Post(id: String, content: String)

// a persistent sharded store for a given channel
private[channel] object ChannelStore:
  val EntityKey = EntityTypeKey[Command]("channel-store")

  type SummaryReceiver = ActorRef[StatusReply[State.Summary]]

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

  sealed trait State extends CborSerializable:
    def onCommand(channelId: Channel.Id, command: Command): ReplyEffect[Event, State]
    def onEvent(event: Event): State
    val successReply = (s: State) => StatusReply.Success(State.Summary(s))

  object State:
    final case class Summary(state: State)

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
