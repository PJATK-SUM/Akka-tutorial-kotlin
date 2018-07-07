package net.elenx.hd.akka.sample.iot.group.device

import akka.actor.AbstractActor
import akka.actor.Props
import akka.event.Logging
import net.elenx.hd.akka.sample.iot.group.DeviceRegistered
import net.elenx.hd.akka.sample.iot.group.RequestTrackDevice
import java.util.*

class DeviceActor(private val groupId: String,
                  private val deviceId: String) : AbstractActor()
{
    companion object
    {
        fun props(groupId: String, deviceId: String): Props = Props.create(DeviceActor::class.java, groupId, deviceId)
    }

    private val log = Logging.getLogger(context.system, this)

    private var lastTemperatureReading = Optional.empty<Double>()

    override fun preStart() = log.info("DeviceActor actor {}-{} started", groupId, deviceId)
    override fun postStop() = log.info("DeviceActor actor {}-{} stopped", groupId, deviceId)

    override fun createReceive(): Receive =
        receiveBuilder()
            .match(RequestTrackDevice::class.java) { trackDevice(it) }
            .match(RecordTemperature::class.java) { recordTemperature(it) }
            .match(ReadTemperature::class.java) { sender.tell(RespondTemperature(it.requestId, lastTemperatureReading), self) }
            .build()

    private fun trackDevice(request: RequestTrackDevice) =
        if (isDeviceIdCorresponds(request))
            sender.tell(DeviceRegistered(request.requestId), self)
        else
            log.warning(
                "Ignoring TrackDevice request for {}-{}. This actor is responsible for {}-{}.",
                request.groupId, request.deviceId, this.groupId, this.deviceId
            )

    private fun isDeviceIdCorresponds(request: RequestTrackDevice) =
        this.groupId == request.groupId && this.deviceId == request.deviceId

    private fun recordTemperature(record: RecordTemperature) =
        record
            .also { log.info("Recorded temperature reading {} with {}", it.value, it.requestId) }
            .also { lastTemperatureReading = Optional.of(it.value) }
            .let { sender.tell(TemperatureRecorded(it.requestId), self) }

}