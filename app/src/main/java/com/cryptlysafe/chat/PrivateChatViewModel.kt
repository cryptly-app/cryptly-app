package com.cryptlysafe.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

class PrivateChatViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    
    private val _uiState = MutableStateFlow(PrivateChatState())
    val uiState: StateFlow<PrivateChatState> = _uiState.asStateFlow()

    private var encryptionKey: SecretKey? = null

    init {
        loadUsers()
        loadMessages()
        generateEncryptionKey()
    }

    private fun generateEncryptionKey() {
        // In a real app, you would use a more secure key generation method
        val key = ByteArray(32) // 256 bits
        Random().nextBytes(key)
        encryptionKey = SecretKeySpec(key, "AES")
    }

    fun onMessageChange(message: String) {
        _uiState.value = _uiState.value.copy(currentMessage = message)
    }

    fun selectUser(userId: String) {
        _uiState.value = _uiState.value.copy(selectedUserId = userId)
        loadMessages()
    }

    fun sendMessage() {
        val message = _uiState.value.currentMessage
        if (message.isBlank() || _uiState.value.selectedUserId == null) return

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val encryptedMessage = encryptMessage(message)
                val currentUser = auth.currentUser ?: return@launch
                
                val chatMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    content = encryptedMessage,
                    senderId = currentUser.uid,
                    senderName = currentUser.phoneNumber ?: "Unknown",
                    receiverId = _uiState.value.selectedUserId!!,
                    timestamp = System.currentTimeMillis()
                )

                firestore.collection("messages")
                    .document(chatMessage.id)
                    .set(chatMessage)
                    .await()

                _uiState.value = _uiState.value.copy(
                    currentMessage = "",
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to send message"
                )
            }
        }
    }

    private fun loadUsers() {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser ?: return@launch
                
                val usersSnapshot = firestore.collection("users")
                    .whereNotEqualTo("id", currentUser.uid)
                    .get()
                    .await()

                val users = usersSnapshot.documents.mapNotNull { doc ->
                    doc.toObject(ChatUser::class.java)
                }

                _uiState.value = _uiState.value.copy(users = users)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to load users"
                )
            }
        }
    }

    private fun loadMessages() {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser ?: return@launch
                val selectedUserId = _uiState.value.selectedUserId ?: return@launch

                val messagesQuery = firestore.collection("messages")
                    .whereIn("senderId", listOf(currentUser.uid, selectedUserId))
                    .whereIn("receiverId", listOf(currentUser.uid, selectedUserId))
                    .orderBy("timestamp", Query.Direction.ASCENDING)

                messagesQuery.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        _uiState.value = _uiState.value.copy(
                            error = error.message ?: "Failed to load messages"
                        )
                        return@addSnapshotListener
                    }

                    val messages = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(ChatMessage::class.java)?.let { message ->
                            message.copy(content = decryptMessage(message.content))
                        }
                    } ?: emptyList()

                    _uiState.value = _uiState.value.copy(messages = messages)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to load messages"
                )
            }
        }
    }

    private fun encryptMessage(message: String): String {
        val key = encryptionKey ?: return message
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val iv = ByteArray(16)
        Random().nextBytes(iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))
        val encrypted = cipher.doFinal(message.toByteArray())
        return Base64.encodeToString(iv + encrypted, Base64.DEFAULT)
    }

    private fun decryptMessage(encryptedMessage: String): String {
        val key = encryptionKey ?: return encryptedMessage
        val decoded = Base64.decode(encryptedMessage, Base64.DEFAULT)
        val iv = decoded.sliceArray(0..15)
        val encrypted = decoded.sliceArray(16..decoded.lastIndex)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
        return String(cipher.doFinal(encrypted))
    }
} 