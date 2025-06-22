package com.cryptlysafe.cryptly.private

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PrivatePinViewModel : ViewModel() {
    
    companion object {
        private const val CORRECT_PIN = "1234"
        private const val MAX_PIN_LENGTH = 4
    }
    
    private val _pinState = MutableStateFlow(PrivatePinState())
    val pinState: StateFlow<PrivatePinState> = _pinState.asStateFlow()
    
    fun updatePin(newPin: String) {
        // Only allow numeric input and limit to 4 digits
        val filteredPin = newPin.filter { it.isDigit() }.take(MAX_PIN_LENGTH)
        
        _pinState.value = _pinState.value.copy(
            pin = filteredPin,
            showError = false,
            errorMessage = ""
        )
    }
    
    fun validatePin() {
        val currentPin = _pinState.value.pin
        
        if (currentPin.isEmpty()) {
            _pinState.value = _pinState.value.copy(
                showError = true,
                errorMessage = "Please enter a PIN"
            )
            return
        }
        
        if (currentPin.length < MAX_PIN_LENGTH) {
            _pinState.value = _pinState.value.copy(
                showError = true,
                errorMessage = "PIN must be 4 digits"
            )
            return
        }
        
        if (currentPin == CORRECT_PIN) {
            _pinState.value = _pinState.value.copy(
                isPinCorrect = true,
                showError = false,
                errorMessage = ""
            )
        } else {
            _pinState.value = _pinState.value.copy(
                showError = true,
                errorMessage = "Incorrect PIN. Please try again.",
                pin = "" // Clear the PIN for security
            )
        }
    }
    
    fun resetPinState() {
        _pinState.value = PrivatePinState()
    }
    
    fun clearError() {
        _pinState.value = _pinState.value.copy(
            showError = false,
            errorMessage = ""
        )
    }
}

data class PrivatePinState(
    val pin: String = "",
    val isPinCorrect: Boolean = false,
    val showError: Boolean = false,
    val errorMessage: String = "",
    val isLoading: Boolean = false
) 