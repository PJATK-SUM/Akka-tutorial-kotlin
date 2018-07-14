package net.elenx.hd.akka.sample.iot.manager.group.query

import akka.actor.ActorRef
import akka.actor.PoisonPill
import akka.testkit.javadsl.TestKit
import net.elenx.hd.akka.sample.iot.manager.group.device.ReadTemperature
import net.elenx.hd.akka.sample.iot.manager.group.device.RespondTemperature
import net.elenx.hd.akka.sample.test.support.ActorSystemTestBase
import org.junit.Assert
import org.junit.Test
import scala.concurrent.duration.FiniteDuration
import java.util.*
import java.util.concurrent.TimeUnit

class DeviceGroupQueryActorTest : ActorSystemTestBase()
{
    companion object
    {
        private const val CEILING_THERMOMETER_ID = "ceiling-thermometer"
        private val ceilingTemperature = Temperature(1.0)

        private const val WALL_THERMOMETER_ID = "ceiling-wall"
        private val wallTemperature = Temperature(2.0)

        private const val QUERY_REQUEST_ID = 1L
        private const val QUERY_TIMEOUT = 3L
    }

    @Test
    fun shouldReturnTemperatureValueForWorkingDevices()
    {
        //given
        val (requester, ceilingThermometer, wallThermometer) = createMockActors()
        val actorRefDeviceIdMap = createActorRefDeviceIdMap(ceilingThermometer, wallThermometer)
        val expectedTemperatures = createExpectedTemperatures(ceilingTemperature, wallTemperature)

        //when
        val queryActor = createQueryActor(actorRefDeviceIdMap, requester)

        ceilingThermometer.expectMsgClass(ReadTemperature::class.java)
        queryActor.tell(RespondTemperature(0L, Optional.of(1.0)), ceilingThermometer.ref)

        wallThermometer.expectMsgClass(ReadTemperature::class.java)
        queryActor.tell(RespondTemperature(0L, Optional.of(2.0)), wallThermometer.ref)

        val response = requester.expectMsgClass(RespondAllTemperatures::class.java)

        //then
        Assert.assertEquals(QUERY_REQUEST_ID, response.requestId)
        Assert.assertEquals(expectedTemperatures, response.temperatures)

    }

    @Test
    fun testReturnTemperatureNotAvailableForDevicesWithNoReadings()
    {
        //given
        val (requester, ceilingThermometer, wallThermometer) = createMockActors()
        val actorRefDeviceIdMap = createActorRefDeviceIdMap(ceilingThermometer, wallThermometer)
        val expectedTemperatures = createExpectedTemperatures(TemperatureNotAvailable(), wallTemperature)

        //when
        val queryActor = createQueryActor(actorRefDeviceIdMap, requester)

        queryActor.tell(RespondTemperature(0L, Optional.empty()), ceilingThermometer.ref)
        queryActor.tell(RespondTemperature(0L, Optional.of(2.0)), wallThermometer.ref)

        val response = requester.expectMsgClass(RespondAllTemperatures::class.java)

        //then
        Assert.assertEquals(1L, response.requestId)
        Assert.assertEquals(expectedTemperatures, response.temperatures)

    }

    @Test
    fun testReturnDeviceNotAvailableIfDeviceStopsBeforeAnswering()
    {

        //given
        val (requester, ceilingThermometer, wallThermometer) = createMockActors()
        val actorRefDeviceIdMap = createActorRefDeviceIdMap(ceilingThermometer, wallThermometer)
        val expectedTemperatures = createExpectedTemperatures(ceilingTemperature, DeviceNotAvailable())

        //when
        val queryActor = createQueryActor(actorRefDeviceIdMap, requester)

        queryActor.tell(RespondTemperature(0L, Optional.of(1.0)), ceilingThermometer.ref)
        wallThermometer.ref.tell(PoisonPill.getInstance(), ActorRef.noSender())

        val response = requester.expectMsgClass(RespondAllTemperatures::class.java)

        //then
        Assert.assertEquals(1L, response.requestId)
        Assert.assertEquals(expectedTemperatures, response.temperatures)

    }

    @Test
    fun testReturnTemperatureReadingEvenIfDeviceStopsAfterAnswering()
    {
        //given
        val (requester, ceilingThermometer, wallThermometer) = createMockActors()
        val actorRefDeviceIdMap = createActorRefDeviceIdMap(ceilingThermometer, wallThermometer)
        val expectedTemperatures = createExpectedTemperatures(ceilingTemperature, wallTemperature)

        //when
        val queryActor = createQueryActor(actorRefDeviceIdMap, requester)

        queryActor.tell(RespondTemperature(0L, Optional.of(1.0)), ceilingThermometer.ref)
        queryActor.tell(RespondTemperature(0L, Optional.of(2.0)), wallThermometer.ref)
        wallThermometer.ref.tell(PoisonPill.getInstance(), ActorRef.noSender())

        val response = requester.expectMsgClass(RespondAllTemperatures::class.java)

        //then
        Assert.assertEquals(1L, response.requestId)
        Assert.assertEquals(expectedTemperatures, response.temperatures)

    }

    @Test
    fun testReturnDeviceTimedOutIfDeviceDoesNotAnswerInTime()
    {
        //given
        val (requester, ceilingThermometer, wallThermometer) = createMockActors()
        val actorRefDeviceIdMap = createActorRefDeviceIdMap(ceilingThermometer, wallThermometer)
        val expectedTemperatures = createExpectedTemperatures(ceilingTemperature, DeviceTimedOut())

        //when
        val queryActor = createQueryActor(actorRefDeviceIdMap, requester)

        queryActor.tell(RespondTemperature(0L, Optional.of(1.0)), ceilingThermometer.ref)

        val response = requester.expectMsgClass(FiniteDuration(5, TimeUnit.SECONDS), RespondAllTemperatures::class.java)

        //then
        Assert.assertEquals(1L, response.requestId)
        Assert.assertEquals(expectedTemperatures, response.temperatures)

    }

    private fun createMockActors(): Triple<TestKit, TestKit, TestKit> = Triple(TestKit(system), TestKit(system), TestKit(system))

    private fun createActorRefDeviceIdMap(ceilingThermometer: TestKit, wallThermometer: TestKit) =
        mapOf(
            Pair(ceilingThermometer.ref, CEILING_THERMOMETER_ID),
            Pair(wallThermometer.ref, WALL_THERMOMETER_ID)
        )

    private fun createExpectedTemperatures(ceilingTemperature: TemperatureReading, wallTemperature: TemperatureReading) =
        mapOf(
            Pair(CEILING_THERMOMETER_ID, ceilingTemperature),
            Pair(WALL_THERMOMETER_ID, wallTemperature)
        )

    private fun createQueryActor(actorRefDeviceIdMap: Map<ActorRef, String>, requester: TestKit) =
        system.actorOf(
            DeviceGroupQueryActor.props(
                actorRefDeviceIdMap,
                QUERY_REQUEST_ID,
                requester.ref,
                FiniteDuration(QUERY_TIMEOUT, TimeUnit.SECONDS)))

}