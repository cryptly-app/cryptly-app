package com.cryptlysafe.cryptly.calls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CallViewModel : ViewModel() {
    
    private val _callData = MutableStateFlow(CallDataModel())
    val callData: StateFlow<CallDataModel> = _callData.asStateFlow()
    
    private val _callState = MutableStateFlow(CallState())
    val callState: StateFlow<CallState> = _callState.asStateFlow()
    
    private var callStartTime: Long = 0
    private var durationTimer: kotlinx.coroutines.Job? = null
    
    fun initializeCall(callId: String, isIncoming: Boolean) {
        viewModelScope.launch {
            // Simulate loading call data
            delay(500)
            
            _callData.value = _callData.value.copy(
                callId = callId,
                callStatus = if (isIncoming) CallStatus.RINGING else CallStatus.CONNECTING,
                callerName = "John Doe", // Mock data - would come from Firebase
                callerId = "user123",
                isVideoCall = false,
                isIncoming = isIncoming
            )
            
            _callState.value = _callState.value.copy(
                isVideoCall = false
            )
            
            if (!isIncoming) {
                // Simulate outgoing call connection
                delay(2000)
                _callData.value = _callData.value.copy(callStatus = CallStatus.CONNECTED)
                startCallTimer()
            }
        }
    }
    
    fun acceptCall() {
        viewModelScope.launch {
            _callData.value = _callData.value.copy(callStatus = CallStatus.CONNECTING)
            
            // Simulate call acceptance
            delay(1000)
            _callData.value = _callData.value.copy(callStatus = CallStatus.CONNECTED)
            startCallTimer()
        }
    }
    
    fun rejectCall() {
        viewModelScope.launch {
            _callData.value = _callData.value.copy(callStatus = CallStatus.REJECTED)
            
            // Simulate call rejection
            delay(500)
            endCall()
        }
    }
    
    fun endCall() {
        viewModelScope.launch {
            _callData.value = _callData.value.copy(callStatus = CallStatus.ENDED)
            stopCallTimer()
            
            // Save call to log
            saveCallToLog()
            
            // Simulate call ending
            delay(500)
        }
    }
    
    fun toggleMute() {
        _callState.value = _callState.value.copy(
            isMuted = !_callState.value.isMuted
        )
        
        // TODO: Implement actual mute functionality with call service
        viewModelScope.launch {
            // Simulate mute/unmute operation
            delay(100)
        }
    }
    
    fun toggleSpeaker() {
        _callState.value = _callState.value.copy(
            isSpeakerOn = !_callState.value.isSpeakerOn
        )
        
        // TODO: Implement actual speaker functionality
        viewModelScope.launch {
            // Simulate speaker toggle
            delay(100)
        }
    }
    
    fun toggleCamera() {
        if (_callState.value.isVideoCall) {
            _callState.value = _callState.value.copy(
                isCameraOn = !_callState.value.isCameraOn
            )
            
            // TODO: Implement actual camera toggle
            viewModelScope.launch {
                // Simulate camera toggle
                delay(100)
            }
        }
    }
    
    private fun startCallTimer() {
        callStartTime = System.currentTimeMillis()
        durationTimer = viewModelScope.launch {
            while (true) {
                delay(1000)
                val duration = System.currentTimeMillis() - callStartTime
                val formattedDuration = formatDuration(duration)
                _callState.value = _callState.value.copy(callDuration = formattedDuration)
            }
        }
    }
    
    private fun stopCallTimer() {
        durationTimer?.cancel()
        durationTimer = null
    }
    
    private fun formatDuration(durationMs: Long): String {
        val seconds = (durationMs / 1000).toInt()
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }
    
    private fun saveCallToLog() {
        // TODO: Implement call log saving to Firebase
        viewModelScope.launch {
            val callLog = CallLogEntry(
                id = _callData.value.callId,
                callerId = _callData.value.callerId,
                callerName = _callData.value.callerName,
                CryptlyCallType = if (_callData.value.isVideoCall) "VIDEO" else "VOICE",
                callStatus = _callData.value.callStatus.name,
                duration = _callState.value.callDuration,
                timestamp = Date(),
                isIncoming = _callData.value.isIncoming
            )
            
            // Simulate saving to Firebase
            delay(200)
            // CallRepository.saveCallLog(callLog)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        stopCallTimer()
    }
}

data class CallLogEntry(
    val id: String,
    val callerId: String,
    val callerName: String?,
    val CryptlyCallType: String,
    val callStatus: String,
    val duration: String,
    val timestamp: Date,
    val isIncoming: Boolean
) 