package net.elenx.hd.akka.sample.iot.manager.group

import akka.actor.ActorRef
import akka.actor.PoisonPill
import akka.testkit.javadsl.TestKit
import net.elenx.hd.akka.sample.iot.manager.group.protocol.DeviceRegistered
import net.elenx.hd.akka.sample.iot.manager.group.protocol.ReplyDeviceList
import net.elenx.hd.akka.sample.iot.manager.group.protocol.RequestDeviceList
import net.elenx.hd.akka.sample.iot.manager.group.protocol.RequestTrackDevice
import net.elenx.hd.akka.sample.test.support.ActorSystemTestBase
import org.junit.Assert
import org.junit.Test

class DeviceGroupActorDeviceActionTest : ActorSystemTestBase()
{
    companion object
    {
        private const val DEVICE_GROUP_ID_HALL = "hall"
        private const val CEILING_THERMOMETER_ID_HALL = "thermometer-ceiling"
        private const val WALL_THERMOMETER_ID_HALL = "thermometer-wall"
        private const val HALL_DEVICE_COUNT = 2
    }

    @Test
    fun shouldListActiveDevices()
    {
        //given
        val mockActor = TestKit(system)
        val groupActor = system.actorOf(DeviceGroupActor.props(DEVICE_GROUP_ID_HALL))

        //when
        registerAllDevices(groupActor, mockActor)

        val registeredIds = queryActiveDeviceIds(groupActor, mockActor)

        //then
        Assert.assertEquals(HALL_DEVICE_COUNT, registeredIds.size)
        Assert.assertEquals(setOf(CEILING_THERMOMETER_ID_HALL, WALL_THERMOMETER_ID_HALL), registeredIds)

    }

    @Test
    fun testListActiveDevicesAfterOneShutsDown()
    {
        //given
        val mockActor = TestKit(system)
        val groupActor = system.actorOf(DeviceGroupActor.props(DEVICE_GROUP_ID_HALL))

        //when
        val registeredDevices = registerAllDevices(groupActor, mockActor)
        val shutdownDeviceIdRefPair = registeredDevices.first()
        val shutdownDeviceRef = shutdownDeviceIdRefPair.second
        val shutdownDeviceId = shutdownDeviceIdRefPair.first

        mockActor.watch(shutdownDeviceIdRefPair.second)
        shutdownDeviceRef.tell(PoisonPill.getInstance(), ActorRef.noSender())

        //then
        mockActor.expectTerminated(shutdownDeviceRef)

        mockActor.awaitAssert {
            assertDeviceRemoved(shutdownDeviceId, queryActiveDeviceIds(groupActor, mockActor))
        }
    }

    private fun assertDeviceRemoved(shutdownDeviceId: String, activeDeviceIds: Set<String>)
    {
        Assert.assertEquals(1, activeDeviceIds.size)
        Assert.assertFalse(activeDeviceIds.contains(shutdownDeviceId))
    }

    private fun queryActiveDeviceIds(groupActor: ActorRef, mockActor: TestKit) =
        mockActor
            .also { groupActor.tell(RequestDeviceList(0L), it.ref) }
            .expectMsgClass(ReplyDeviceList::class.java)
            .ids

    private fun registerAllDevices(groupActor: ActorRef, mockActor: TestKit) =
        setOf(
            Pair(CEILING_THERMOMETER_ID_HALL, registerDevice(groupActor, mockActor, DEVICE_GROUP_ID_HALL, CEILING_THERMOMETER_ID_HALL)),
            Pair(WALL_THERMOMETER_ID_HALL, registerDevice(groupActor, mockActor, DEVICE_GROUP_ID_HALL, WALL_THERMOMETER_ID_HALL))
        )

    private fun registerDevice(groupActor: ActorRef,
                               mockActor: TestKit,
                               groupId: String,
                               deviceId: String) =
        mockActor
            .also { groupActor.tell(RequestTrackDevice(-1L, groupId, deviceId), it.ref) }
            .also { it.expectMsgClass(DeviceRegistered::class.java) }
            .lastSender

}