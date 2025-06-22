package com.cryptlysafe.cryptly.calls

data class CallState(
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isCameraOn: Boolean = false,
    val isVideoCall: Boolean = false,
    val callDuration: String = ""
) 