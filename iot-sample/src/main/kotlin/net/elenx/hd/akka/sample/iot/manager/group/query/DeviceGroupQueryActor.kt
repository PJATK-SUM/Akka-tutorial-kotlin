package net.elenx.hd.akka.sample.iot.manager.group.query

import akka.actor.AbstractActor
import akka.actor.ActorRef
import akka.actor.Cancellable
import akka.actor.Props
import akka.actor.Terminated
import net.elenx.hd.akka.sample.iot.manager.group.device.ReadTemperature
import net.elenx.hd.akka.sample.iot.manager.group.device.RespondTemperature
import scala.concurrent.duration.FiniteDuration

class DeviceGroupQueryActor private constructor(private val actorRefDeviceIdMap: Map<ActorRef, String>,
                                                private val requestId: Long,
                                                private val requester: ActorRef) : AbstractActor()
{
    companion object
    {
        fun props(actorRefDeviceIdMap: Map<ActorRef, String>,
                  requestId: Long,
                  requester: ActorRef,
                  timeout: FiniteDuration) =
            Props.create(DeviceGroupQueryActor::class.java) { createInstance(actorRefDeviceIdMap, requestId, requester, timeout) }

        private fun createInstance(actorRefDeviceIdMap: Map<ActorRef, String>,
                                   requestId: Long,
                                   requester: ActorRef,
                                   timeout: FiniteDuration) =
            DeviceGroupQueryActor(actorRefDeviceIdMap, requestId, requester)
                .also { it.initTimeoutTimer(timeout) }

    }

    private lateinit var queryTimeoutTimer: Cancellable

    fun initTimeoutTimer(timeout: FiniteDuration)
    {
        queryTimeoutTimer =
            context.system.scheduler().scheduleOnce(
                timeout, self, CollectionTimeout(), context.dispatcher(), self
            )
    }

    override fun preStart() =
        actorRefDeviceIdMap.keys
            .onEach { context.watch(it) }
            .forEach { it.tell(ReadTemperature(0L), self) } //ID is irrelevant in this style, but still it may be good exercise to use it instead.

    override fun postStop()
    {
        queryTimeoutTimer.cancel()
    }

    override fun createReceive(): Receive =
        waitingForReplies(HashMap(), actorRefDeviceIdMap.keys)

    private fun waitingForReplies(repliesSoFar: Map<String, TemperatureReading>,
                                  stillWaiting: Set<ActorRef>): Receive =
        receiveBuilder()
            .match(RespondTemperature::class.java) { this.onRespondTemperature(it, repliesSoFar, stillWaiting) }
            .match(Terminated::class.java) { handleResponse(it.actor, DeviceNotAvailable(), repliesSoFar, stillWaiting) }
            .match(CollectionTimeout::class.java) { onCollectionTimeout(repliesSoFar, stillWaiting) }
            .build()

    private fun onRespondTemperature(respond: RespondTemperature,
                                     repliesSoFar: Map<String, TemperatureReading>,
                                     stillWaiting: Set<ActorRef>) =
        respond.value
            .map { Temperature(it) as TemperatureReading }
            .orElse(TemperatureNotAvailable())
            .let { handleResponse(sender, it, repliesSoFar, stillWaiting) }

    private fun handleResponse(actor: ActorRef,
                               temperatureReading: TemperatureReading,
                               repliesSoFar: Map<String, TemperatureReading>,
                               stillWaiting: Set<ActorRef>) =
        dequeFromWatching(actor)
            .let { repliesSoFar + Pair(it, temperatureReading) }
            .let { updateStateWith(stillWaiting - actor, it) }

    private fun dequeFromWatching(actor: ActorRef) =
        actor
            .also { context.unwatch(it) }
            .let { actorRefDeviceIdMap[actor]!! }

    private fun updateStateWith(stillWaiting: Set<ActorRef>, repliesSoFar: Map<String, TemperatureReading>) =
        if (stillWaiting.isEmpty())
            requester
                .tell(RespondAllTemperatures(requestId, repliesSoFar), self)
                .also { context.stop(self) }
        else
            context.become(waitingForReplies(repliesSoFar, stillWaiting))

    private fun onCollectionTimeout(repliesSoFar: Map<String, TemperatureReading>,
                                    stillWaiting: Set<ActorRef>) =
        repliesSoFar
            .plus(gatherOutOfTime(stillWaiting))
            .let { requester.tell(RespondAllTemperatures(requestId, it), self) }
            .apply { context.stop(self) }

    private fun gatherOutOfTime(stillWaiting: Set<ActorRef>) =
        stillWaiting
            .map { actorRefDeviceIdMap[it]!! }
            .map { Pair(it, DeviceTimedOut()) }
            .toSet()

}