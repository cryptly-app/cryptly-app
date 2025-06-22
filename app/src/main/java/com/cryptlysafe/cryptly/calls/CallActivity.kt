package com.cryptlysafe.cryptly.calls

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class CallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val callId = intent.getStringExtra("call_id")
        val isIncoming = intent.getBooleanExtra("is_incoming", false)


        setContent {
            if (callId != null) {
                CallScreen(
                    callId = callId,
                    isIncoming = isIncoming,
                    onCallEnded = { finish() }
                )
            } else {
                // If callId is null, we can't proceed.
                finish()
            }
        }
    }
}

@Composable
fun CallScreen(callId: String, isIncoming: Boolean, onCallEnded: () -> Unit) {
    // For now, this is a placeholder.
    // In a real app, this would contain the call UI.
    Text(text = "Call screen for call ID: $callId. Incoming: $isIncoming")
    // To end the call, you would call onCallEnded()
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    CallScreen(callId = "preview_call_id", isIncoming = true, onCallEnded = {})
} 