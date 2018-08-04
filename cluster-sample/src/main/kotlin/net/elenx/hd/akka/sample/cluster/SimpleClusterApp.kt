package net.elenx.hd.akka.sample.cluster

import akka.actor.ActorSystem
import akka.actor.Props
import com.typesafe.config.ConfigFactory

object SimpleClusterApp
{
    @JvmStatic
    fun main(args: Array<String>)
    {
        if (args.isEmpty())
            startup(arrayOf("2551", "2552", "0"))
        else
            startup(args)
    }

    private fun startup(ports: Array<String>) =
        ports
            .map { createConfigFor(it) }
            .map { it.withFallback(ConfigFactory.load()) }
            .map { ActorSystem.create("ClusterSystem", it) }
            .map { it.actorOf(Props.create(SimpleClusterListener::class.java), "clusterListener") }

    private fun createConfigFor(port: String) =
        ConfigFactory.parseString("""|akka.remote.netty.tcp.port=$port
                                     |akka.remote.artery.canonical.port=$port""".trimMargin())
}
