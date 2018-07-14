package net.elenx.hd.akka.sample.iot.group

data class RequestDeviceList(val requestId: Long)
data class ReplyDeviceList(val requestId: Long,
                           val ids: Set<String>)