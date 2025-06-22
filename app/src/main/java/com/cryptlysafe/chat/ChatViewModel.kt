package com.cryptlysafe.cryptly.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptlysafe.utils.EncryptionUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.*

class ChatViewModel : ViewModel() {
    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val MESSAGES_PER_PAGE = 20

    private var lastDocument: DocumentSnapshot? = null
    private var isInitialLoad = true

    // Inject ChatRepository
    private val chatRepository = ChatRepository()
    
    // Messages StateFlow for UI
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    
    // Current chat ID
    private var currentChatId: String = ""

    fun loadMessages(chatId: String) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true) }
                
                val messagesRef = firestore.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(MESSAGES_PER_PAGE.toLong())

                val snapshot = messagesRef.get().await()
                lastDocument = snapshot.documents.lastOrNull()
                
                val messages = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ChatMessage::class.java)?.let { message ->
                        if (message.type == MessageType.TEXT) {
                            message.copy(content = decryptMessage(message.content))
                        } else {
                            message
                        }
                    }
                }.reversed()

                _state.update { 
                    it.copy(
                        messages = messages,
                        isLoading = false,
                        hasMoreMessages = messages.size >= MESSAGES_PER_PAGE
                    )
                }

                // Load chat details
                val chatDoc = firestore.collection("chats").document(chatId).get().await()
                chatDoc.data?.let { data ->
                    _state.update {
                        it.copy(
                            contactName = data["contactName"] as? String ?: "",
                            isLocked = data["isLocked"] as? Boolean ?: false
                        )
                    }
                }

                setupMessageListener(chatId)
                setupTypingListener(chatId)
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        error = e.message ?: "Failed to load messages",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun loadMoreMessages() {
        if (lastDocument == null || _state.value.isLoadingMore) return

        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoadingMore = true) }

                val messagesRef = firestore.collection("chats")
                    .document(_state.value.chatId)
                    .collection("messages")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .startAfter(lastDocument!!)
                    .limit(MESSAGES_PER_PAGE.toLong())

                val snapshot = messagesRef.get().await()
                lastDocument = snapshot.documents.lastOrNull()

                val newMessages = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ChatMessage::class.java)?.let { message ->
                        if (message.type == MessageType.TEXT) {
                            message.copy(content = decryptMessage(message.content))
                        } else {
                            message
                        }
                    }
                }.reversed()

                _state.update { 
                    it.copy(
                        messages = it.messages + newMessages,
                        isLoadingMore = false,
                        hasMoreMessages = newMessages.size >= MESSAGES_PER_PAGE
                    )
                }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        error = e.message ?: "Failed to load more messages",
                        isLoadingMore = false
                    )
                }
            }
        }
    }

    fun sendTextMessage(text: String) {
        viewModelScope.launch {
            try {
                val encryptedText = encryptMessage(text)
                val message = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    content = encryptedText,
                    timestamp = System.currentTimeMillis(),
                    type = MessageType.TEXT,
                    isOutgoing = true,
                    status = MessageStatus.SENDING
                )

                // Add to local state immediately
                _state.update { 
                    it.copy(
                        messages = it.messages + message,
                        error = null
                    )
                }

                // Save to Firestore
                val messageRef = firestore.collection("chats")
                    .document(_state.value.chatId)
                    .collection("messages")
                    .document(message.id)

                messageRef.set(message).await()
                
                // Update status to sent
                messageRef.update("status", MessageStatus.SENT.name).await()

                // Notify recipient
                notifyRecipient(_state.value.chatId)
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        error = e.message ?: "Failed to send message",
                        messages = _state.value.messages.map { msg ->
                            if (msg.id == message.id) {
                                msg.copy(status = MessageStatus.FAILED)
                            } else {
                                msg
                            }
                        }
                    )
                }
            }
        }
    }

    fun sendMediaMessage(uri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                val message = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    content = "",
                    timestamp = System.currentTimeMillis(),
                    type = MessageType.IMAGE,
                    isOutgoing = true,
                    status = MessageStatus.SENDING
                )

                // Add to local state immediately
                _state.update { 
                    it.copy(
                        messages = it.messages + message,
                        error = null
                    )
                }

                // Upload to Firebase Storage
                val storageRef = storage.reference
                    .child("chats/${_state.value.chatId}/images/${message.id}")

                val uploadTask = storageRef.putFile(uri).await()
                val downloadUrl = storageRef.downloadUrl.await().toString()

                // Save to Firestore
                val messageRef = firestore.collection("chats")
                    .document(_state.value.chatId)
                    .collection("messages")
                    .document(message.id)

                messageRef.set(message.copy(content = downloadUrl)).await()
                
                // Update status to sent
                messageRef.update("status", MessageStatus.SENT.name).await()

                // Notify recipient
                notifyRecipient(_state.value.chatId)
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        error = e.message ?: "Failed to send media",
                        messages = _state.value.messages.map { msg ->
                            if (msg.id == message.id) {
                                msg.copy(status = MessageStatus.FAILED)
                            } else {
                                msg
                            }
                        }
                    )
                }
            }
        }
    }

    fun deleteMessage(message: ChatMessage) {
        viewModelScope.launch {
            try {
                // Delete from Firestore
                firestore.collection("chats")
                    .document(_state.value.chatId)
                    .collection("messages")
                    .document(message.id)
                    .delete()
                    .await()

                // Update local state
                _state.update {
                    it.copy(
                        messages = it.messages.filter { msg -> msg.id != message.id }
                    )
                }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(error = e.message ?: "Failed to delete message")
                }
            }
        }
    }

    fun copyMessage(message: ChatMessage) {
        viewModelScope.launch {
            try {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Message", message.content)
                clipboard.setPrimaryClip(clip)
            } catch (e: Exception) {
                _state.update { 
                    it.copy(error = e.message ?: "Failed to copy message")
                }
            }
        }
    }

    fun retryMessage(message: ChatMessage) {
        when (message.type) {
            MessageType.TEXT -> sendTextMessage(message.content)
            MessageType.IMAGE -> {
                // TODO: Implement retry for image messages
            }
            MessageType.FILE -> {
                // TODO: Implement retry for file messages
            }
        }
    }

    fun lockChat(pin: String) {
        viewModelScope.launch {
            try {
                firestore.collection("chats")
                    .document(_state.value.chatId)
                    .update("isLocked", true, "pin", pin)
                    .await()

                _state.update { it.copy(isLocked = true) }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(error = e.message ?: "Failed to lock chat")
                }
            }
        }
    }

    fun unlockChat() {
        viewModelScope.launch {
            try {
                firestore.collection("chats")
                    .document(_state.value.chatId)
                    .update("isLocked", false)
                    .await()

                _state.update { it.copy(isLocked = false) }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(error = e.message ?: "Failed to unlock chat")
                }
            }
        }
    }

    fun onTyping() {
        viewModelScope.launch {
            try {
                firestore.collection("chats")
                    .document(_state.value.chatId)
                    .collection("typing")
                    .document(auth.currentUser?.uid ?: return@launch)
                    .set(mapOf(
                        "isTyping" to true,
                        "timestamp" to System.currentTimeMillis()
                    ))
                    .await()

                // Clear typing status after 3 seconds
                kotlinx.coroutines.delay(3000)
                firestore.collection("chats")
                    .document(_state.value.chatId)
                    .collection("typing")
                    .document(auth.currentUser?.uid ?: return@launch)
                    .update("isTyping", false)
                    .await()
            } catch (e: Exception) {
                // Ignore typing errors
            }
        }
    }

    private fun setupMessageListener(chatId: String) {
        firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _state.update { 
                        it.copy(error = error.message ?: "Failed to listen for messages")
                    }
                    return@addSnapshotListener
                }

                snapshot?.documents?.firstOrNull()?.let { doc ->
                    val message = doc.toObject(ChatMessage::class.java)
                    if (message != null && !_state.value.messages.any { it.id == message.id }) {
                        viewModelScope.launch {
                            if (message.type == MessageType.TEXT) {
                                message.copy(content = decryptMessage(message.content))
                            } else {
                                message
                            }.let { decryptedMessage ->
                                _state.update {
                                    it.copy(messages = it.messages + decryptedMessage)
                                }
                            }
                        }
                    }
                }
            }
    }

    private fun setupTypingListener(chatId: String) {
        firestore.collection("chats")
            .document(chatId)
            .collection("typing")
            .whereNotEqualTo("userId", auth.currentUser?.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                val isTyping = snapshot?.documents?.any { doc ->
                    doc.getBoolean("isTyping") == true &&
                    System.currentTimeMillis() - (doc.getLong("timestamp") ?: 0) < 3000
                } ?: false

                _state.update { it.copy(isTyping = isTyping) }
            }
    }

    private fun notifyRecipient(chatId: String) {
        // TODO: Implement push notification
    }

    private fun encryptMessage(text: String): String {
        // TODO: Implement message encryption
        return text
    }

    private fun decryptMessage(text: String): String {
        // TODO: Implement message decryption
        return text
    }

    /**
     * Send a message to a specific chat
     * 
     * @param chatId The chat ID to send the message to
     * @param message The plain text message to send
     */
    fun sendMessage(chatId: String, message: String) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true, error = null) }
                
                // Use ChatRepository to send encrypted message
                val result = chatRepository.sendMessage(chatId, message, getCurrentUserId())
                
                result.onSuccess { chatMessage ->
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            error = null
                        )
                    }
                    // Message is automatically added to local state via repository
                }.onFailure { exception ->
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to send message"
                        )
                    }
                }
                
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to send message"
                    )
                }
            }
        }
    }
    
    /**
     * Observe messages for a specific chat in real-time
     * 
     * @param chatId The chat ID to observe messages for
     */
    fun observeMessages(chatId: String) {
        currentChatId = chatId
        
        // Use ChatRepository to listen to encrypted messages
        chatRepository.listenToMessages(chatId) { messages ->
            _messages.value = messages
            _state.update { 
                it.copy(
                    chatId = chatId,
                    isLoading = false,
                    error = null
                )
            }
        }
    }
    
    /**
     * Load messages for a specific chat (non-real-time)
     * 
     * @param chatId The chat ID to load messages for
     * @param limit Maximum number of messages to load
     */
    fun loadMessages(chatId: String, limit: Int = 50) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true, error = null) }
                
                val result = chatRepository.loadMessages(chatId, limit)
                
                result.onSuccess { messages ->
                    _messages.value = messages
                    _state.update { 
                        it.copy(
                            chatId = chatId,
                            isLoading = false,
                            error = null
                        )
                    }
                }.onFailure { exception ->
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to load messages"
                        )
                    }
                }
                
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load messages"
                    )
                }
            }
        }
    }
    
    /**
     * Delete a specific message
     * 
     * @param messageId The ID of the message to delete
     */
    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true, error = null) }
                
                val result = chatRepository.deleteMessage(messageId)
                
                result.onSuccess {
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            error = null
                        )
                    }
                    // Message is automatically removed from local state via repository
                }.onFailure { exception ->
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to delete message"
                        )
                    }
                }
                
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to delete message"
                    )
                }
            }
        }
    }
    
    /**
     * Mark a message as read
     * 
     * @param messageId The ID of the message to mark as read
     */
    fun markMessageAsRead(messageId: String) {
        viewModelScope.launch {
            try {
                val result = chatRepository.markMessageAsRead(messageId)
                
                result.onFailure { exception ->
                    _state.update { 
                        it.copy(
                            error = exception.message ?: "Failed to mark message as read"
                        )
                    }
                }
                
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        error = e.message ?: "Failed to mark message as read"
                    )
                }
            }
        }
    }
    
    /**
     * Search messages
     * 
     * @param query The search query
     * @param filter Optional message filter
     */
    fun searchMessages(query: String, filter: MessageFilter? = null) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true, error = null) }
                
                val result = chatRepository.searchMessages(query, filter)
                
                result.onSuccess { searchResults ->
                    val searchMessages = searchResults.map { it.message }
                    _messages.value = searchMessages
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            error = null
                        )
                    }
                }.onFailure { exception ->
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to search messages"
                        )
                    }
                }
                
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to search messages"
                    )
                }
            }
        }
    }
    
    /**
     * Set typing indicator
     * 
     * @param chatId The chat ID to set typing indicator for
     * @param isTyping Whether the user is typing
     */
    fun setTypingIndicator(chatId: String, isTyping: Boolean) {
        viewModelScope.launch {
            try {
                chatRepository.setTypingIndicator(chatId, isTyping)
            } catch (e: Exception) {
                // Typing indicator errors are not critical, just log them
                println("Failed to set typing indicator: ${e.message}")
            }
        }
    }
    
    /**
     * Load chat rooms
     */
    fun loadChatRooms() {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true, error = null) }
                
                val result = chatRepository.loadChatRooms()
                
                result.onSuccess { chatRooms ->
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            error = null
                        )
                    }
                }.onFailure { exception ->
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to load chat rooms"
                        )
                    }
                }
                
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load chat rooms"
                    )
                }
            }
        }
    }
    
    /**
     * Clear error state
     */
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
    
    /**
     * Stop listening to messages for current chat
     */
    fun stopObservingMessages() {
        if (currentChatId.isNotEmpty()) {
            chatRepository.stopListeningToMessages(currentChatId)
        }
    }
    
    /**
     * Clean up resources when ViewModel is destroyed
     */
    override fun onCleared() {
        super.onCleared()
        chatRepository.stopAllListeners()
    }
    
    // Helper functions
    private fun getCurrentUserId(): String {
        // TODO: Get from Firebase Auth or user session
        return "currentUser"
    }
}

data class ChatState(
    val chatId: String = "",
    val contactName: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMoreMessages: Boolean = false,
    val isTyping: Boolean = false,
    val isLocked: Boolean = false,
    val error: String? = null
)

data class ChatMessage(
    val id: String = "",
    val content: String = "",
    val timestamp: Long = 0,
    val type: MessageType = MessageType.TEXT,
    val isOutgoing: Boolean = false,
    val status: MessageStatus = MessageStatus.SENT
)

enum class MessageType {
    TEXT, IMAGE, FILE
}

enum class MessageStatus {
    SENDING, SENT, DELIVERED, READ, FAILED
} 