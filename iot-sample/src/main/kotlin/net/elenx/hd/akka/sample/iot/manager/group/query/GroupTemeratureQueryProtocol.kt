package net.elenx.hd.akka.sample.iot.manager.group.query

data class RequestAllTemperatures(val requestId: Long,
                                  val requestTimeOut: Long)
data class RespondAllTemperatures(val requestId: Long, val temperatures: Map<String, TemperatureReading>)
class CollectionTimeout

interface TemperatureReading

data class Temperature(val value: Double) : TemperatureReading
class TemperatureNotAvailable : TemperatureReading
{
    override fun equals(other: Any?): Boolean = this === other || javaClass == other?.javaClass
    override fun hashCode() = javaClass.hashCode()
}
class DeviceNotAvailable : TemperatureReading
{
    override fun equals(other: Any?): Boolean = this === other || javaClass == other?.javaClass
    override fun hashCode() = javaClass.hashCode()
}
class DeviceTimedOut : TemperatureReading
{
    override fun equals(other: Any?): Boolean = this === other || javaClass == other?.javaClass
    override fun hashCode() = javaClass.hashCode()
}