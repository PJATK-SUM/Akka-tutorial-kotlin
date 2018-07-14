package net.elenx.hd.akka.sample.iot.manager.group

import akka.actor.ActorRef
import akka.testkit.javadsl.TestKit
import net.elenx.hd.akka.sample.iot.manager.group.device.RecordTemperature
import net.elenx.hd.akka.sample.iot.manager.group.device.TemperatureRecorded
import net.elenx.hd.akka.sample.iot.manager.group.protocol.DeviceRegistered
import net.elenx.hd.akka.sample.iot.manager.group.protocol.RequestTrackDevice
import net.elenx.hd.akka.sample.iot.manager.group.query.RequestAllTemperatures
import net.elenx.hd.akka.sample.iot.manager.group.query.RespondAllTemperatures
import net.elenx.hd.akka.sample.iot.manager.group.query.Temperature
import net.elenx.hd.akka.sample.iot.manager.group.query.TemperatureNotAvailable
import net.elenx.hd.akka.sample.iot.manager.group.query.TemperatureReading
import net.elenx.hd.akka.sample.test.support.ActorSystemTestBase
import org.junit.Assert
import org.junit.Test

class DeviceGroupActorQueryTest : ActorSystemTestBase()
{
    companion object
    {
        private const val HALL_GROUP_ID = "group"

        private const val CEILING_THERMOMETER_ID = "thermometer-ceiling"
        private val ceilingTemperature = Temperature(1.0)

        private const val WALL_THERMOMETER_ID = "thermometer-wall"
        private val wallTemperature = Temperature(2.0)

        private const val FLOOR_THERMOMETER_ID = "thermometer-floor"
        private val floorTemperature = TemperatureNotAvailable()

        private const val REQUEST_TIMEOUT = 3L
    }

    @Test
    fun testCollectTemperaturesFromAllActiveDevices()
    {
        //given
        val mockActor = TestKit(system)
        val groupActor = system.actorOf(DeviceGroupActor.props(HALL_GROUP_ID))

        val ceilingThermometer = registerDeviceActor(groupActor, mockActor, CEILING_THERMOMETER_ID)
        val wallThermometer = registerDeviceActor(groupActor, mockActor, WALL_THERMOMETER_ID)
        registerDeviceActor(groupActor, mockActor, FLOOR_THERMOMETER_ID)

        val expectedTemperatures = createExpectedTemperatures()

        //when
        recordTemperature(ceilingThermometer, ceilingTemperature.value, mockActor)
        recordTemperature(wallThermometer, wallTemperature.value, mockActor)

        groupActor.tell(RequestAllTemperatures(0L, REQUEST_TIMEOUT), mockActor.ref)
        val response = mockActor.expectMsgClass(RespondAllTemperatures::class.java)

        //then
        Assert.assertEquals(expectedTemperatures, response.temperatures)

    }

    private fun recordTemperature(deviceActor: ActorRef, value: Double, mockActor: TestKit): Unit =
        mockActor
            .also { deviceActor.tell(RecordTemperature(0L, value), it.ref) }
            .let { it.expectMsgClass(TemperatureRecorded::class.java) }

    private fun registerDeviceActor(groupActor: ActorRef, mockActor: TestKit, deviceId: String) =
        mockActor
            .also { groupActor.tell(RequestTrackDevice(0L, HALL_GROUP_ID, deviceId), it.ref) }
            .also { it.expectMsgClass(DeviceRegistered::class.java) }
            .lastSender

    private fun createExpectedTemperatures(): Map<String, TemperatureReading> =
        mapOf(
            Pair(CEILING_THERMOMETER_ID, ceilingTemperature),
            Pair(WALL_THERMOMETER_ID, wallTemperature),
            Pair(FLOOR_THERMOMETER_ID, floorTemperature)
        )
}
