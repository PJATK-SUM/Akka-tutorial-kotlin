package net.elenx.hd.akka.sample.iot.device

import akka.actor.AbstractActor
import akka.actor.Props
import akka.event.Logging
import java.util.*

class Device(private val groupId: String,
             private val deviceId: String) : AbstractActor()
{
    companion object
    {
        fun props(groupId: String, deviceId: String): Props = Props.create(Device::class.java, groupId, deviceId)
    }

    private val log = Logging.getLogger(context.system, this)

    private var lastTemperatureReading = Optional.empty<Double>()

    override fun preStart() = log.info("Device actor {}-{} started", groupId, deviceId)
    override fun postStop() = log.info("Device actor {}-{} stopped", groupId, deviceId)

    override fun createReceive(): Receive =
        receiveBuilder()
            .match(RecordTemperature::class.java) { r -> recordTemperature(r) }
            .match(ReadTemperature::class.java) { r -> sender.tell(RespondTemperature(r.requestId, lastTemperatureReading), self) }
            .build()

    private fun recordTemperature(record: RecordTemperature)
    {
        log.info("Recorded temperature reading {} with {}", record.value, record.requestId)
        lastTemperatureReading = Optional.of(record.value)
        sender.tell(TemperatureRecorded(record.requestId), self)
    }

}