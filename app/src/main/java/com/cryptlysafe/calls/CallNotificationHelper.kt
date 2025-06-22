package com.cryptlysafe.cryptly.calls

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.cryptlysafe.cryptly.R

class CallNotificationHelper(private val context: Context) {
    
    companion object {
        private const val CHANNEL_ID_CALLS = "cryptly_calls"
        private const val CHANNEL_ID_INCOMING = "cryptly_incoming_calls"
        private const val NOTIFICATION_ID_CALL = 1001
        private const val NOTIFICATION_ID_INCOMING = 1002
        
        private const val ACTION_ACCEPT_CALL = "com.cryptlysafe.cryptly.ACCEPT_CALL"
        private const val ACTION_REJECT_CALL = "com.cryptlysafe.cryptly.REJECT_CALL"
        private const val ACTION_END_CALL = "com.cryptlysafe.cryptly.END_CALL"
    }
    
    init {
        createNotificationChannels()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val callChannel = NotificationChannel(
                CHANNEL_ID_CALLS,
                "Active Calls",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for active calls"
                setSound(null, null)
                enableVibration(false)
            }
            
            val incomingChannel = NotificationChannel(
                CHANNEL_ID_INCOMING,
                "Incoming Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for incoming calls"
                setSound(null, null)
                enableVibration(true)
            }
            
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannels(listOf(callChannel, incomingChannel))
        }
    }
    
    fun showIncomingCallNotification(
        callId: String,
        callerName: String,
        isVideoCall: Boolean
    ) {
        val intent = Intent(context, CallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("call_id", callId)
            putExtra("is_incoming", true)
            putExtra("caller_name", callerName)
            putExtra("is_video_call", isVideoCall)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val acceptIntent = Intent(ACTION_ACCEPT_CALL).apply {
            putExtra("call_id", callId)
        }
        val acceptPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val rejectIntent = Intent(ACTION_REJECT_CALL).apply {
            putExtra("call_id", callId)
        }
        val rejectPendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            rejectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_INCOMING)
            .setContentTitle("Incoming ${if (isVideoCall) "Video" else "Voice"} Call")
            .setContentText(callerName)
            .setSmallIcon(R.drawable.ic_call)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_call_accept,
                "Accept",
                acceptPendingIntent
            )
            .addAction(
                R.drawable.ic_call_reject,
                "Reject",
                rejectPendingIntent
            )
            .build()
        
        NotificationManagerCompat.from(context).notify(
            NOTIFICATION_ID_INCOMING,
            notification
        )
    }
    
    fun showActiveCallNotification(
        callId: String,
        callerName: String,
        duration: String,
        isVideoCall: Boolean
    ) {
        val intent = Intent(context, CallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("call_id", callId)
            putExtra("is_incoming", false)
            putExtra("caller_name", callerName)
            putExtra("is_video_call", isVideoCall)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val endCallIntent = Intent(ACTION_END_CALL).apply {
            putExtra("call_id", callId)
        }
        val endCallPendingIntent = PendingIntent.getBroadcast(
            context,
            3,
            endCallIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_CALLS)
            .setContentTitle("${if (isVideoCall) "Video" else "Voice"} Call")
            .setContentText("$callerName • $duration")
            .setSmallIcon(R.drawable.ic_call_active)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_call_end,
                "End Call",
                endCallPendingIntent
            )
            .build()
        
        NotificationManagerCompat.from(context).notify(
            NOTIFICATION_ID_CALL,
            notification
        )
    }
    
    fun updateCallDuration(duration: String) {
        // Update the active call notification with new duration
        val notificationManager = NotificationManagerCompat.from(context)
        val notification = notificationManager.activeNotifications.find {
            it.id == NOTIFICATION_ID_CALL
        }
        
        notification?.let {
            // Rebuild notification with updated duration
            // This is a simplified version - in practice you'd need to rebuild the notification
        }
    }
    
    fun dismissIncomingCallNotification() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_INCOMING)
    }
    
    fun dismissActiveCallNotification() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_CALL)
    }
    
    fun dismissAllCallNotifications() {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(NOTIFICATION_ID_INCOMING)
        notificationManager.cancel(NOTIFICATION_ID_CALL)
    }
} 