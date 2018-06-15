package net.elenx.hd.akka.sample

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.testkit.javadsl.TestKit
import net.elenx.hd.akka.sample.Greeter.Greet
import net.elenx.hd.akka.sample.Greeter.WhoToGreet
import net.elenx.hd.akka.sample.Printer.Greeting
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test

class AkkaQuickstartTest
{

    companion object
    {
        internal var system: ActorSystem? = null

        @BeforeClass
        @JvmStatic
        fun setup()
        {
            system = ActorSystem.create()
        }

        @AfterClass
        @JvmStatic
        fun teardown()
        {
            TestKit.shutdownActorSystem(system!!)
            system = null
        }
    }

    @Test
    fun testGreeterActorSendingOfGreeting()
    {
        val testProbe = TestKit(system)
        val helloGreeter = system!!.actorOf(Greeter.props("Hello", testProbe.ref))
        helloGreeter.tell(WhoToGreet("Akka"), ActorRef.noSender())
        helloGreeter.tell(Greet(), ActorRef.noSender())
        val greeting = testProbe.expectMsgClass(Greeting::class.java)
        assertEquals("Hello, Akka", greeting.message)
    }
}
