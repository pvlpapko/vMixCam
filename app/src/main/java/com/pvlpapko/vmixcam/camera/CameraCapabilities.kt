package com.pvlpapko.vmixcam.camera

data class CameraCapabilities(
    val supportsOis: Boolean = false,
    val supportsEis: Boolean = false,
    val supportsManualFocus: Boolean = true,
    val supportsManualExposure: Boolean = true,
    val maxZoom: Float = 8f
)
