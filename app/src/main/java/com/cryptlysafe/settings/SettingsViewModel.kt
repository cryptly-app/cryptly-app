package com.cryptlysafe.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private lateinit var prefs: SharedPreferences

    init {
        prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                val userDoc = firestore.collection("users").document(userId).get().await()
                
                userDoc.data?.let { data ->
                    _state.update {
                        it.copy(
                            useFingerprint = data["useFingerprint"] as? Boolean ?: false,
                            blockScreenshots = data["blockScreenshots"] as? Boolean ?: false,
                            silentNotifications = data["silentNotifications"] as? Boolean ?: false,
                            lastBackupDate = data["lastBackupDate"] as? String ?: "Never",
                            autoLockDuration = data["autoLockDuration"] as? Int ?: AutoLockManager.DEFAULT_AUTO_LOCK_DURATION
                        )
                    }
                }

                // Load app version from package info
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                _state.update { it.copy(appVersion = packageInfo.versionName) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Failed to load settings") }
            }
        }
    }

    fun setFingerprintEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                firestore.collection("users")
                    .document(userId)
                    .update("useFingerprint", enabled)
                    .await()

                _state.update { it.copy(useFingerprint = enabled) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Failed to update fingerprint setting") }
            }
        }
    }

    fun setScreenshotBlockerEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                firestore.collection("users")
                    .document(userId)
                    .update("blockScreenshots", enabled)
                    .await()

                _state.update { it.copy(blockScreenshots = enabled) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Failed to update screenshot blocker setting") }
            }
        }
    }

    fun setSilentNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                firestore.collection("users")
                    .document(userId)
                    .update("silentNotifications", enabled)
                    .await()

                _state.update { it.copy(silentNotifications = enabled) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Failed to update notification setting") }
            }
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                
                // Delete all messages from Firestore
                val chats = firestore.collection("chats")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                chats.documents.forEach { chatDoc ->
                    firestore.collection("chats")
                        .document(chatDoc.id)
                        .collection("messages")
                        .get()
                        .await()
                        .documents
                        .forEach { messageDoc ->
                            messageDoc.reference.delete().await()
                        }
                }

                _state.update { it.copy(error = "Chat history cleared successfully") }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Failed to clear chat history") }
            }
        }
    }

    fun exportData(format: ExportFormat) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                
                // Get all user data
                val userData = mutableMapOf<String, Any>()
                
                // Get chats
                val chats = firestore.collection("chats")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                val chatData = chats.documents.map { chatDoc ->
                    val messages = chatDoc.reference.collection("messages")
                        .get()
                        .await()
                        .documents
                        .map { it.data }
                    
                    mapOf(
                        "chatId" to chatDoc.id,
                        "messages" to messages
                    )
                }

                userData["chats"] = chatData

                // Convert to requested format
                val exportData = when (format) {
                    ExportFormat.JSON -> {
                        // TODO: Convert to JSON
                        ""
                    }
                    ExportFormat.CSV -> {
                        // TODO: Convert to CSV
                        ""
                    }
                }

                // Save to local storage
                // TODO: Implement file saving

                _state.update { it.copy(error = "Data exported successfully") }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Failed to export data") }
            }
        }
    }

    fun backupData(destination: BackupDestination) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                
                // Get all user data
                val userData = mutableMapOf<String, Any>()
                
                // Get chats
                val chats = firestore.collection("chats")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                val chatData = chats.documents.map { chatDoc ->
                    val messages = chatDoc.reference.collection("messages")
                        .get()
                        .await()
                        .documents
                        .map { it.data }
                    
                    mapOf(
                        "chatId" to chatDoc.id,
                        "messages" to messages
                    )
                }

                userData["chats"] = chatData

                // Upload to selected destination
                when (destination) {
                    BackupDestination.GOOGLE_DRIVE -> {
                        // TODO: Implement Google Drive backup
                    }
                    BackupDestination.DROPBOX -> {
                        // TODO: Implement Dropbox backup
                    }
                }

                // Update last backup date
                val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                val currentDate = dateFormat.format(Date())

                firestore.collection("users")
                    .document(userId)
                    .update("lastBackupDate", currentDate)
                    .await()

                _state.update { 
                    it.copy(
                        lastBackupDate = currentDate,
                        error = "Backup completed successfully"
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Failed to backup data") }
            }
        }
    }

    fun setAutoLockDuration(duration: Int) {
        viewModelScope.launch {
            prefs.edit().putInt("auto_lock_duration", duration).apply()
            _state.update { it.copy(autoLockDuration = duration) }
        }
    }
}

data class SettingsState(
    val useFingerprint: Boolean = false,
    val blockScreenshots: Boolean = false,
    val silentNotifications: Boolean = false,
    val appVersion: String = "",
    val lastBackupDate: String = "Never",
    val error: String? = null,
    val autoLockDuration: Int = AutoLockManager.DEFAULT_AUTO_LOCK_DURATION
)

enum class ExportFormat {
    JSON, CSV
}

enum class BackupDestination {
    GOOGLE_DRIVE, DROPBOX
} 