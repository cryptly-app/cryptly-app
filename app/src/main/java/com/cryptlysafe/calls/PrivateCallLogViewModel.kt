package com.cryptlysafe.cryptly.calls

import android.app.Application
import android.content.Context
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.*

// Dummy Enums and Data Models (Add actual implementation as per your project)
enum class CallStatus { ENDED, MISSED }
enum class CryptlyCallType { INCOMING, OUTGOING, MISSED }
enum class EncryptionStatus { ENCRYPTED }
data class CallDataModel(
    val callId: String = "",
    val callerId: String = "",
    val receiverId: String = "",
    val CryptlyCallType: CryptlyCallType = CryptlyCallType.INCOMING,
    val duration: Int = 0,
    val timestamp: Date = Date(),
    val callerName: String = "",
    val isVideoCall: Boolean = false,
    val isIncoming: Boolean = true,
    val callStatus: CallStatus = CallStatus.ENDED,
    val encryptionStatus: EncryptionStatus = EncryptionStatus.ENCRYPTED
)

enum class CallFilter { ALL, INCOMING, OUTGOING, MISSED }

data class PrivateCallLogState(
    val calls: List<CallDataModel> = emptyList(),
    val currentFilter: CallFilter = CallFilter.ALL,
    val isLoading: Boolean = false,
    val error: String? = null
)

class PrivateCallLogViewModel(application: Application) : AndroidViewModel(application) {

    private val callRepository = CallRepository() // You must implement this
    private val _uiState = MutableStateFlow(PrivateCallLogState())
    val uiState: StateFlow<PrivateCallLogState> = _uiState.asStateFlow()
    private val _callLogs = MutableStateFlow<List<CallDataModel>>(emptyList())
    val callLogs: StateFlow<List<CallDataModel>> = _callLogs.asStateFlow()
    private var currentUserId: String = getCurrentUserId()

    private var currentCallStartTime: Long = 0
    private var currentCallNumber: String = ""
    private var currentCryptlyCallType: CryptlyCallType = CryptlyCallType.INCOMING

    init {
        loadCallLogs(currentUserId)
        setupCallListener()
    }

    fun loadCallLogs(userId: String) {
        currentUserId = userId
        callRepository.getCallLogs(userId) { callLogs ->
            _callLogs.value = callLogs
            _uiState.update { it.copy(isLoading = false, error = null) }
        }
    }

    fun logCall(call: CallDataModel) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            callRepository.logCall(call).onSuccess {
                _uiState.update { it.copy(isLoading = false, error = null) }
                loadCallLogs(currentUserId)
            }.onFailure {
                _uiState.update { it.copy(isLoading = false, error = it.message ?: "Log failed") }
            }
        }
    }

    fun deleteCall(callId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            callRepository.deleteCall(callId, currentUserId).onSuccess {
                _uiState.update { it.copy(isLoading = false) }
                loadCallLogs(currentUserId)
            }.onFailure {
                _uiState.update { it.copy(isLoading = false, error = it.message ?: "Delete failed") }
            }
        }
    }

    fun setFilter(filter: CallFilter) {
        _uiState.update { it.copy(currentFilter = filter) }
        val filteredLogs = when (filter) {
            CallFilter.ALL -> _callLogs.value
            CallFilter.INCOMING -> _callLogs.value.filter { it.isIncoming }
            CallFilter.OUTGOING -> _callLogs.value.filter { !it.isIncoming }
            CallFilter.MISSED -> _callLogs.value.filter { it.callStatus == CallStatus.MISSED }
        }
        _callLogs.value = filteredLogs
    }

    fun addCall(phoneNumber: String, contactName: String, type: CryptlyCallType, duration: Long) {
        val call = CallDataModel(
            callId = generateCallId(),
            callerId = currentUserId,
            receiverId = phoneNumber,
            CryptlyCallType = type,
            duration = duration.toInt(),
            timestamp = Date(),
            callerName = contactName,
            isVideoCall = false,
            isIncoming = type == CryptlyCallType.INCOMING,
            callStatus = if (duration > 0) CallStatus.ENDED else CallStatus.MISSED,
            encryptionStatus = EncryptionStatus.ENCRYPTED
        )
        logCall(call)
    }

    fun stopListeningToCallLogs() {
        callRepository.stopListeningToCallLogs(currentUserId)
    }

    override fun onCleared() {
        super.onCleared()
        callRepository.stopAllListeners()
    }

    private fun setupCallListener() {
        val context = getApplication<Application>().applicationContext
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        val phoneStateListener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                when (state) {
                    TelephonyManager.CALL_STATE_RINGING -> {
                        currentCallStartTime = System.currentTimeMillis()
                        currentCallNumber = phoneNumber ?: ""
                        currentCryptlyCallType = CryptlyCallType.INCOMING
                    }
                    TelephonyManager.CALL_STATE_OFFHOOK -> {
                        if (currentCallStartTime == 0L) {
                            currentCallStartTime = System.currentTimeMillis()
                            currentCryptlyCallType = CryptlyCallType.OUTGOING
                        }
                    }
                    TelephonyManager.CALL_STATE_IDLE -> {
                        if (currentCallStartTime > 0) {
                            val duration = (System.currentTimeMillis() - currentCallStartTime) / 1000
                            if (duration > 0) {
                                addCall(currentCallNumber, "", currentCryptlyCallType, duration)
                            } else if (currentCryptlyCallType == CryptlyCallType.INCOMING) {
                                addCall(currentCallNumber, "", CryptlyCallType.MISSED, 0)
                            }
                            currentCallStartTime = 0
                            currentCallNumber = ""
                        }
                    }
                }
            }
        }

        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
    }

    private fun generateCallId(): String {
        return "call_${System.currentTimeMillis()}_${UUID.randomUUID().toString().substring(0, 8)}"
    }

    private fun getCurrentUserId(): String {
        return FirebaseAuth.getInstance().currentUser?.uid ?: "unknown_user"
    }
}
