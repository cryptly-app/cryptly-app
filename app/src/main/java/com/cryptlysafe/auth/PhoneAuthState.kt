package com.cryptlysafe.cryptly.auth

data class PhoneAuthState(
    val phoneNumber: String = "",
    val otp: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isOtpSent: Boolean = false,
    val isAuthenticated: Boolean = false
) 