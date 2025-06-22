package com.cryptlysafe.chat

data class PrivateChatState(
    val messages: List<ChatMessage> = emptyList(),
    val users: List<ChatUser> = emptyList(),
    val currentMessage: String = "",
    val selectedUserId: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class ChatMessage(
    val id: String = "",
    val content: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val receiverId: String = "",
    val timestamp: Long = 0
)

data class ChatUser(
    val id: String = "",
    val name: String = "",
    val phoneNumber: String = ""
) 