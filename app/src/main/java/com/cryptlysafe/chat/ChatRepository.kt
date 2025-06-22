package com.cryptlysafe.cryptly.chat

import android.util.Log
import com.cryptlysafe.cryptly.utils.EncryptionUtils
import com.cryptlysafe.cryptly.utils.EncryptionException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.*

class ChatRepository {
    
    companion object {
        private const val TAG = "ChatRepository"
        private const val CHATS_COLLECTION = "chats"
        private const val MESSAGES_COLLECTION = "messages"
        private const val TYPING_COLLECTION = "typing"
        private const val CHAT_ROOMS_COLLECTION = "chatRooms"
    }
    
    // Firebase Firestore instance
    private val firestore = FirebaseFirestore.getInstance()
    
    // Active listeners for cleanup
    private val activeListeners = mutableMapOf<String, ListenerRegistration>()
    
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    
    private val _chatRooms = MutableStateFlow<List<ChatRoom>>(emptyList())
    val chatRooms: StateFlow<List<ChatRoom>> = _chatRooms.asStateFlow()
    
    private val _typingIndicators = MutableStateFlow<List<TypingIndicator>>(emptyList())
    val typingIndicators: StateFlow<List<TypingIndicator>> = _typingIndicators.asStateFlow()
    
    init {
        loadMockData()
    }
    
    /**
     * Send an encrypted message to a chat
     * 
     * @param chatId The chat ID to send the message to
     * @param message The plain text message to encrypt and send
     * @param senderId The ID of the message sender
     * @return Result containing the created ChatMessage or error
     */
    suspend fun sendMessage(chatId: String, message: String, senderId: String): Result<ChatMessage> {
        return try {
            Log.d(TAG, "Sending encrypted message to chat: $chatId")
            
            // Generate unique message ID
            val messageId = generateMessageId()
            val timestamp = System.currentTimeMillis()
            
            // Encrypt the message using AES-256
            val encryptedText = try {
                EncryptionUtils.encrypt(message)
            } catch (e: EncryptionException) {
                Log.e(TAG, "Failed to encrypt message: ${e.message}")
                return Result.failure(e)
            }
            
            Log.d(TAG, "Message encrypted successfully for chat: $chatId")
            
            // Create message data for Firestore
            val messageData = mapOf(
                "messageId" to messageId,
                "chatId" to chatId,
                "encryptedText" to encryptedText,
                "senderId" to senderId,
                "timestamp" to timestamp,
                "messageType" to "TEXT",
                "status" to "SENDING"
            )
            
            // Store encrypted message in Firestore
            val messageRef = firestore
                .collection(CHATS_COLLECTION)
                .document(chatId)
                .collection(MESSAGES_COLLECTION)
                .document(messageId)
            
            messageRef.set(messageData).await()
            
            Log.d(TAG, "Encrypted message stored in Firestore: $messageId")
            
            // Create ChatMessage object for local state
            val chatMessage = ChatMessage(
                id = messageId,
                chatId = chatId,
                senderId = senderId,
                senderName = getCurrentUserName(),
                content = message, // Store decrypted content locally for UI
                messageType = MessageType.TEXT,
                timestamp = Date(timestamp),
                status = MessageStatus.SENT,
                isEncrypted = true
            )
            
            // Update local state
            val currentMessages = _messages.value.toMutableList()
            currentMessages.add(chatMessage)
            _messages.value = currentMessages
            
            // Update chat room's last message
            updateChatRoomLastMessage(chatId, chatMessage)
            
            Result.success(chatMessage)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Listen to encrypted messages in real-time for a specific chat
     * 
     * @param chatId The chat ID to listen to
     * @param onMessagesReceived Callback function to handle decrypted messages
     */
    fun listenToMessages(chatId: String, onMessagesReceived: (List<ChatMessage>) -> Unit) {
        Log.d(TAG, "Starting real-time listener for chat: $chatId")
        
        // Remove existing listener if any
        activeListeners[chatId]?.remove()
        
        val listener = firestore
            .collection(CHATS_COLLECTION)
            .document(chatId)
            .collection(MESSAGES_COLLECTION)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to messages: ${error.message}")
                    return@addSnapshotListener
                }
                
                if (snapshot == null) {
                    Log.d(TAG, "No messages found for chat: $chatId")
                    onMessagesReceived(emptyList())
                    return@addSnapshotListener
                }
                
                val decryptedMessages = mutableListOf<ChatMessage>()
                
                for (document in snapshot.documents) {
                    try {
                        val messageId = document.getString("messageId") ?: continue
                        val encryptedText = document.getString("encryptedText") ?: continue
                        val senderId = document.getString("senderId") ?: continue
                        val timestamp = document.getLong("timestamp") ?: continue
                        val messageType = document.getString("messageType") ?: "TEXT"
                        val status = document.getString("status") ?: "SENT"
                        
                        // Decrypt the message
                        val decryptedText = try {
                            EncryptionUtils.decrypt(encryptedText)
                        } catch (e: EncryptionException) {
                            Log.e(TAG, "Failed to decrypt message $messageId: ${e.message}")
                            // Create a placeholder message for failed decryption
                            ChatMessage(
                                id = messageId,
                                chatId = chatId,
                                senderId = senderId,
                                senderName = "Unknown",
                                content = "[Encrypted Message]",
                                messageType = MessageType.TEXT,
                                timestamp = Date(timestamp),
                                status = MessageStatus.FAILED,
                                isEncrypted = true
                            ).also { decryptedMessages.add(it) }
                            continue
                        }
                        
                        Log.d(TAG, "Message decrypted successfully: $messageId")
                        
                        // Create ChatMessage object
                        val chatMessage = ChatMessage(
                            id = messageId,
                            chatId = chatId,
                            senderId = senderId,
                            senderName = getSenderName(senderId),
                            content = decryptedText,
                            messageType = MessageType.valueOf(messageType),
                            timestamp = Date(timestamp),
                            status = MessageStatus.valueOf(status),
                            isEncrypted = true
                        )
                        
                        decryptedMessages.add(chatMessage)
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing message document: ${e.message}")
                    }
                }
                
                Log.d(TAG, "Received ${decryptedMessages.size} decrypted messages for chat: $chatId")
                onMessagesReceived(decryptedMessages)
            }
        
        // Store listener for cleanup
        activeListeners[chatId] = listener
    }
    
    /**
     * Delete a specific message from Firestore
     * 
     * @param messageId The ID of the message to delete
     * @param chatId The chat ID containing the message
     * @return Result indicating success or failure
     */
    suspend fun deleteMessage(messageId: String, chatId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Deleting message: $messageId from chat: $chatId")
            
            // Delete from Firestore
            val messageRef = firestore
                .collection(CHATS_COLLECTION)
                .document(chatId)
                .collection(MESSAGES_COLLECTION)
                .document(messageId)
            
            messageRef.delete().await()
            
            Log.d(TAG, "Message deleted successfully from Firestore: $messageId")
            
            // Update local state
            val updatedMessages = _messages.value.filter { it.id != messageId }
            _messages.value = updatedMessages
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete message: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Load messages for a specific chat (non-real-time)
     * 
     * @param chatId The chat ID to load messages for
     * @param limit Maximum number of messages to load
     * @return Result containing list of decrypted messages
     */
    suspend fun loadMessages(chatId: String, limit: Int = 50): Result<List<ChatMessage>> {
        return try {
            Log.d(TAG, "Loading messages for chat: $chatId, limit: $limit")
            
            val messagesRef = firestore
                .collection(CHATS_COLLECTION)
                .document(chatId)
                .collection(MESSAGES_COLLECTION)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())
            
            val snapshot = messagesRef.get().await()
            val decryptedMessages = mutableListOf<ChatMessage>()
            
            for (document in snapshot.documents) {
                try {
                    val messageId = document.getString("messageId") ?: continue
                    val encryptedText = document.getString("encryptedText") ?: continue
                    val senderId = document.getString("senderId") ?: continue
                    val timestamp = document.getLong("timestamp") ?: continue
                    val messageType = document.getString("messageType") ?: "TEXT"
                    val status = document.getString("status") ?: "SENT"
                    
                    // Decrypt the message
                    val decryptedText = try {
                        EncryptionUtils.decrypt(encryptedText)
                    } catch (e: EncryptionException) {
                        Log.e(TAG, "Failed to decrypt message $messageId: ${e.message}")
                        continue
                    }
                    
                    val chatMessage = ChatMessage(
                        id = messageId,
                        chatId = chatId,
                        senderId = senderId,
                        senderName = getSenderName(senderId),
                        content = decryptedText,
                        messageType = MessageType.valueOf(messageType),
                        timestamp = Date(timestamp),
                        status = MessageStatus.valueOf(status),
                        isEncrypted = true
                    )
                    
                    decryptedMessages.add(chatMessage)
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing message document: ${e.message}")
                }
            }
            
            Log.d(TAG, "Loaded ${decryptedMessages.size} messages for chat: $chatId")
            Result.success(decryptedMessages)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load messages: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Stop listening to messages for a specific chat
     * 
     * @param chatId The chat ID to stop listening to
     */
    fun stopListeningToMessages(chatId: String) {
        Log.d(TAG, "Stopping listener for chat: $chatId")
        activeListeners[chatId]?.remove()
        activeListeners.remove(chatId)
    }
    
    /**
     * Stop all active listeners
     */
    fun stopAllListeners() {
        Log.d(TAG, "Stopping all active listeners")
        activeListeners.values.forEach { it.remove() }
        activeListeners.clear()
    }
    
    // Helper functions
    private fun generateMessageId(): String {
        return "msg_${System.currentTimeMillis()}_${UUID.randomUUID().toString().substring(0, 8)}"
    }
    
    private fun getCurrentUserId(): String {
        // TODO: Get from Firebase Auth
        return "currentUser"
    }
    
    private fun getCurrentUserName(): String {
        // TODO: Get from Firebase Auth or user profile
        return "You"
    }
    
    private fun getSenderName(senderId: String): String {
        return if (senderId == getCurrentUserId()) {
            getCurrentUserName()
        } else {
            // TODO: Get from user cache or Firestore
            "User $senderId"
        }
    }
    
    private fun updateChatRoomLastMessage(chatId: String, message: ChatMessage) {
        val updatedChatRooms = _chatRooms.value.map { chatRoom ->
            if (chatRoom.id == chatId) {
                chatRoom.copy(
                    lastMessage = message,
                    updatedAt = Date(),
                    unreadCount = if (message.senderId == getCurrentUserId()) 0 else chatRoom.unreadCount + 1
                )
            } else chatRoom
        }
        _chatRooms.value = updatedChatRooms
    }
    
    // Legacy functions for backward compatibility
    suspend fun sendMessage(chatId: String, content: String, messageType: MessageType = MessageType.TEXT): Result<ChatMessage> {
        return sendMessage(chatId, content, getCurrentUserId())
    }
    
    suspend fun deleteMessage(messageId: String): Result<Unit> {
        // Find the chat ID for this message
        val message = _messages.value.find { it.id == messageId }
        return if (message != null) {
            deleteMessage(messageId, message.chatId)
        } else {
            Result.failure(Exception("Message not found"))
        }
    }
    
    // Mock data loading (kept for backward compatibility)
    private fun loadMockData() {
        // Load mock chat rooms
        val mockChatRooms = listOf(
            ChatRoom(
                id = "chat1",
                name = "John Doe",
                participants = listOf(
                    ChatParticipant("user1", "John Doe", isOnline = true),
                    ChatParticipant("currentUser", "You")
                ),
                lastMessage = ChatMessage(
                    id = "msg1",
                    chatId = "chat1",
                    senderId = "user1",
                    senderName = "John Doe",
                    content = "Hey, how are you?",
                    timestamp = Date(System.currentTimeMillis() - 300000)
                ),
                unreadCount = 2
            ),
            ChatRoom(
                id = "chat2",
                name = "Jane Smith",
                participants = listOf(
                    ChatParticipant("user2", "Jane Smith", isOnline = false),
                    ChatParticipant("currentUser", "You")
                ),
                lastMessage = ChatMessage(
                    id = "msg2",
                    chatId = "chat2",
                    senderId = "currentUser",
                    senderName = "You",
                    content = "Thanks for the info!",
                    timestamp = Date(System.currentTimeMillis() - 600000)
                ),
                unreadCount = 0
            )
        )
        _chatRooms.value = mockChatRooms
    }
    
    // Other existing functions (simplified for brevity)
    suspend fun loadChatRooms(): Result<List<ChatRoom>> {
        return try {
            Result.success(_chatRooms.value)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun markMessageAsRead(messageId: String): Result<Unit> {
        return try {
            val updatedMessages = _messages.value.map { message ->
                if (message.id == messageId) {
                    message.copy(isRead = true, readBy = message.readBy + getCurrentUserId())
                } else message
            }
            _messages.value = updatedMessages
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun searchMessages(query: String, filter: MessageFilter? = null): Result<List<MessageSearchResult>> {
        return try {
            val results = _messages.value
                .filter { it.content.contains(query, ignoreCase = true) }
                .map { message ->
                    val chatRoom = _chatRooms.value.find { it.id == message.chatId }
                        ?: ChatRoom(id = message.chatId)
                    MessageSearchResult(
                        message = message,
                        chatRoom = chatRoom,
                        matchedText = message.content,
                        relevanceScore = 1.0f
                    )
                }
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun setTypingIndicator(chatId: String, isTyping: Boolean): Result<Unit> {
        return try {
            val indicator = TypingIndicator(
                chatId = chatId,
                userId = getCurrentUserId(),
                userName = getCurrentUserName(),
                isTyping = isTyping
            )
            
            val currentIndicators = _typingIndicators.value.toMutableList()
            val existingIndex = currentIndicators.indexOfFirst { it.userId == getCurrentUserId() && it.chatId == chatId }
            
            if (existingIndex >= 0) {
                if (isTyping) {
                    currentIndicators[existingIndex] = indicator
                } else {
                    currentIndicators.removeAt(existingIndex)
                }
            } else if (isTyping) {
                currentIndicators.add(indicator)
            }
            
            _typingIndicators.value = currentIndicators
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Real-time listeners for Firebase (simplified)
    fun listenToMessages(chatId: String): Flow<List<ChatMessage>> {
        return messages
    }
    
    fun listenToChatRooms(): Flow<List<ChatRoom>> {
        return chatRooms
    }
    
    fun listenToTypingIndicators(chatId: String): Flow<List<TypingIndicator>> {
        return typingIndicators
    }
} 