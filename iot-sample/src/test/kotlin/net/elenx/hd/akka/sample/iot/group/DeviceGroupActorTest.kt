package net.elenx.hd.akka.sample.iot.group

import akka.testkit.javadsl.TestKit
import net.elenx.hd.akka.sample.iot.group.device.RecordTemperature
import net.elenx.hd.akka.sample.iot.group.device.TemperatureRecorded
import net.elenx.hd.akka.sample.test.support.ActorSystemTestBase
import org.junit.Assert
import org.junit.Test

class DeviceGroupActorTest : ActorSystemTestBase()
{
    companion object
    {
        private const val DEVICE_GROUP_ID_HALL = "hall"
        private const val THERMOMETER_ID_HALL = "thermometer-a13"
        private const val BAROMETER_ID_HALL = "barometer-s0"

        private const val DEVICE_GROUP_ID_GARAGE = "garage"

    }

    @Test
    fun testRegisterDeviceActor()
    {
        val probe = TestKit(system)
        val groupActor = system.actorOf(DeviceGroupActor.props(DEVICE_GROUP_ID_HALL))

        groupActor.tell(RequestTrackDevice(1L, DEVICE_GROUP_ID_HALL, THERMOMETER_ID_HALL), probe.ref)
        probe.expectMsgClass(DeviceRegistered::class.java)
        val deviceActor1 = probe.lastSender

        groupActor.tell(RequestTrackDevice(1L, DEVICE_GROUP_ID_HALL, BAROMETER_ID_HALL), probe.ref)
        probe.expectMsgClass(DeviceRegistered::class.java)
        val deviceActor2 = probe.lastSender
        Assert.assertNotEquals(deviceActor1, deviceActor2)

        // Check that the device actors are working
        deviceActor1.tell(RecordTemperature(0L, 1.0), probe.ref)
        Assert.assertEquals(0L, probe.expectMsgClass(TemperatureRecorded::class.java).requestId)
        deviceActor2.tell(RecordTemperature(1L, 2.0), probe.ref)
        Assert.assertEquals(1L, probe.expectMsgClass(TemperatureRecorded::class.java).requestId)
    }

    @Test
    fun testIgnoreRequestsForWrongGroupId()
    {
        val probe = TestKit(system)
        val groupActor = system.actorOf(DeviceGroupActor.props(DEVICE_GROUP_ID_HALL))

        groupActor.tell(RequestTrackDevice(1L, DEVICE_GROUP_ID_GARAGE, THERMOMETER_ID_HALL), probe.ref)
        probe.expectNoMsg()
    }
}