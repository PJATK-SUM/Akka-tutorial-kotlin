package net.elenx.hd.akka.sample.iot.manager.group.protocol

data class RequestTrackDevice(val requestId: Long,
                              val groupId: String,
                              val deviceId: String)

data class DeviceRegistered(val requestId: Long)