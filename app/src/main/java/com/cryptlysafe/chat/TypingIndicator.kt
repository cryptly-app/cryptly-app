package com.cryptlysafe.cryptly.chat

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun TypingIndicator(
    typingUsers: List<TypingIndicator>,
    modifier: Modifier = Modifier
) {
    if (typingUsers.isEmpty()) return
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Typing bubble
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = 4.dp,
                bottomEnd = 16.dp
            ),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.padding(end = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Animated dots
                TypingDots()
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Typing text
                Text(
                    text = getTypingText(typingUsers),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun TypingDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    
    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 0),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    
    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    
    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TypingDot(alpha = dot1Alpha)
        TypingDot(alpha = dot2Alpha)
        TypingDot(alpha = dot3Alpha)
    }
}

@Composable
private fun TypingDot(alpha: Float) {
    Box(
        modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
            )
    )
}

@Composable
fun TypingIndicatorWithAvatar(
    typingUsers: List<TypingIndicator>,
    modifier: Modifier = Modifier
) {
    if (typingUsers.isEmpty()) return
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = typingUsers.firstOrNull()?.userName?.firstOrNull()?.uppercase() ?: "?",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Typing indicator
        TypingIndicator(typingUsers = typingUsers)
    }
}

@Composable
fun CompactTypingIndicator(
    isTyping: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isTyping) return
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TypingDots()
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = "typing...",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
    }
}

@Composable
fun TypingIndicatorInChatList(
    chatId: String,
    typingUsers: List<TypingIndicator>,
    modifier: Modifier = Modifier
) {
    val isTyping = typingUsers.any { it.chatId == chatId && it.isTyping }
    
    if (!isTyping) return
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TypingDots()
        
        Spacer(modifier = Modifier.width(4.dp))
        
        Text(
            text = "typing...",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            maxLines = 1
        )
    }
}

@Composable
fun AnimatedTypingIndicator(
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    var shouldShow by remember { mutableStateOf(false) }
    
    LaunchedEffect(isVisible) {
        if (isVisible) {
            shouldShow = true
        } else {
            delay(300) // Delay to allow for smooth animation
            shouldShow = false
        }
    }
    
    AnimatedVisibility(
        visible = shouldShow,
        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(300)
        ),
        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(300)
        ),
        modifier = modifier
    ) {
        TypingIndicator(
            typingUsers = listOf(
                TypingIndicator(
                    chatId = "",
                    userId = "",
                    userName = "",
                    isTyping = true
                )
            )
        )
    }
}

@Composable
fun TypingIndicatorWithMultipleUsers(
    typingUsers: List<TypingIndicator>,
    modifier: Modifier = Modifier
) {
    if (typingUsers.isEmpty()) return
    
    Column(modifier = modifier) {
        // Show typing indicators for each user
        typingUsers.forEach { typingUser ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // User avatar
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = typingUser.userName.firstOrNull()?.uppercase() ?: "?",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Typing indicator
                CompactTypingIndicator(isTyping = true)
            }
        }
    }
}

private fun getTypingText(typingUsers: List<TypingIndicator>): String {
    return when {
        typingUsers.size == 1 -> "${typingUsers.first().userName} is typing..."
        typingUsers.size == 2 -> "${typingUsers[0].userName} and ${typingUsers[1].userName} are typing..."
        typingUsers.size == 3 -> "${typingUsers[0].userName}, ${typingUsers[1].userName}, and ${typingUsers[2].userName} are typing..."
        else -> "${typingUsers.size} people are typing..."
    }
}

@Composable
fun TypingIndicatorPreview() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Basic typing indicator
        TypingIndicator(
            typingUsers = listOf(
                TypingIndicator(
                    chatId = "chat1",
                    userId = "user1",
                    userName = "John Doe",
                    isTyping = true
                )
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Typing indicator with avatar
        TypingIndicatorWithAvatar(
            typingUsers = listOf(
                TypingIndicator(
                    chatId = "chat1",
                    userId = "user1",
                    userName = "John Doe",
                    isTyping = true
                )
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Compact typing indicator
        CompactTypingIndicator(isTyping = true)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Multiple users typing
        TypingIndicatorWithMultipleUsers(
            typingUsers = listOf(
                TypingIndicator(
                    chatId = "chat1",
                    userId = "user1",
                    userName = "John Doe",
                    isTyping = true
                ),
                TypingIndicator(
                    chatId = "chat1",
                    userId = "user2",
                    userName = "Jane Smith",
                    isTyping = true
                )
            )
        )
    }
} 