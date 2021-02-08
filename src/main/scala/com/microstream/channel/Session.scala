package com.microstream.channel

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

object Session:
    enum WebSocketMsg:
      case Message(txt: String)
      case Complete
      case Fail

    enum Message:
      case Incoming(txt: String)
      case Connected(socket: ActorRef[WebSocketMsg])
      case Close

    def apply(channelId: Channel.Id) = Behaviors.setup[Session.Message] { context => 
      context.log.info("Started a new session")
    
      Behaviors.receiveMessage {
        case Message.Close => Behaviors.stopped
        case _ => Behaviors.same
      }
    }

    // def connected()