package net.elenx.hd.akka.sample.test.support

import akka.actor.ActorSystem
import akka.testkit.javadsl.TestKit
import org.junit.AfterClass
import org.junit.BeforeClass

abstract class ActorSystemTestBase
{
    companion object
    {
        lateinit var system: ActorSystem

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
            TestKit.shutdownActorSystem(system)
        }
    }
}