package com.microstream

import akka.actor.typed.ActorSystem
import akka.actor.typed.SpawnProtocol
import akka.actor.typed.scaladsl.ActorContext
import com.microstream.channel.ChannelController

def RootController(ctx: ActorContext[Nothing]) =
  import akka.http.scaladsl.server.Directives._
  
  pathPrefix("api") {
    ChannelController(ctx).route
  }
