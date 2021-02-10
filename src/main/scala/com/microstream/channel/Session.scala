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

      // def connected(socketRef: ActorRef[WebSocketMsg]) = Behaviors.receiveMessage[Session.Message] {

      // }
    
      Behaviors.receiveMessage {
        case Message.Close      => Behaviors.stopped
        // case Message.Connected  => 
        case _ => Behaviors.same
      }

      // TODO: handle the connected state
      // send incoming msgs to socketRef from channel
      // send WebSocketMsg msg to channel
      
    }


    // def connected()