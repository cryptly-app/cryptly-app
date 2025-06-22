package com.cryptlysafe.cryptly.chat

import java.util.*

data class ChatMessage(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderAvatar: String? = null,
    val content: String = "",
    val messageType: MessageType = MessageType.TEXT,
    val timestamp: Date = Date(),
    val isEncrypted: Boolean = true,
    val encryptionLevel: EncryptionLevel = EncryptionLevel.STANDARD,
    val status: MessageStatus = MessageStatus.SENDING,
    val isRead: Boolean = false,
    val readBy: List<String> = emptyList(),
    val replyTo: String? = null,
    val forwardedFrom: String? = null,
    val mediaUrl: String? = null,
    val mediaThumbnail: String? = null,
    val mediaSize: Long = 0,
    val mediaDuration: Int? = null,
    val mediaType: String? = null,
    val location: LocationData? = null,
    val contact: ContactData? = null,
    val reactions: List<Reaction> = emptyList(),
    val editedAt: Date? = null,
    val isEdited: Boolean = false,
    val deletedAt: Date? = null,
    val isDeleted: Boolean = false
)

enum class MessageType {
    TEXT,
    IMAGE,
    VIDEO,
    AUDIO,
    DOCUMENT,
    LOCATION,
    CONTACT,
    STICKER,
    GIF,
    VOICE_MESSAGE,
    SYSTEM_MESSAGE
}

enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED,
    CANCELLED
}

enum class EncryptionLevel {
    BASIC,
    STANDARD,
    HIGH,
    MILITARY_GRADE
}

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val address: String? = null,
    val placeName: String? = null
)

data class ContactData(
    val name: String,
    val phoneNumber: String,
    val email: String? = null,
    val avatar: String? = null
)

data class Reaction(
    val userId: String,
    val userName: String,
    val emoji: String,
    val timestamp: Date = Date()
)

data class ChatRoom(
    val id: String = "",
    val name: String = "",
    val description: String? = null,
    val avatar: String? = null,
    val participants: List<ChatParticipant> = emptyList(),
    val lastMessage: ChatMessage? = null,
    val unreadCount: Int = 0,
    val isGroup: Boolean = false,
    val isEncrypted: Boolean = true,
    val encryptionLevel: EncryptionLevel = EncryptionLevel.STANDARD,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val createdBy: String = "",
    val isActive: Boolean = true,
    val isArchived: Boolean = false,
    val isMuted: Boolean = false,
    val wallpaper: String? = null,
    val theme: ChatTheme = ChatTheme.DEFAULT
)

data class ChatParticipant(
    val userId: String,
    val name: String,
    val avatar: String? = null,
    val isOnline: Boolean = false,
    val lastSeen: Date? = null,
    val role: ParticipantRole = ParticipantRole.MEMBER,
    val joinedAt: Date = Date(),
    val isAdmin: Boolean = false,
    val isMuted: Boolean = false
)

enum class ParticipantRole {
    OWNER,
    ADMIN,
    MEMBER,
    GUEST
}

enum class ChatTheme {
    DEFAULT,
    DARK,
    LIGHT,
    CUSTOM
}

data class TypingIndicator(
    val chatId: String,
    val userId: String,
    val userName: String,
    val isTyping: Boolean,
    val timestamp: Date = Date()
)

data class MessageSearchResult(
    val message: ChatMessage,
    val chatRoom: ChatRoom,
    val matchedText: String,
    val relevanceScore: Float
)

data class MessageFilter(
    val messageTypes: List<MessageType> = emptyList(),
    val dateRange: DateRange? = null,
    val senderId: String? = null,
    val hasMedia: Boolean? = null,
    val isEncrypted: Boolean? = null
)

data class DateRange(
    val startDate: Date,
    val endDate: Date
) 