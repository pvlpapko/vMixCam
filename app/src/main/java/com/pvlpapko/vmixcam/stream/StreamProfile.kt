package com.pvlpapko.vmixcam.stream

data class StreamProfile(
    val phoneIp: String,
    val srtPort: Int = 9999,
    val rtspPort: Int = 8554,
    val width: Int = 1920,
    val height: Int = 1080,
    val fps: Int = 30,
    val bitrateMbps: Int = 8
) {
    val srtUrl: String get() = "srt://$phoneIp:$srtPort?mode=caller"
    val rtspUrl: String get() = "rtsp://$phoneIp:$rtspPort/live"
}
