package net.elenx.hd.akka.sample.iot.manager

import akka.actor.AbstractActor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.Terminated
import akka.event.Logging
import net.elenx.hd.akka.sample.iot.manager.group.DeviceGroupActor
import net.elenx.hd.akka.sample.iot.manager.group.RequestTrackDevice

class DeviceGroupsManagerActor : AbstractActor()
{
    companion object
    {
        fun props(): Props = Props.create(DeviceGroupsManagerActor::class.java)
    }

    private val log = Logging.getLogger(context.system, this)

    private val groupIdActorMap: MutableMap<String, ActorRef> = HashMap()
    private val actorRefGroupIdMap: MutableMap<ActorRef, String> = HashMap()

    override fun preStart() = log.info("DeviceManager started")
    override fun postStop() = log.info("DeviceManager stopped")

    override fun createReceive(): Receive =
        receiveBuilder()
            .match(RequestTrackDevice::class.java, this::trackDevice)
            .match(Terminated::class.java, this::onWatchedActorTerminated)
            .build()

    private fun trackDevice(trackMsg: RequestTrackDevice) =
        acquireActorFor(trackMsg.groupId)
            .forward(trackMsg, context)

    private fun acquireActorFor(groupId: String) = groupIdActorMap[groupId] ?: registerActorOf(groupId)
    private fun registerActorOf(groupId: String) =
        context
            .actorOf(DeviceGroupActor.props(groupId), "device-group-$groupId")
            .also { context.watch(it) }
            .also { groupIdActorMap[groupId] = it }
            .also { actorRefGroupIdMap[it] = groupId }
            .also { log.info("Registered device group actor for {}.", groupId) }

    private fun onWatchedActorTerminated(terminated: Terminated): Unit =
        terminated
            .actor
            .let { actorRefGroupIdMap.remove(it) }
            .also { log.info("Device group actor for {} has been terminated.", it) }
            .let { groupIdActorMap.remove(it) }

}