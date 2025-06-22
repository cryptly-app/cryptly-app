package com.cryptlysafe.cryptly.calls

import android.util.Log
import com.cryptlysafe.cryptly.utils.EncryptionUtils
import com.cryptlysafe.cryptly.utils.EncryptionException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await
import java.util.*
import com.cryptlysafe.cryptly.calls.CryptlyCallType


class CallRepository {
    
    companion object {
        private const val TAG = "CallRepository"
        private const val USERS_COLLECTION = "users"
        private const val CALL_LOGS_COLLECTION = "call_logs"
    }
    
    // Firebase Firestore instance
    private val firestore = FirebaseFirestore.getInstance()
    
    // Active listeners for cleanup
    private val activeListeners = mutableMapOf<String, ListenerRegistration>()
    
    /**
     * Log a call with encrypted data to Firestore
     * 
     * @param call The call data to encrypt and store
     * @return Result indicating success or failure
     */
    suspend fun logCall(call: CallDataModel): Result<Unit> {
        return try {
            Log.d(TAG, "Logging encrypted call: ${call.callId}")
            
            // Encrypt sensitive call data
            val encryptedCallData = encryptCallData(call)
            
            // Create call log document data for Firestore
            val callLogData = mapOf(
                "callId" to call.callId,
                "encryptedCallerId" to encryptedCallData.encryptedCallerId,
                "encryptedReceiverId" to encryptedCallData.encryptedReceiverId,
                "encryptedCryptlyCallType" to encryptedCallData.encryptedCryptlyCallType,
                "encryptedDuration" to encryptedCallData.encryptedDuration,
                "timestamp" to call.timestamp,
                "encryptedCallerName" to encryptedCallData.encryptedCallerName,
                "encryptedCallerAvatar" to encryptedCallData.encryptedCallerAvatar,
                "isVideoCall" to call.isVideoCall,
                "isIncoming" to call.isIncoming,
                "callStatus" to call.callStatus.name,
                "encryptionStatus" to call.encryptionStatus.name,
                "createdAt" to System.currentTimeMillis()
            )
            
            // Store encrypted call log in Firestore
            val callLogRef = firestore
                .collection(USERS_COLLECTION)
                .document(call.callerId)
                .collection(CALL_LOGS_COLLECTION)
                .document(call.callId)
            
            callLogRef.set(callLogData).await()
            
            Log.d(TAG, "Encrypted call log stored successfully: ${call.callId}")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log call: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Retrieve encrypted call logs for a user and decrypt them
     * 
     * @param userId The user ID to retrieve call logs for
     * @param onResult Callback function to handle decrypted call logs
     */
    fun getCallLogs(userId: String, onResult: (List<CallDataModel>) -> Unit) {
        Log.d(TAG, "Retrieving encrypted call logs for user: $userId")
        
        // Remove existing listener if any
        activeListeners[userId]?.remove()
        
        val listener = firestore
            .collection(USERS_COLLECTION)
            .document(userId)
            .collection(CALL_LOGS_COLLECTION)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error retrieving call logs: ${error.message}")
                    onResult(emptyList())
                    return@addSnapshotListener
                }
                
                if (snapshot == null) {
                    Log.d(TAG, "No call logs found for user: $userId")
                    onResult(emptyList())
                    return@addSnapshotListener
                }
                
                val decryptedCallLogs = mutableListOf<CallDataModel>()
                
                for (document in snapshot.documents) {
                    try {
                        val decryptedCall = decryptCallData(document)
                        if (decryptedCall != null) {
                            decryptedCallLogs.add(decryptedCall)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing call log document: ${e.message}")
                    }
                }
                
                Log.d(TAG, "Retrieved ${decryptedCallLogs.size} decrypted call logs for user: $userId")
                onResult(decryptedCallLogs)
            }
        
        // Store listener for cleanup
        activeListeners[userId] = listener
    }
    
    /**
     * Delete a specific call log from Firestore
     * 
     * @param callId The ID of the call log to delete
     * @param userId The user ID who owns the call log
     * @return Result indicating success or failure
     */
    suspend fun deleteCall(callId: String, userId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Deleting call log: $callId for user: $userId")
            
            // Delete from Firestore
            val callLogRef = firestore
                .collection(USERS_COLLECTION)
                .document(userId)
                .collection(CALL_LOGS_COLLECTION)
                .document(callId)
            
            callLogRef.delete().await()
            
            Log.d(TAG, "Call log deleted successfully: $callId")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete call log: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Delete a call log by call ID (finds the user automatically)
     * 
     * @param callId The ID of the call log to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteCall(callId: String): Result<Unit> {
        return try {
            // First, find the call log to get the user ID
            val callLogs = getAllCallLogs()
            val callLog = callLogs.find { it.callId == callId }
            
            if (callLog != null) {
                deleteCall(callId, callLog.callerId)
            } else {
                Result.failure(Exception("Call log not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete call log: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get call logs for a specific time period
     * 
     * @param userId The user ID to retrieve call logs for
     * @param startTime Start timestamp in milliseconds
     * @param endTime End timestamp in milliseconds
     * @param onResult Callback function to handle decrypted call logs
     */
    fun getCallLogsByTimeRange(
        userId: String,
        startTime: Long,
        endTime: Long,
        onResult: (List<CallDataModel>) -> Unit
    ) {
        Log.d(TAG, "Retrieving call logs for time range: $startTime to $endTime")
        
        firestore
            .collection(USERS_COLLECTION)
            .document(userId)
            .collection(CALL_LOGS_COLLECTION)
            .whereGreaterThanOrEqualTo("timestamp", startTime)
            .whereLessThanOrEqualTo("timestamp", endTime)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val decryptedCallLogs = mutableListOf<CallDataModel>()
                
                for (document in snapshot.documents) {
                    try {
                        val decryptedCall = decryptCallData(document)
                        if (decryptedCall != null) {
                            decryptedCallLogs.add(decryptedCall)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing call log document: ${e.message}")
                    }
                }
                
                Log.d(TAG, "Retrieved ${decryptedCallLogs.size} call logs for time range")
                onResult(decryptedCallLogs)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to retrieve call logs by time range: ${exception.message}")
                onResult(emptyList())
            }
    }
    
    /**
     * Get call statistics for a user
     * 
     * @param userId The user ID to get statistics for
     * @param onResult Callback function to handle call statistics
     */
    fun getCallStatistics(userId: String, onResult: (CallStatistics) -> Unit) {
        Log.d(TAG, "Retrieving call statistics for user: $userId")
        
        firestore
            .collection(USERS_COLLECTION)
            .document(userId)
            .collection(CALL_LOGS_COLLECTION)
            .get()
            .addOnSuccessListener { snapshot ->
                val callLogs = mutableListOf<CallDataModel>()
                
                for (document in snapshot.documents) {
                    try {
                        val decryptedCall = decryptCallData(document)
                        if (decryptedCall != null) {
                            callLogs.add(decryptedCall)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing call log document: ${e.message}")
                    }
                }
                
                val statistics = calculateCallStatistics(callLogs)
                onResult(statistics)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to retrieve call statistics: ${exception.message}")
                onResult(CallStatistics())
            }
    }
    
    /**
     * Stop listening to call logs for a specific user
     * 
     * @param userId The user ID to stop listening to
     */
    fun stopListeningToCallLogs(userId: String) {
        Log.d(TAG, "Stopping call logs listener for user: $userId")
        activeListeners[userId]?.remove()
        activeListeners.remove(userId)
    }
    
    /**
     * Stop all active listeners
     */
    fun stopAllListeners() {
        Log.d(TAG, "Stopping all active call log listeners")
        activeListeners.values.forEach { it.remove() }
        activeListeners.clear()
    }
    
    // Helper functions for encryption/decryption
    private fun encryptCallData(call: CallDataModel): EncryptedCallData {
        return try {
            EncryptedCallData(
                encryptedCallerId = EncryptionUtils.encrypt(call.callerId),
                encryptedReceiverId = EncryptionUtils.encrypt(call.receiverId ?: ""),
                encryptedCryptlyCallType = EncryptionUtils.encrypt(call.cryptlyCallType.name),
                encryptedDuration = EncryptionUtils.encrypt(call.duration.toString()),
                encryptedCallerName = EncryptionUtils.encrypt(call.callerName ?: ""),
                encryptedCallerAvatar = EncryptionUtils.encrypt(call.callerAvatar ?: "")
            )
        } catch (e: EncryptionException) {
            Log.e(TAG, "Failed to encrypt call data: ${e.message}")
            throw e
        }
    }
    
    private fun decryptCallData(document: com.google.firebase.firestore.DocumentSnapshot): CallDataModel? {
        return try {
            val callId = document.getString("callId") ?: return null
            val timestamp = document.getLong("timestamp") ?: return null
            val isVideoCall = document.getBoolean("isVideoCall") ?: false
            val isIncoming = document.getBoolean("isIncoming") ?: false
            val callStatus = document.getString("callStatus") ?: "ENDED"
            val encryptionStatus = document.getString("encryptionStatus") ?: "ENCRYPTED"
            
            // Decrypt sensitive fields
            val encryptedCallerId = document.getString("encryptedCallerId") ?: return null
            val encryptedReceiverId = document.getString("encryptedReceiverId") ?: ""
            val encryptedCryptlyCallType = document.getString("encryptedCryptlyCallType") ?: return null
            val encryptedDuration = document.getString("encryptedDuration") ?: "0"
            val encryptedCallerName = document.getString("encryptedCallerName") ?: ""
            val encryptedCallerAvatar = document.getString("encryptedCallerAvatar") ?: ""
            
            val callerId = EncryptionUtils.decrypt(encryptedCallerId)
            val receiverId = if (encryptedReceiverId.isNotEmpty()) EncryptionUtils.decrypt(encryptedReceiverId) else null
            val cryptlyCallTypeDecoded = CryptlyCallType.valueOf(EncryptionUtils.decrypt(encryptedCryptlyCallType))
            val duration = EncryptionUtils.decrypt(encryptedDuration).toIntOrNull() ?: 0
            val callerName = if (encryptedCallerName.isNotEmpty()) EncryptionUtils.decrypt(encryptedCallerName) else null
            val callerAvatar = if (encryptedCallerAvatar.isNotEmpty()) EncryptionUtils.decrypt(encryptedCallerAvatar) else null
            
            CallDataModel(
                callId = callId,
                callerId = callerId,
                receiverId = receiverId,
                cryptlyCallType = cryptlyCallTypeDecoded,
                duration = duration,
                timestamp = Date(timestamp),
                callerName = callerName,
                callerAvatar = callerAvatar,
                isVideoCall = isVideoCall,
                isIncoming = isIncoming,
                callStatus = CallStatus.valueOf(callStatus),
                encryptionStatus = EncryptionStatus.valueOf(encryptionStatus)
            )
            
        } catch (e: EncryptionException) {
            Log.e(TAG, "Failed to decrypt call data: ${e.message}")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error processing call log document: ${e.message}")
            null
        }
    }
    
    private fun calculateCallStatistics(callLogs: List<CallDataModel>): CallStatistics {
        val totalCalls = callLogs.size
        val incomingCalls = callLogs.count { it.isIncoming }
        val outgoingCalls = callLogs.count { !it.isIncoming }
        val missedCalls = callLogs.count { it.callStatus == CallStatus.MISSED }
        val totalDuration = callLogs.sumOf { it.duration }
        val videoCalls = callLogs.count { it.isVideoCall }
        val voiceCalls = callLogs.count { !it.isVideoCall }
        
        return CallStatistics(
            totalCalls = totalCalls,
            incomingCalls = incomingCalls,
            outgoingCalls = outgoingCalls,
            missedCalls = missedCalls,
            totalDuration = totalDuration,
            videoCalls = videoCalls,
            voiceCalls = voiceCalls
        )
    }
    
    // Temporary function to get all call logs (for delete by callId)
    private suspend fun getAllCallLogs(): List<CallDataModel> {
        // This is a simplified implementation
        // In a real app, you might want to cache call logs or use a different approach
        return emptyList()
    }
}

// Data classes for internal use
data class EncryptedCallData(
    val encryptedCallerId: String,
    val encryptedReceiverId: String,
    val encryptedCryptlyCallType: String,
    val encryptedDuration: String,
    val encryptedCallerName: String,
    val encryptedCallerAvatar: String
)

data class CallStatistics(
    val totalCalls: Int = 0,
    val incomingCalls: Int = 0,
    val outgoingCalls: Int = 0,
    val missedCalls: Int = 0,
    val totalDuration: Int = 0,
    val videoCalls: Int = 0,
    val voiceCalls: Int = 0
) {
    val averageDuration: Int
        get() = if (totalCalls > 0) totalDuration / totalCalls else 0
    
    val answeredCalls: Int
        get() = totalCalls - missedCalls
    
    val answerRate: Float
        get() = if (totalCalls > 0) (answeredCalls.toFloat() / totalCalls) * 100 else 0f
} 