package com.microstream.channel

import com.microstream.CborSerializable
import scala.concurrent.duration._

import akka.persistence.typed.scaladsl.{ Effect, ReplyEffect, EventSourcedBehavior, RetentionCriteria }
import akka.cluster.sharding.typed.scaladsl.{ EntityTypeKey, ClusterSharding, Entity }
import akka.actor.typed.{ ActorSystem, ActorRef, Behavior, SupervisorStrategy }
import akka.persistence.typed.PersistenceId
import akka.pattern.StatusReply

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
        case Command.Open(name, replyTo)                   => 
          Effect.persist(Event.Opened(channelId, name)).thenReply(replyTo)(successReply)
        case other @ (_:Command.Customize | _:Command.Message) => 
          Effect.reply(other.asInstanceOf[Command.All].replyTo)(StatusReply.Error(s"The channel is not open"))
      def onEvent(event: Event) = event match
        case Event.Opened(_, name) => State.Open(name, LazyList.empty)
        case _                     => this

    case class Open(name: String, msgs: LazyList[Event.Messaged]) extends State:
      def onCommand(channelId: Channel.Id, command: Command) = command match
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
