package com.microstream.channel

import akka.actor.typed.ActorSystem
import akka.actor.typed.ActorRef
import akka.NotUsed
import akka.stream.scaladsl.Flow
import akka.stream.typed.scaladsl._
import akka.stream.OverflowStrategy
import akka.http.scaladsl.model.ws.{Message => WsMessage, TextMessage}
import scala.concurrent.duration._
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import scala.concurrent.ExecutionContext
import akka.pattern.StatusReply
import scala.util.{Success, Failure}
import org.slf4j.LoggerFactory

class ChannelController(chanGuardian: ActorRef[ChannelGuardian.Message])(implicit
    system: ActorSystem[_]
) extends FailFastCirceSupport {
  implicit lazy val ec: ExecutionContext = system.executionContext
  implicit lazy val t: Timeout = Timeout(3.seconds)

  val logger = LoggerFactory.getLogger(getClass)

  lazy val route = {
    import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
    import akka.http.scaladsl.server.Directives._
    import akka.http.scaladsl.model._

    (cors() & pathPrefix("channels")) {
      (pathEndOrSingleSlash & get) {
        onComplete {
          chanGuardian
            .askWithStatus(ChannelGuardian.Message.GetChannels(_))
            .map(_.channels)
        } {
          case Success(a)                             => complete(StatusCodes.OK -> a)
          case Failure(StatusReply.ErrorMessage(msg)) => complete(StatusCodes.BadRequest -> msg)
          case e =>
            logger.info("Unexpected Error {} at `channels` GET", e.toString)
            complete(StatusCodes.InternalServerError)
        }
      } ~
        (pathEndOrSingleSlash & post) {
          entity(as[CreateChannelDto]) { dto =>
            onComplete {
              chanGuardian
                .askWithStatus(ChannelGuardian.Message.CreateChannel(dto, _))
                .map(ChannelCreationSummaryDto(_))
            } {
              case Success(a)                             => complete(StatusCodes.Created -> a)
              case Failure(StatusReply.ErrorMessage(msg)) => complete(StatusCodes.BadRequest -> msg)
              case e =>
                logger.info("Unexpected Error {} at `channels` POST", e.toString)
                complete(StatusCodes.InternalServerError)
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

  private def openSession(id: String) =
    chanGuardian
      .askWithStatus(ChannelGuardian.Message.JoinChannel(id, _))
      .map(_.ref)

  private def assembleGraph(session: ActorRef[Session.Message]) = {
    import Session._

    val sink = Flow[WsMessage]
      .collect { case TextMessage.Strict(txt) => Message.FromSocket(txt) }
      .to(
        ActorSink.actorRef(session, Message.Close, (_: Throwable) => Message.Close)
      )

    val source = ActorSource
      .actorRef[SocketMsg](
        { case SocketMsg.Complete => system.log.info("ws completed") },
        { case SocketMsg.Fail =>
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
        case SocketMsg.Message(txt) => TextMessage(txt)
        case SocketMsg.Complete     => TextMessage("completed")
        case SocketMsg.Fail         => TextMessage("failed")
      }

    Flow.fromSinkAndSource(sink, source)
  }
}
