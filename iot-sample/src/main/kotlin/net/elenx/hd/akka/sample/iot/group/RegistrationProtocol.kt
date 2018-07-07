package net.elenx.hd.akka.sample.iot.group

data class RequestTrackDevice(val requestId: Long,
                              val groupId: String,
                              val deviceId: String)

data class DeviceRegistered(val requestId: Long)