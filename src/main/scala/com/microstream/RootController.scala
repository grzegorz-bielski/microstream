package com.microstream

import akka.actor.typed.ActorSystem
import akka.actor.typed.SpawnProtocol
import akka.actor.typed.scaladsl.ActorContext
import com.microstream.channel.ChannelController

object RootController {
  def apply(ctx: ActorContext[Nothing]) = {
    import akka.http.scaladsl.server.Directives._

    pathPrefix("api") {
      new ChannelController(ctx).route
    }
  }
}
