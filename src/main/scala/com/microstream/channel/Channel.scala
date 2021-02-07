package com.microstream.channel

import com.microstream.CborSerializable
import scala.concurrent.duration._

import akka.persistence.typed.scaladsl.ReplyEffect
import akka.persistence.typed.scaladsl.Effect
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.sharding.typed.scaladsl.Entity
import akka.persistence.typed.scaladsl.EventSourcedBehavior
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.RetentionCriteria
import akka.actor.typed.SupervisorStrategy

// https://github.com/johanandren/chat-with-akka-http-websockets/blob/master/src/main/scala/chat/Server.scala
object Channel:
  val EntityKey = EntityTypeKey[Command]("Channel")
  opaque type Id = String
  object Id:
    def apply(str: String): Id = str

  enum Command extends CborSerializable:
    case Customize(name: Option[String], description: Option[String])
    case Message(id: String, content: String)

  enum Event extends CborSerializable:
    case Customized(channelId: Id, name: Option[String], description: Option[String])
    case Messaged(channelId: Id, msgId: String, content: String)

  object State:
    val empty = State(None, None, Vector())

  final case class State(
    name: Option[String], 
    description: Option[String], 
    msgs: Vector[Event.Messaged]
  ):
    def handleCommand(channelId: Id, command: Command): ReplyEffect[Event, State] = command match
      case Command.Customize(name, description) => ???

    def handleEvent(event: Event): State = event match
      case m: Event.Messaged    => copy(msgs = msgs :+ m)
      case e: Event.Customized  => ???


  def apply(channelId: Id) =
    EventSourcedBehavior
      .withEnforcedReplies[Command, Event, State](
        persistenceId = PersistenceId(EntityKey.name, channelId),
        emptyState = State.empty,
        commandHandler = (state, command) => state.handleCommand(channelId, command),
        eventHandler = (state, event) => state.handleEvent(event)
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
