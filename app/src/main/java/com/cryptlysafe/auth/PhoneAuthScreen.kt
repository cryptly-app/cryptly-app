package com.cryptlysafe.cryptly.auth

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneAuthScreen(
    navController: NavController,
    viewModel: PhoneAuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as ComponentActivity

    val uiState by viewModel.uiState.collectAsState()
    var showResendTimer by remember { mutableStateOf(false) }
    var resendTimer by remember { mutableStateOf(60) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Navigation on success
    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) {
            navController.navigate("private_mode") {
                popUpTo("auth_screen") { inclusive = true }
            }
        }
    }

    // Resend OTP timer logic
    LaunchedEffect(showResendTimer) {
        if (showResendTimer) {
            while (resendTimer > 0) {
                delay(1000)
                resendTimer--
            }
            showResendTimer = false
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            TextField(
                value = uiState.phoneNumber,
                onValueChange = { viewModel.onPhoneNumberChange(it) },
                label = { Text("Phone Number") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    viewModel.sendOtp(activity)
                    showResendTimer = true
                    resendTimer = 60
                },
                enabled = !uiState.isLoading && !showResendTimer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Send OTP")
            }

            if (uiState.isOtpSent) {
                Spacer(modifier = Modifier.height(16.dp))

                TextField(
                    value = uiState.otp,
                    onValueChange = { viewModel.onOtpChange(it) },
                    label = { Text("Enter OTP") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.verifyOtp() },
                    enabled = !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Verify OTP")
                }

                if (showResendTimer) {
                    Text("Resend in $resendTimer seconds")
                }
            }

            if (uiState.error != null) {
                LaunchedEffect(uiState.error) {
                    snackbarHostState.showSnackbar(uiState.error!!)
                }
            }
        }
    }
}
