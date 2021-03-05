package com.microstream.channel

import com.microstream.CborSerializable
import scala.concurrent.duration._

import akka.persistence.typed.scaladsl.{
  Effect,
  ReplyEffect,
  EventSourcedBehavior,
  RetentionCriteria
}
import akka.cluster.sharding.typed.scaladsl.{EntityTypeKey, ClusterSharding, Entity}
import akka.cluster.sharding.typed.{ClusterShardingSettings}
import akka.actor.typed.{ActorSystem, ActorRef, Behavior, SupervisorStrategy}
import akka.persistence.typed.PersistenceId
import akka.pattern.StatusReply
import com.fasterxml.jackson.annotation._

// a persistent sharded store for a given channel
object ChannelStore {
  val EntityKey = EntityTypeKey[Command]("channel-store")

  // arbitrary, ~ 10 X the planned number of nodes in cluster
  val tags = Vector.tabulate(3)(i => s"channel-tag-$i")

  type SummaryReceiver = ActorRef[StatusReply[Summary]]

  sealed trait Command extends CborSerializable
  object Command {
    case class Open(name: String, replyTo: SummaryReceiver) extends Command
    case class Customize(name: String, replyTo: SummaryReceiver) extends Command
    case class Message(id: String, content: String, replyTo: SummaryReceiver) extends Command
    case class IsOpen(replyTo: ActorRef[StatusReply[Boolean]]) extends Command

    type All = Command.Open with Command.Customize with Command.Message with Command.IsOpen
  }

  sealed trait Event extends CborSerializable
  object Event {
    case class Opened(channelId: Channel.Id, name: String) extends Event
    case class Customized(channelId: Channel.Id, name: String) extends Event
    case class Messaged(channelId: Channel.Id, msgId: String, content: String) extends Event
  }

  case class Summary(state: String) extends CborSerializable

  val successReply = (s: State) => StatusReply.Success(Summary("ok"))

  sealed abstract class State {
    def onCommand(
        channelId: Channel.Id,
        command: Command
    ): ReplyEffect[Event, State]
    def onEvent(event: Event): State
  }

  case class Empty() extends State {
    def onCommand(channelId: Channel.Id, command: Command) = {
      println(("channelId", channelId, "command", command))
      command match {
        case Command.IsOpen(replyTo) =>
          Effect.reply(replyTo)(StatusReply.Success(false))

        case Command.Open(name, replyTo) =>
          Effect
            .persist(Event.Opened(channelId, name))
            .thenReply(replyTo)(successReply)
        case other @ (_: Command.Customize | _: Command.Message) =>
          Effect.reply(other.asInstanceOf[Command.All].replyTo)(
            StatusReply.Error(s"The channel is not open")
          )
      }

    }
    def onEvent(event: Event) = {
      println(("event", event))
      event match {
        case Event.Opened(_, name) => Open(name, LazyList.empty)
        case _                     => this
      }

    }
  }

  case class Open(name: String, msgs: LazyList[Event.Messaged]) extends State {
    def onCommand(channelId: Channel.Id, command: Command) = command match {
      case Command.IsOpen(replyTo) =>
        Effect.reply(replyTo)(StatusReply.Success(true))

      case Command.Open(name, replyTo) =>
        Effect.reply(replyTo)(
          StatusReply.Error(s"The `$name` channel has already been opened")
        )
      case Command.Customize(name, replyTo) =>
        Effect
          .persist(Event.Customized(channelId, name))
          .thenReply(replyTo)(successReply)
      case Command.Message(id, content, replyTo) =>
        Effect
          .persist(Event.Messaged(channelId, id, content))
          .thenReply(replyTo)(successReply)
    }

    def onEvent(event: Event): State = event match {
      case Event.Customized(_, name) => copy(name = name)
      case msg: Event.Messaged       => copy(msgs = msgs :+ msg)
      case _                         => this
    }

  }

  def initSharding(role: String)(implicit system: ActorSystem[_]) =
    ClusterSharding(system) init Entity(ChannelStore.EntityKey) { ctx =>
      val tagIndex = math.abs(ctx.entityId.hashCode % tags.size)

      ChannelStore(
        Channel.Id(ctx.entityId),
        tags(tagIndex)
      )
    }.withSettings(ClusterShardingSettings(system).withRole(role))

  def apply(channelId: Channel.Id, projectionTag: String) =
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, State](
        persistenceId = PersistenceId(EntityKey.name, channelId),
        emptyState = Empty(),
        commandHandler = (state, command) => state.onCommand(channelId, command),
        eventHandler = (state, event) => state.onEvent(event)
      )
      .withRetention(
        RetentionCriteria.snapshotEvery(
          numberOfEvents = 100,
          keepNSnapshots = 3
        )
      )
      .withTagger(_ => Set(projectionTag))
      .onPersistFailure(
        SupervisorStrategy.restartWithBackoff(
          minBackoff = 200.millis,
          maxBackoff = 5.seconds,
          randomFactor = 0.1
        )
      )
}

// todo: add slick, projections and create read model for channels listing
