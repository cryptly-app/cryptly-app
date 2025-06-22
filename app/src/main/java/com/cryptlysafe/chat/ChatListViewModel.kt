package com.cryptlysafe.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptlysafe.utils.EncryptionUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

class ChatListViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ChatListState())
    val uiState: StateFlow<ChatListState> = _uiState.asStateFlow()

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val chatsCollection = firestore.collection("users")
        .document(auth.currentUser?.uid ?: "")
        .collection("chats")

    init {
        loadChats()
        setupChatListener()
    }

    fun createChat(contactId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                // Create chat document
                val chat = Chat(
                    id = UUID.randomUUID().toString(),
                    contactId = contactId,
                    contactName = contactId, // In real app, fetch from contacts
                    lastMessage = "",
                    lastMessageTime = System.currentTimeMillis(),
                    unreadCount = 0
                )

                // Save to Firestore
                chatsCollection.document(chat.id)
                    .set(chat)
                    .await()

                // Update UI state
                _uiState.update { currentState ->
                    currentState.copy(
                        chats = listOf(chat) + currentState.chats,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    error = "Failed to create chat: ${e.message}"
                ) }
            }
        }
    }

    private fun loadChats() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                val snapshot = chatsCollection
                    .orderBy("lastMessageTime", Query.Direction.DESCENDING)
                    .get()
                    .await()

                val chats = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Chat::class.java)
                }

                _uiState.update { it.copy(
                    chats = chats,
                    isLoading = false
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    error = "Failed to load chats: ${e.message}"
                ) }
            }
        }
    }

    private fun setupChatListener() {
        chatsCollection
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _uiState.update { it.copy(
                        error = "Failed to listen for chat updates: ${error.message}"
                    ) }
                    return@addSnapshotListener
                }

                snapshot?.let { docs ->
                    val chats = docs.mapNotNull { doc ->
                        doc.toObject(Chat::class.java)
                    }
                    _uiState.update { it.copy(chats = chats) }
                }
            }
    }
} 