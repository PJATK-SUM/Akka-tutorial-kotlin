package net.elenx.hd.akka.sample.iot.group.device

import java.util.*

data class RecordTemperature(val requestId: Long,
                             val value: Double)

data class TemperatureRecorded(val requestId: Long)

data class ReadTemperature(val requestId: Long)
data class RespondTemperature(val requestId: Long,
                              val value: Optional<Double>)