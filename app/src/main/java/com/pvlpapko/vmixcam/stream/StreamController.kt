package com.pvlpapko.vmixcam.stream

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class StreamController {
    private val _state = MutableStateFlow(StreamState.IDLE)
    val state: StateFlow<StreamState> = _state

    fun start(profile: StreamProfile) {
        _state.value = StreamState.STARTING
        // Native SRT/RTSP transport should be connected here.
        // Camera preview and controls are already isolated from transport.
        _state.value = StreamState.RUNNING(profile.srtUrl)
    }

    fun stop() {
        _state.value = StreamState.IDLE
    }
}

sealed class StreamState {
    data object IDLE : StreamState()
    data object STARTING : StreamState()
    data class RUNNING(val url: String) : StreamState()
    data class ERROR(val message: String) : StreamState()
}
