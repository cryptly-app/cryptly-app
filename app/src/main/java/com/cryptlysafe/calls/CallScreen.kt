package com.cryptlysafe.cryptly.calls

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptlysafe.cryptly.R
import androidx.compose.ui.graphics.vector.ImageVector


@Composable
fun CallScreen(
    callState: CallState,
    onAcceptCall: () -> Unit,
    onRejectCall: () -> Unit,
    onEndCall: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleCamera: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Caller info section
        CallerInfoSection(callState)

        Spacer(modifier = Modifier.height(32.dp))

        // Call status
        CallStatusSection(callState)

        Spacer(modifier = Modifier.weight(1f))

        // Call controls
        CallControlsSection(
            callState = callState,
            onAcceptCall = onAcceptCall,
            onRejectCall = onRejectCall,
            onEndCall = onEndCall,
            onToggleMute = onToggleMute,
            onToggleSpeaker = onToggleSpeaker,
            onToggleCamera = onToggleCamera
        )
    }
}

@Composable
private fun CallerInfoSection(callState: CallState) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(Color.Gray, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Caller Avatar",
                tint = Color.White,
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Caller Name",
            fontSize = 20.sp,
            color = Color.White
        )
    }
}

@Composable
private fun CallStatusSection(callState: CallState) {
    Text(
        text = "Duration: ${callState.callDuration}",
        fontSize = 14.sp,
        color = Color.Gray
    )
}

@Composable
private fun CallControlsSection(
    callState: CallState,
    onAcceptCall: () -> Unit,
    onRejectCall: () -> Unit,
    onEndCall: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleCamera: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mute button
        CallControlButton(
            icon = if (callState.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
            onClick = onToggleMute
        )

        // Speaker button
        CallControlButton(
            icon = if (callState.isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
            onClick = onToggleSpeaker
        )

        // Camera button
        CallControlButton(
            icon = if (callState.isCameraOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
            onClick = onToggleCamera
        )

        // End call button
        CallControlButton(
            icon = Icons.Default.CallEnd,
            onClick = onEndCall,
            backgroundColor = Color.Red
        )
    }
}

@Composable
fun CallControlButton(
    icon: ImageVector,
    onClick: () -> Unit,
    backgroundColor: Color = Color.DarkGray
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(64.dp)
            .background(backgroundColor, CircleShape)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
    }
}
