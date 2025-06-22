package com.cryptlysafe.chat

data class ChatState(
    val contactName: String = "",
    val isOnline: Boolean = false,
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class Message(
    val id: String = "",
    val content: String = "",
    val type: MessageType = MessageType.TEXT,
    val timestamp: Long = 0L,
    val isOutgoing: Boolean = false,
    val isRead: Boolean = false
)

enum class MessageType {
    TEXT,
    IMAGE,
    FILE
} 