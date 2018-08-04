package net.elenx.hd.akka.sample.cluster

import akka.actor.AbstractActor
import akka.cluster.Cluster
import akka.cluster.ClusterEvent
import akka.cluster.ClusterEvent.MemberEvent
import akka.cluster.ClusterEvent.MemberRemoved
import akka.cluster.ClusterEvent.MemberUp
import akka.cluster.ClusterEvent.UnreachableMember
import akka.event.Logging

class SimpleClusterListener : AbstractActor()
{
    private var log = Logging.getLogger(context.system, this)
    private var cluster = Cluster.get(context.system)

    override fun preStart() =
        cluster
            .subscribe(self,
                ClusterEvent.initialStateAsEvents(),
                MemberEvent::class.java,
                UnreachableMember::class.java)

    override fun postStop() = cluster.unsubscribe(self)

    override fun createReceive(): Receive =
        receiveBuilder()
            .match(MemberUp::class.java) { log.info("Member is Up: {}", it.member()) }
            .match(UnreachableMember::class.java) { log.info("Member detected as unreachable: {}", it.member()) }
            .match(MemberRemoved::class.java) { log.info("Member is Removed: {}", it.member()) }
            .match(MemberEvent::class.java) { log.info("Membership event from member: {}", it.member()) }
            .build()
}
