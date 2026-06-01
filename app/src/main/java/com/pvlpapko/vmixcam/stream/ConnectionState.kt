package com.pvlpapko.vmixcam.stream

data class ConnectionState(
    val mode: ConnectionMode = ConnectionMode.SRT_LISTENER,
    val phoneIp: String = "0.0.0.0",
    val vmixIp: String = "",
    val port: Int = 9999,
    val latency: Int = 120,
    val isReady: Boolean = false
) {
    fun vmixUrl(): String = when (mode) {
        ConnectionMode.SRT_LISTENER -> "srt://$phoneIp:$port?mode=caller&latency=$latency"
        ConnectionMode.SRT_CALLER -> "srt://$vmixIp:$port?mode=listener&latency=$latency"
        ConnectionMode.RTSP -> "rtsp://$phoneIp:8554/live"
    }

    fun statusText(): String = when (mode) {
        ConnectionMode.SRT_LISTENER -> "SRT ГОТОВ: $phoneIp:$port"
        ConnectionMode.SRT_CALLER -> "SRT CALLER: $vmixIp:$port"
        ConnectionMode.RTSP -> "RTSP ГОТОВ: $phoneIp:8554/live"
    }
}
