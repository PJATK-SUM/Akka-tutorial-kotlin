package net.elenx.hd.akka.sample.iot.group

import akka.actor.AbstractActor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.Terminated
import akka.event.Logging
import net.elenx.hd.akka.sample.iot.group.device.DeviceActor

class DeviceGroupActor(private val groupId: String) : AbstractActor()
{
    companion object
    {
        fun props(groupId: String): Props = Props.create(DeviceGroupActor::class.java, groupId)
    }

    private val log = Logging.getLogger(context.system, this)

    private val deviceIdActorMap = mutableMapOf<String, ActorRef>()
    private val actorRefDeviceIdMap = mutableMapOf<ActorRef, String>()

    override fun preStart() = log.info("DeviceGroup {} started.", groupId)
    override fun postStop() = log.info("DeviceGroup {} stopped.", groupId)

    override fun createReceive(): Receive =
        receiveBuilder()
            .match(RequestTrackDevice::class.java) { if (isGroupValid(it.groupId)) trackDevice(it) else logInvalidGroup(it.groupId) }
            .match(RequestDeviceList::class.java, this::onRequestDeviceList)
            .match(Terminated::class.java, this::onWatchedActorTerminated)
            .build()

    private fun isGroupValid(groupId: String) = groupId == this.groupId

    private fun trackDevice(trackMsg: RequestTrackDevice) =
        acquireActorFor(trackMsg.groupId, trackMsg.deviceId)
            .forward(trackMsg, context)

    private fun acquireActorFor(groupId: String, deviceId: String) = deviceIdActorMap[deviceId] ?: registerActorOf(groupId, deviceId)
    private fun registerActorOf(groupId: String, deviceId: String) =
        context
            .actorOf(DeviceActor.props(groupId, deviceId), "device-$deviceId")
            .also { context.watch(it) }
            .also { deviceIdActorMap[deviceId] = it }
            .also { actorRefDeviceIdMap[it] = deviceId }
            .also { log.info("Registered device actor for {}.", deviceId) }

    private fun logInvalidGroup(groupId: String) =
        log.warning(
            "Ignoring TrackDevice request for {}. This actor is responsible for {}.",
            groupId, this.groupId
        )

    private fun onRequestDeviceList(requestDeviceList: RequestDeviceList) =
        sender.tell(ReplyDeviceList(requestDeviceList.requestId, deviceIdActorMap.keys), self)

    private fun onWatchedActorTerminated(terminated: Terminated): Unit =
        terminated
            .actor
            .let { actorRefDeviceIdMap.remove(it) }
            .also { log.info("Device actor for {} has been terminated.", it) }
            .let { deviceIdActorMap.remove(it) }

}