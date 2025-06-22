package com.cryptlysafe.chat

data class ChatListState(
    val chats: List<Chat> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class Chat(
    val id: String = "",
    val contactId: String = "",
    val contactName: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Long = 0L,
    val unreadCount: Int = 0
) 