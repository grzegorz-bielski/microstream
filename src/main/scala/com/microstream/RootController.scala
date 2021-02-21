package com.microstream

import akka.actor.typed.ActorSystem
import akka.actor.typed.SpawnProtocol
import akka.actor.typed.scaladsl.ActorContext
import com.microstream.channel.ChannelController
import akka.actor.typed.ActorRef
import com.microstream.channel.{ChannelGuardian, SessionGuardian}

object RootController {
  def apply()(implicit
      system: ActorSystem[_],
      chanGuardian: ActorRef[ChannelGuardian.Message],
      sessionGuardian: ActorRef[SessionGuardian.Message]
  ) = {
    import akka.http.scaladsl.server.Directives._

    pathPrefix("api") {
      new ChannelController(chanGuardian, sessionGuardian).route
    }
  }
}
