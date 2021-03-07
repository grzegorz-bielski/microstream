package com.microstream

import akka.actor.typed.ActorSystem
import akka.actor.typed.SpawnProtocol
import akka.actor.typed.scaladsl.ActorContext
import com.microstream.channel.ChannelController
import akka.actor.typed.ActorRef
import com.microstream.channel.ChannelGuardian

object RootController {
  def apply()(implicit
      system: ActorSystem[_],
      chanGuardian: ActorRef[ChannelGuardian.Message]
  ) = {
    import akka.http.scaladsl.server.Directives._

    pathPrefix("api") {
      new ChannelController(chanGuardian).route
    }
  }
}
