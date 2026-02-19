package com.yourgpt.sdk

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class YourGPTNotificationService : FirebaseMessagingService() {
    
    companion object {
        const val CHANNEL_ID = "yourgpt_messages"
        const val CHANNEL_NAME = "YourGPT Messages"
        const val CHANNEL_DESCRIPTION = "Notifications for new messages from YourGPT widget"
        const val NOTIFICATION_GROUP = "com.yourgpt.sdk.MESSAGES"
        private const val TAG = "YourGPTNotificationService"
        
        // Notification extras
        const val EXTRA_WIDGET_UID = "widget_uid"
        const val EXTRA_MESSAGE_ID = "message_id"
        const val EXTRA_CONVERSATION_ID = "conversation_id"
        const val EXTRA_SENDER_NAME = "sender_name"
        const val EXTRA_MESSAGE_CONTENT = "message_content"
        const val EXTRA_TIMESTAMP = "timestamp"
        
        private var tokenCallback: ((String) -> Unit)? = null
        private var messageCallback: ((Map<String, Any>) -> Unit)? = null
        
        fun setTokenCallback(callback: (String) -> Unit) {
            tokenCallback = callback
        }
        
        fun setMessageCallback(callback: (Map<String, Any>) -> Unit) {
            messageCallback = callback
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        // Use Helper to create notification channel
        YourGPTNotificationHelper.createNotificationChannel(this)
    }
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        
        // Log token for debugging
        android.util.Log.d(TAG, "New FCM token: $token")
        
        // Store token locally
        storeToken(token)
        
        // Notify SDK about new token
        tokenCallback?.invoke(token)
        
        // Send token to YourGPT backend
        CoroutineScope(Dispatchers.IO).launch {
            // If using NotificationClient, let it handle token registration
            if (YourGPTNotificationClient.isInitialized()) {
                YourGPTNotificationClient.sendTokenToYourGPT(token)
            } else {
                sendTokenToBackend(token)
            }
        }
    }
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        android.util.Log.d(TAG, "Message received from: ${remoteMessage.from}")
        android.util.Log.d(TAG, "Message data: ${remoteMessage.data}")
        android.util.Log.d(TAG, "Message notification: ${remoteMessage.notification}")
        
        // First, try to handle with YourGPTNotificationClient (minimalist mode)
        if (YourGPTNotificationClient.isInitialized()) {
            val handled = YourGPTNotificationClient.handleNotification(this, remoteMessage)
            if (handled) {
                android.util.Log.d(TAG, "Notification handled by YourGPTNotificationClient")
                return
            }
        }
        
        // If not handled by client, proceed with normal flow
        val data = remoteMessage.data
        val notification = remoteMessage.notification
        
        // Parse YourGPT specific data - check both formats
        val widgetUid = data["widget_uid"] ?: data["project_uid"]
        val messageType = data["type"] ?: "message"
        
        // Check if this is a YourGPT widget message (both old and new formats)
        if (widgetUid != null && (messageType == "widget_message" || messageType == "conversation")) {
            handleWidgetMessage(data, notification)
        } else if (notification != null) {
            // Handle standard FCM notification with notification payload
            showStandardNotification(notification)
        }
        
        // Notify callback listeners for advanced mode
        messageCallback?.invoke(data)
    }
    
    private fun handleWidgetMessage(data: Map<String, String>, notification: RemoteMessage.Notification?) {
        // For advanced mode handling - minimalist mode is handled by NotificationClient
        // Check if notifications are enabled
        if (!YourGPTNotificationHelper.areNotificationsEnabled(this)) {
            return
        }
        
        // Extract message details based on notification format
        val messageId: String
        val conversationId: String?
        val senderName: String
        val messageContent: String
        val widgetUid: String
        val timestamp: Long
        
        // Check if it's the new format with project_uid
        if (data.containsKey("project_uid") && data["type"] == "conversation") {
            // New format
            messageId = data["messageId"] ?: System.currentTimeMillis().toString()
            conversationId = data["session_uid"] // Use session_uid as conversation ID
            senderName = notification?.title ?: data["sender_name"] ?: "YourGPT Assistant"
            messageContent = notification?.body ?: data["message_content"] ?: "New message"
            widgetUid = data["project_uid"] ?: return
            
            // Try to parse nested data if available
            val nestedData = data["data"]
            timestamp = if (nestedData != null) {
                try {
                    // Parse timestamp from nested JSON if present
                    System.currentTimeMillis()
                } catch (e: Exception) {
                    System.currentTimeMillis()
                }
            } else {
                System.currentTimeMillis()
            }
        } else {
            // Old format fallback
            messageId = data["message_id"] ?: System.currentTimeMillis().toString()
            conversationId = data["conversation_id"]
            senderName = data["sender_name"] ?: notification?.title ?: "YourGPT Assistant"
            messageContent = data["message_content"] ?: notification?.body ?: "New message"
            widgetUid = data["widget_uid"] ?: return
            timestamp = data["timestamp"]?.toLongOrNull() ?: System.currentTimeMillis()
        }
        
        // Get default config for notifications
        val config = YourGPTNotificationConfig()
        
        // Create notification
        showNotification(
            messageId = messageId,
            conversationId = conversationId,
            senderName = senderName,
            messageContent = messageContent,
            widgetUid = widgetUid,
            timestamp = timestamp,
            config = config
        )
    }
    
    private fun showNotification(
        messageId: String,
        conversationId: String?,
        senderName: String,
        messageContent: String,
        widgetUid: String,
        timestamp: Long,
        config: YourGPTNotificationConfig
    ) {
        // Check if notifications are permitted
        if (!YourGPTNotificationHelper.areNotificationsEnabled(this)) {
            return
        }
        
        // Create intent for notification click
        val clickIntent = YourGPTNotificationHelper.createWidgetDeepLink(
            context = this,
            widgetUid = widgetUid,
            conversationId = conversationId
        )
        
        val pendingIntent = YourGPTNotificationHelper.createClickPendingIntent(
            context = this,
            intent = clickIntent,
            requestCode = messageId.hashCode()
        )
        
        // Build notification using Helper
        val notificationBuilder = YourGPTNotificationHelper.createRichNotification(
            context = this,
            title = senderName,
            message = messageContent,
            bigText = messageContent,
            clickIntent = pendingIntent
        ).apply {
            setGroup(NOTIFICATION_GROUP)
            setWhen(timestamp)
            setShowWhen(true)
            
            // Apply config settings
            if (config.soundEnabled) {
                val soundUri = config.soundUri ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                setSound(soundUri)
            }
            
            if (config.vibrationEnabled) {
                setVibrate(config.vibrationPattern)
            }
            
            if (config.ledEnabled) {
                setLights(config.ledColor, config.ledOnMs, config.ledOffMs)
            }
            
            config.largeIcon?.let { setLargeIcon(it) }
        }
        
        // Show notification using Helper
        YourGPTNotificationHelper.showNotification(
            context = this,
            notificationId = messageId.hashCode(),
            builder = notificationBuilder
        )
        
        // Create summary notification for grouped messages
        if (config.groupMessages) {
            showGroupSummaryNotification()
        }
    }
    
    private fun showGroupSummaryNotification() {
        val summaryBuilder = YourGPTNotificationHelper.createGroupSummary(
            context = this,
            groupKey = NOTIFICATION_GROUP,
            summaryText = "You have new messages"
        )
        
        YourGPTNotificationHelper.showNotification(
            context = this,
            notificationId = 0,
            builder = summaryBuilder
        )
    }
    
    private fun showStandardNotification(notification: RemoteMessage.Notification) {
        val notificationBuilder = YourGPTNotificationHelper.createSimpleNotification(
            context = this,
            title = notification.title ?: "YourGPT",
            message = notification.body ?: "You have a new message"
        )
        
        YourGPTNotificationHelper.showNotification(
            context = this,
            notificationId = System.currentTimeMillis().toInt(),
            builder = notificationBuilder
        )
    }
    
    private fun showCustomDataNotification(data: Map<String, String>) {
        // Handle custom data payloads with any structure
        val title = data["title"] ?: data["notification"]?.let { 
            try {
                JSONObject(it).getString("title")
            } catch (e: Exception) { null }
        } ?: "YourGPT Message"
        
        val body = data["body"] ?: data["message"] ?: data["notification"]?.let {
            try {
                JSONObject(it).getString("body")
            } catch (e: Exception) { null }
        } ?: "This is notification body"
        
        val notificationBuilder = YourGPTNotificationHelper.createSimpleNotification(
            context = this,
            title = title,
            message = body
        )
        
        YourGPTNotificationHelper.showNotification(
            context = this,
            notificationId = System.currentTimeMillis().toInt(),
            builder = notificationBuilder
        )
    }
    
    private fun storeToken(token: String) {
        val sharedPrefs = getSharedPreferences("yourgpt_sdk_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().apply {
            putString("fcm_token", token)
            putLong("fcm_token_timestamp", System.currentTimeMillis())
            apply()
        }
    }
    
    private suspend fun sendTokenToBackend(token: String) {
        try {
            val config = YourGPTSDKCore.getInstance().currentConfig
            if (config != null && config.enableNotifications) {
                // This would be implemented to send the token to YourGPT backend
                // For now, we'll just log it
                android.util.Log.d(TAG, "Would send token to backend for widget: ${config.widgetUid}")
                
                // In production, this would make an API call like:
                // YourGPTAPI.registerDeviceToken(
                //     widgetUid = config.widgetUid,
                //     token = token,
                //     platform = "android"
                // )
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to send token to backend", e)
        }
    }
}