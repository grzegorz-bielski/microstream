package com.microstream

import akka.actor.typed.ActorSystem
import akka.actor.typed.SpawnProtocol
import akka.actor.typed.scaladsl.ActorContext
import com.microstream.channel.ChannelController
import akka.actor.typed.ActorRef
import com.microstream.channel.ChannelGuardian

object RootController {
  def apply(chanGuardian: ActorRef[ChannelGuardian.Message])(implicit system: ActorSystem[_]) = {
    import akka.http.scaladsl.server.Directives._

    pathPrefix("api") {
      new ChannelController(chanGuardian).route
    }
  }
}
