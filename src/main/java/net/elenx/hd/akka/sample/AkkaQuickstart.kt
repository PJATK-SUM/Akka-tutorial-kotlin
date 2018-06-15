package net.elenx.hd.akka.sample

import akka.actor.ActorRef
import akka.actor.ActorSystem
import net.elenx.hd.akka.sample.Greeter.Greet
import net.elenx.hd.akka.sample.Greeter.WhoToGreet

object AkkaQuickstart
{
    @JvmStatic
    fun main(args: Array<String>)
    {
        val system = ActorSystem.create("helloakka")
        try
        {

            val printerActor = system.actorOf(Printer.props(), "printerActor")

            val howdyGreeter = system.actorOf(Greeter.props("Howdy", printerActor), "howdyGreeter")
            val helloGreeter = system.actorOf(Greeter.props("Hello", printerActor), "helloGreeter")
            val goodDayGreeter = system.actorOf(Greeter.props("Good day", printerActor), "goodDayGreeter")

            howdyGreeter.tell(WhoToGreet("Akka"), ActorRef.noSender())
            howdyGreeter.tell(Greet(), ActorRef.noSender())

            howdyGreeter.tell(WhoToGreet("Lightbend"), ActorRef.noSender())
            howdyGreeter.tell(Greet(), ActorRef.noSender())

            helloGreeter.tell(WhoToGreet("Java"), ActorRef.noSender())
            helloGreeter.tell(Greet(), ActorRef.noSender())

            goodDayGreeter.tell(WhoToGreet("Play"), ActorRef.noSender())
            goodDayGreeter.tell(Greet(), ActorRef.noSender())

            println(">>> Press ENTER to exit <<<")
            readLine()
        }
        finally
        {
            system.terminate()
        }
    }
}
