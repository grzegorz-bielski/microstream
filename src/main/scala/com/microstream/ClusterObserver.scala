package com.microstream

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{Behavior}
import akka.cluster.ClusterEvent.{ClusterDomainEvent, MemberRemoved, MemberUp, UnreachableMember}
import akka.cluster.typed.{Cluster, Subscribe}



def ClusterObserver() = Behaviors.setup[ClusterDomainEvent] { context =>
  val cluster = Cluster(context.system)
  cluster.subscriptions ! Subscribe(context.self.ref, classOf[ClusterDomainEvent])

  context.log.info(s"Started actor ${context.self.path} - (${context.self.getClass})")

  Behaviors.receiveMessage {
    case MemberUp(member) =>
        context.log.info("Member is Up: {}", member.address)
        Behaviors.same
    case UnreachableMember(member) =>
        context.log.info("Member detected as unreachable: {}", member)
        Behaviors.same
    case MemberRemoved(member, previousStatus) =>
        context.log.info(
        "Member is Removed: {} after {}",
        member.address, previousStatus)
        Behaviors.same
    case _ =>
        Behaviors.same
  }
}
