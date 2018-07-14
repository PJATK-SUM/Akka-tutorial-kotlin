package net.elenx.hd.akka.sample.iot.group

import akka.actor.AbstractActor
import akka.actor.ActorRef
import akka.actor.Props
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

    override fun preStart() = log.info("DeviceGroup {} started", groupId)
    override fun postStop() = log.info("DeviceGroup {} stopped", groupId)

    override fun createReceive(): Receive =
        receiveBuilder()
            .match(RequestTrackDevice::class.java) { if (isGroupValid(it.groupId)) trackDevice(it) else logInvalidGroup(it.groupId) }
            .build()

    private fun isGroupValid(groupId: String) = groupId == this.groupId

    private fun trackDevice(trackMsg: RequestTrackDevice) =
        acquireActorFor(trackMsg.groupId, trackMsg.deviceId)
            .also { deviceIdActorMap[trackMsg.deviceId] = it }
            .forward(trackMsg, context)

    private fun acquireActorFor(groupId: String, deviceId: String) = deviceIdActorMap[deviceId] ?: createDeviceActorOf(groupId, deviceId)
    private fun createDeviceActorOf(groupId: String, deviceId: String) =
        context
            .actorOf(DeviceActor.props(groupId, deviceId), "device-$deviceId")
            .also { logDeviceActorCreationOf(deviceId) }

    private fun logDeviceActorCreationOf(deviceId: String) = log.info("Created device actor for {}", deviceId)

    private fun logInvalidGroup(groupId: String) =
        log.warning(
            "Ignoring TrackDevice request for {}. This actor is responsible for {}.",
            groupId, this.groupId
        )

}