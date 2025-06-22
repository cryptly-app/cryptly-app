package com.cryptlysafe.cryptly.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatBubble(
    message: ChatMessage,
    isOwnMessage: Boolean,
    onMessageClick: (ChatMessage) -> Unit = {},
    onMessageLongClick: (ChatMessage) -> Unit = {},
    onReactionClick: (ChatMessage, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isOwnMessage) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    val textColor = if (isOwnMessage) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start
    ) {
        // Message bubble
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isOwnMessage) 16.dp else 4.dp,
                        bottomEnd = if (isOwnMessage) 4.dp else 16.dp
                    )
                )
                .background(backgroundColor)
                .clickable { onMessageClick(message) }
                .padding(12.dp)
        ) {
            Column {
                // Reply to message (if any)
                message.replyTo?.let { replyId ->
                    ReplyMessagePreview(
                        replyId = replyId,
                        textColor = textColor.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // Message content
                when (message.messageType) {
                    MessageType.TEXT -> TextMessageContent(message, textColor)
                    MessageType.IMAGE -> ImageMessageContent(message, textColor)
                    MessageType.VIDEO -> VideoMessageContent(message, textColor)
                    MessageType.AUDIO -> AudioMessageContent(message, textColor)
                    MessageType.DOCUMENT -> DocumentMessageContent(message, textColor)
                    MessageType.LOCATION -> LocationMessageContent(message, textColor)
                    MessageType.CONTACT -> ContactMessageContent(message, textColor)
                    MessageType.VOICE_MESSAGE -> VoiceMessageContent(message, textColor)
                    else -> TextMessageContent(message, textColor)
                }
                
                // Message metadata
                MessageMetadata(message, textColor, isOwnMessage)
            }
        }
        
        // Reactions
        if (message.reactions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            MessageReactions(
                reactions = message.reactions,
                onReactionClick = { emoji -> onReactionClick(message, emoji) }
            )
        }
    }
}

@Composable
private fun TextMessageContent(message: ChatMessage, textColor: Color) {
    Text(
        text = message.content,
        color = textColor,
        fontSize = 16.sp,
        lineHeight = 20.sp
    )
}

@Composable
private fun ImageMessageContent(message: ChatMessage, textColor: Color) {
    Column {
        // TODO: Implement image display with Coil or Glide
        Box(
            modifier = Modifier
                .size(200.dp, 150.dp)
                .background(Color.Gray.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = "Image",
                tint = textColor.copy(alpha = 0.6f),
                modifier = Modifier.size(48.dp)
            )
        }
        
        if (message.content.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message.content,
                color = textColor,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun VideoMessageContent(message: ChatMessage, textColor: Color) {
    Column {
        Box(
            modifier = Modifier
                .size(200.dp, 150.dp)
                .background(Color.Gray.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Video",
                tint = textColor.copy(alpha = 0.6f),
                modifier = Modifier.size(48.dp)
            )
        }
        
        if (message.content.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message.content,
                color = textColor,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun AudioMessageContent(message: ChatMessage, textColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Play Audio",
            tint = textColor,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Audio waveform or duration
        Text(
            text = message.mediaDuration?.let { "${it}s" } ?: "Audio",
            color = textColor,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun DocumentMessageContent(message: ChatMessage, textColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.Description,
            contentDescription = "Document",
            tint = textColor,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Column {
            Text(
                text = message.content.ifEmpty { "Document" },
                color = textColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            
            if (message.mediaSize > 0) {
                Text(
                    text = formatFileSize(message.mediaSize),
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun LocationMessageContent(message: ChatMessage, textColor: Color) {
    Column {
        Box(
            modifier = Modifier
                .size(200.dp, 120.dp)
                .background(Color.Gray.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Location",
                tint = textColor.copy(alpha = 0.6f),
                modifier = Modifier.size(48.dp)
            )
        }
        
        message.location?.let { location ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = location.address ?: "Location shared",
                color = textColor,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ContactMessageContent(message: ChatMessage, textColor: Color) {
    message.contact?.let { contact ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Contact",
                tint = textColor,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column {
                Text(
                    text = contact.name,
                    color = textColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = contact.phoneNumber,
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun VoiceMessageContent(message: ChatMessage, textColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Voice Message",
            tint = textColor,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = message.mediaDuration?.let { "${it}s" } ?: "Voice Message",
            color = textColor,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun ReplyMessagePreview(
    replyId: String,
    textColor: Color
) {
    // TODO: Load and display the replied message
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(32.dp)
                .background(textColor)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = "Reply to message",
            color = textColor,
            fontSize = 12.sp,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
        )
    }
}

@Composable
private fun MessageMetadata(
    message: ChatMessage,
    textColor: Color,
    isOwnMessage: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(top = 4.dp)
    ) {
        // Timestamp
        Text(
            text = formatTimestamp(message.timestamp),
            color = textColor.copy(alpha = 0.7f),
            fontSize = 11.sp
        )
        
        // Status indicators for own messages
        if (isOwnMessage) {
            when (message.status) {
                MessageStatus.SENDING -> Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = "Sending",
                    tint = textColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(12.dp)
                )
                MessageStatus.SENT -> Icon(
                    imageVector = Icons.Default.Done,
                    contentDescription = "Sent",
                    tint = textColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(12.dp)
                )
                MessageStatus.DELIVERED -> Icon(
                    imageVector = Icons.Default.DoneAll,
                    contentDescription = "Delivered",
                    tint = textColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(12.dp)
                )
                MessageStatus.READ -> Icon(
                    imageVector = Icons.Default.DoneAll,
                    contentDescription = "Read",
                    tint = textColor,
                    modifier = Modifier.size(12.dp)
                )
                MessageStatus.FAILED -> Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Failed",
                    tint = Color.Red,
                    modifier = Modifier.size(12.dp)
                )
                else -> {}
            }
        }
        
        // Edited indicator
        if (message.isEdited) {
            Text(
                text = "edited",
                color = textColor.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        }
    }
}

@Composable
private fun MessageReactions(
    reactions: List<Reaction>,
    onReactionClick: (String) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        reactions.groupBy { it.emoji }.forEach { (emoji, reactionList) ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                modifier = Modifier.clickable { onReactionClick(emoji) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = emoji,
                        fontSize = 14.sp
                    )
                    if (reactionList.size > 1) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = reactionList.size.toString(),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Date): String {
    val now = Date()
    val diff = now.time - timestamp.time
    
    return when {
        diff < 60000 -> "now" // Less than 1 minute
        diff < 3600000 -> "${diff / 60000}m" // Less than 1 hour
        diff < 86400000 -> "${diff / 3600000}h" // Less than 1 day
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(timestamp)
    }
}

private fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "${size}B"
        size < 1024 * 1024 -> "${size / 1024}KB"
        size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)}MB"
        else -> "${size / (1024 * 1024 * 1024)}GB"
    }
} 