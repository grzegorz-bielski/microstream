package com.microstream.channel

import akka.actor.typed.ActorSystem
import akka.actor.typed.SpawnProtocol
import akka.actor.typed.ActorRef
import akka.NotUsed
import akka.stream.scaladsl._
import akka.stream.typed.scaladsl._
import akka.stream.OverflowStrategy
import akka.http.scaladsl.model.ws.{ Message, TextMessage }
import scala.concurrent.{ ExecutionContextExecutor, Await }
import scala.concurrent.duration._
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import akka.actor.typed.Props

type System = ActorSystem[SpawnProtocol.Command]

object Routes:
  def apply()(using system: System) =
    import akka.http.scaladsl.server.Directives._

    path("channel" / Segment) { id =>
      handleWebSocketMessages {
        assembleGraph(openSession(id))
      }
    }

  private def openSession(id: String)(using system: System): ActorRef[Session.Message] =
    given ExecutionContextExecutor = system.executionContext
    given Timeout = Timeout(3.seconds)
    
    val uuid = java.util.UUID.randomUUID.toString
    
    Await.result(
      system.ask(
        SpawnProtocol.Spawn(Session(Channel.Id(id)), s"session: $uuid", Props.empty, _)
      ),
      5.seconds
    )


  private def assembleGraph(sessionRef: ActorRef[Session.Message])(using system: System) = 
    val sink = Flow[Message]
      .map {
        case TextMessage.Strict(text) => Session.Message.Incoming(text)
      }
      .to(
        ActorSink.actorRef(
          sessionRef, 
          Session.Message.Close, 
          (_: Throwable) => Session.Message.Close
        )
      )

    val source = ActorSource.actorRef[Session.WebSocketMsg](
        { case Session.WebSocketMsg.Complete => system.log.info("completed") },
        { case Session.WebSocketMsg.Fail     => new IllegalStateException() },
        bufferSize = 100,
        overflowStrategy = OverflowStrategy.fail
      )
      .mapMaterializedValue { socketRef => 
          sessionRef ! Session.Message.Connected(socketRef)
          NotUsed
      }
      .map { 
        case Session.WebSocketMsg.Message(txt) => TextMessage(txt)
        case Session.WebSocketMsg.Complete     => TextMessage("completed")
        case Session.WebSocketMsg.Fail         => TextMessage("failed")
      }

    Flow.fromSinkAndSource(sink, source)
    