package com.microstream.channel

import akka.actor.typed.ActorSystem
import akka.actor.typed.SpawnProtocol
import akka.actor.typed.ActorRef
import akka.NotUsed
import akka.stream.scaladsl._
import akka.stream.typed.scaladsl._
import akka.stream.OverflowStrategy
import akka.http.scaladsl.model.ws.{Message => WsMessage, TextMessage}
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.concurrent.duration._
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.ActorContext
import akka.util.Timeout
import akka.actor.typed.Props
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import scala.concurrent.ExecutionContext

class ChannelController(
    chanGuardian: ActorRef[ChannelGuardian.Message],
    sessionGuardian: ActorRef[SessionGuardian.Message]
)(implicit system: ActorSystem[_])
    extends FailFastCirceSupport {
  implicit lazy val ec: ExecutionContext = system.executionContext
  implicit lazy val t: Timeout = Timeout(3.seconds)

  lazy val route = {
    import akka.http.scaladsl.server.Directives._
    import akka.http.scaladsl.model._

    pathPrefix("channel") {
      (pathEndOrSingleSlash & post) {
        entity(as[CreateChannelDto]) { dto =>
          complete {
            chanGuardian
              .ask(ChannelGuardian.Message.CreateChannel(dto, _))
              .map(ChannelSummaryDto(_))
          }
        }
      } ~
        path("connect" / Segment) { id =>
          handleWebSocketMessages {
            Flow.futureFlow {
              openSession(id) map assembleGraph
            }
          }
        }
    }
  }

  private def openSession(id: String) = {
    import SessionGuardian._

    sessionGuardian
      .ask(Message.OpenSession(id, _))
      .collect { case Summary.Joined(ref) => ref }
  }

  private def assembleGraph(session: ActorRef[Session.Message]) = {
    import Session._

    val sink = Flow[WsMessage]
      .collect { case TextMessage.Strict(txt) => Message.FromSocket(txt) }
      .to(
        ActorSink.actorRef(session, Message.Close, (_: Throwable) => Message.Close)
      )

    val source = ActorSource
      .actorRef[WebSocketMsg](
        { case WebSocketMsg.Complete => system.log.info("ws completed") },
        { case WebSocketMsg.Fail =>
          system.log.error("ws failed")
          new IllegalStateException()
        },
        bufferSize = 100,
        overflowStrategy = OverflowStrategy.fail
      )
      .mapMaterializedValue { socket =>
        session ! Message.Connected(socket)
        NotUsed
      }
      .map {
        case WebSocketMsg.Message(txt) => TextMessage(txt)
        case WebSocketMsg.Complete     => TextMessage("completed")
        case WebSocketMsg.Fail         => TextMessage("failed")
      }

    Flow.fromSinkAndSource(sink, source)
  }
}
