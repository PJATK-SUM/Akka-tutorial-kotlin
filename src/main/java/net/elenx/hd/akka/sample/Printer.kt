package net.elenx.hd.akka.sample

import akka.actor.AbstractActor
import akka.actor.Props
import akka.event.Logging

class Printer : AbstractActor()
{
    companion object
    {
        fun props(): Props = Props.create(Printer::class.java) { Printer() }
    }

    private val log = Logging.getLogger(context.system, this)

    class Greeting(val message: String)

    override fun createReceive(): AbstractActor.Receive =
        receiveBuilder()
            .match(Greeting::class.java) { log.info(it.message) }
            .build()

}

