package com.cryptlysafe.cryptly.calls

data class PrivateCallLogState(
    val calls: List<PrivateCall> = emptyList(),
    val currentFilter: CallFilter = CallFilter.ALL,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class PrivateCall(
    val id: String = "",
    val phoneNumber: String = "",
    val contactName: String = "",
    val type: CryptlyCallType = CryptlyCallType.INCOMING,
    val duration: Long = 0L,
    val timestamp: Long = 0L
) {
    override fun toString(): String {
        return "$id|$phoneNumber|$contactName|$type|$duration|$timestamp"
    }

    companion object {
        fun fromString(str: String): PrivateCall {
            val parts = str.split("|")
            return PrivateCall(
                id = parts.getOrNull(0) ?: "",
                phoneNumber = parts.getOrNull(1) ?: "",
                contactName = parts.getOrNull(2) ?: "",
                type = CryptlyCallType.valueOf(parts.getOrNull(3) ?: CryptlyCallType.INCOMING.name),
                duration = parts.getOrNull(4)?.toLongOrNull() ?: 0L,
                timestamp = parts.getOrNull(5)?.toLongOrNull() ?: 0L
            )
        }
    }
}

enum class CallFilter {
    ALL,
    INCOMING,
    OUTGOING,
    MISSED
}
