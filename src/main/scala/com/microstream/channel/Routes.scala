package com.microstream.channel

import akka.actor.typed.ActorSystem
import akka.actor.typed.SpawnProtocol
import akka.actor.typed.ActorRef
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.ws.{ Message, TextMessage }
import akka.stream.scaladsl.Flow
import akka.actor.typed.Props

import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import scala.concurrent.Future
import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

object Routes:
  def apply()(using system: ActorSystem[SpawnProtocol.Command]) =
    given ExecutionContextExecutor = system.executionContext
    given Timeout = Timeout(3.seconds)
  
    path("channel" / Segment) { id =>
      handleWebSocketMessages {
        val uuid = java.util.UUID.randomUUID.toString
        // : Future[ActorRef[Session.Message]]
        val res = system
          .ask[Session.Message](
            SpawnProtocol.Spawn(behavior = Session(), name = s"session: $uuid", props = Props.empty, _)
          )
          .map { x=> 
            
          // transform to flow: https://github.com/johanandren/chat-with-akka-http-websockets/blob/master/src/main/scala/chat/Server.scala   
            x
          }
          
          // await res and return flow


        Flow[Message].collect {
            case TextMessage.Strict(str) => TextMessage(s"Got $str")
        }
      }
    }
