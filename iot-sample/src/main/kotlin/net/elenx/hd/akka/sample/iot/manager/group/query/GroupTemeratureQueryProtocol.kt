package net.elenx.hd.akka.sample.iot.manager.group.query

data class RequestAllTemperatures(val requestId: Long)
data class RespondAllTemperatures(val requestId: Long, val temperatures: Map<String, TemperatureReading>)
class CollectionTimeout

interface TemperatureReading

data class Temperature(val value: Double) : TemperatureReading
class TemperatureNotAvailable : TemperatureReading
class DeviceNotAvailable : TemperatureReading
class DeviceTimedOut : TemperatureReading