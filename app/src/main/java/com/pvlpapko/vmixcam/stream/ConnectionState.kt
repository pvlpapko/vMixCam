package com.pvlpapko.vmixcam.stream

data class ConnectionState(
    val mode: ConnectionMode = ConnectionMode.SRT_LISTENER,
    val ip: String = "",
    val port: Int = 9999,
    val isStreaming: Boolean = false
) {

    fun outputUrl(): String {
        return when (mode) {
            ConnectionMode.SRT_LISTENER ->
                "srt://$ip:$port?mode=caller&latency=120"

            ConnectionMode.SRT_CALLER ->
                "srt://$ip:$port?mode=listener"

            ConnectionMode.RTSP ->
                "rtsp://$ip:8554/live"
        }
    }
}
