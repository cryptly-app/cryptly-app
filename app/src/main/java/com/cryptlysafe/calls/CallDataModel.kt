package com.cryptlysafe.cryptly.calls

import java.util.Date
import com.cryptlysafe.cryptly.calls.CryptlyCallType


data class CallDataModel(
    val callId: String = "",
    val callerId: String = "",
    val receiverId: String? = null,
    val callerName: String? = null,
    val callerAvatar: String? = null,
    val cryptlyCallType: CryptlyCallType = CryptlyCallType.VOICE,
    val duration: Int = 0,
    val timestamp: Date = Date(),
    val callStatus: CallStatus = CallStatus.IDLE,
    val encryptionStatus: EncryptionStatus = EncryptionStatus.UNKNOWN, // ✅ Added this line
    val isVideoCall: Boolean = false,
    val isIncoming: Boolean = false,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isCameraOn: Boolean = false,
    val isRecording: Boolean = false
)



enum class CallStatus {
    IDLE,
    RINGING,
    CONNECTING,
    CONNECTED,
    ON_HOLD,
    ENDED,
    REJECTED,
    MISSED,
    BUSY,
    FAILED
}

enum class EncryptionStatus {
    ENCRYPTED,
    ENCRYPTING,
    NOT_ENCRYPTED,
    UNKNOWN
}

data class CallParticipant(
    val userId: String,
    val name: String,
    val avatar: String?,
    val isOnline: Boolean,
    val isMuted: Boolean,
    val isVideoEnabled: Boolean,
    val signalStrength: Int
)

data class CallSettings(
    val autoRecord: Boolean = false,
    val autoMute: Boolean = false,
    val enableVideo: Boolean = true,
    val enableSpeaker: Boolean = false,
    val encryptionLevel: EncryptionLevel = EncryptionLevel.STANDARD
)

enum class EncryptionLevel {
    BASIC,
    STANDARD,
    HIGH,
    MILITARY_GRADE
} 