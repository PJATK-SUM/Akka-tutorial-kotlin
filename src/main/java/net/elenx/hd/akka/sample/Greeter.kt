package net.elenx.hd.akka.sample

import akka.actor.AbstractActor
import akka.actor.ActorRef
import akka.actor.Props
import net.elenx.hd.akka.sample.Printer.Greeting

class Greeter(private val message: String, private val printerActor: ActorRef) : AbstractActor()
{
    companion object
    {
        fun props(message: String, printerActor: ActorRef): Props = Props.create(Greeter::class.java) { Greeter(message, printerActor) }
    }

    private var greeting = ""

    data class WhoToGreet(val who: String)
    class Greet

    override fun createReceive(): Receive =
        receiveBuilder()
            .match(WhoToGreet::class.java) { this.greeting = "$message, ${it.who}" }
            .match(Greet::class.java) { printerActor.tell(Greeting(greeting), self) }
            .build()

}

