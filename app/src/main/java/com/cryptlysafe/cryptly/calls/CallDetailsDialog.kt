package com.cryptlysafe.cryptly.calls

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CallDetailsDialog(
    call: CallDataModel,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                // Title
                Text(
                    text = "Call Details",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Caller Name/ID
                DetailRow(
                    label = "Caller",
                    value = call.callerName ?: call.callerId
                )
                
                // Call Type
                DetailRow(
                    label = "Type",
                    value = when (call.cryptlyCallType) {
                        CryptlyCallType.VOICE -> "Voice Call"
                        CryptlyCallType.VIDEO -> "Video Call"
                    }
                )
                
                // Duration
                DetailRow(
                    label = "Duration",
                    value = formatDuration(call.duration)
                )
                
                // Date/Time
                DetailRow(
                    label = "Date/Time",
                    value = formatDateTime(call.timestamp)
                )
                
                // Status
                DetailRow(
                    label = "Status",
                    value = call.callStatus.name.replace("_", " ").capitalize()
                )
                
                // Direction
                DetailRow(
                    label = "Direction",
                    value = if (call.isIncoming) "Incoming" else "Outgoing"
                )
                
                // Encryption Status
                DetailRow(
                    label = "Encryption",
                    value = call.encryptionStatus.name.replace("_", " ").capitalize()
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Close")
                    }
                    
                    Button(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun formatDuration(seconds: Int): String {
    return when {
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
        else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
    }
}

private fun formatDateTime(date: Date): String {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return dateFormat.format(date)
}

private fun String.capitalize(): String {
    return this.lowercase().replaceFirstChar { it.uppercase() }
} 