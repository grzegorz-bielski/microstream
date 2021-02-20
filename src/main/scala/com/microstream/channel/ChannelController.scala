package com.microstream.channel

import akka.actor.typed.ActorSystem
import akka.actor.typed.SpawnProtocol
import akka.actor.typed.ActorRef
import akka.NotUsed
import akka.stream.scaladsl._
import akka.stream.typed.scaladsl._
import akka.stream.OverflowStrategy
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.concurrent.duration._
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.ActorContext
import akka.util.Timeout
import akka.actor.typed.Props
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
// import com.microstream.channel.ChannelService

// type System = ActorSystem[SpawnProtocol.Command]

class ChannelController(chanGuardian: ActorRef[ChannelGuardian.Message])(implicit
    system: ActorSystem[_]
) extends FailFastCirceSupport {
  // implicit val system: ActorSystem[Nothing] = system
  // val channelService = new ChannelService()

  lazy val route = {
    import akka.http.scaladsl.server.Directives._
    import akka.http.scaladsl.model._

    pathPrefix("channel") {
      (pathEndOrSingleSlash & post) {
        entity(as[CreateChannelDto]) { dto =>
          chanGuardian ! ChannelGuardian.Message.CreateChannel(dto)

          complete(
            HttpEntity(ContentTypes.`application/json`, """{ "msg": "xd"}""")
          )
        }
      } ~
        path("connect" / Segment) { id =>
          handleWebSocketMessages {
            assembleGraph(openSession(id))
          }
        }
    }
  }

  // private def openSession(id: String): ActorRef[Session.Message] =
  //   ctx.spawn(Session(Channel.Id(id)), s"session: $id")

  private def assembleGraph(sessionRef: ActorRef[Session.Message]) = {
    val sink = Flow[Message]
      .collect { case TextMessage.Strict(text) =>
        Session.Message.Incoming(text)
      }
      .to(
        ActorSink.actorRef(
          sessionRef,
          Session.Message.Close,
          (_: Throwable) => Session.Message.Close
        )
      )

    val source = ActorSource
      .actorRef[Session.WebSocketMsg](
        { case Session.WebSocketMsg.Complete => system.log.info("completed") },
        { case Session.WebSocketMsg.Fail => new IllegalStateException() },
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
  }
}
