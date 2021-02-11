package com.microstream

import akka.actor.typed.ActorSystem
import akka.actor.typed.SpawnProtocol
import com.microstream.channel.ChannelController

object RootController:
    def apply()(using system: ActorSystem[SpawnProtocol.Command]) =
      import akka.http.scaladsl.server.Directives._

      pathPrefix("api") {
        ChannelController().route
      }
