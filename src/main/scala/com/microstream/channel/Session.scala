package com.microstream.channel

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

object Session:
    enum Message:
      case Join

    def apply() = Behaviors.setup[Session.Message] { context => 
        // channel: ActorRef[Channel.Command]
        context.log.info("Started a new session")
    
        Behaviors.same
    }

    // def connected()