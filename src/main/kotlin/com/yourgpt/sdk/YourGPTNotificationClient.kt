package com.yourgpt.sdk

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.messaging.FirebaseMessaging

/**
 * Simplified client for handling YourGPT notifications
 * Inspired by Crisp's approach for easy integration
 */
object YourGPTNotificationClient {
    
    private const val TAG = "YourGPTNotificationClient"
    private var widgetUid: String? = null
    private var notificationMode: NotificationMode = NotificationMode.MINIMALIST
    private var autoOpenWidget = true
    private var isInitialized = false
    
    enum class NotificationMode {
        MINIMALIST,  // Auto-handle everything
        ADVANCED,    // Custom handling with callbacks
        DISABLED     // No notifications
    }
    
    /**
     * Initialize notification client with minimal configuration
     * This is the simplest way to enable notifications
     * 
     * @param context Application or Activity context
     * @param widgetUid Your widget UID
     * @param mode Notification handling mode (default: MINIMALIST)
     */
    @JvmStatic
    fun initialize(
        context: Context,
        widgetUid: String,
        mode: NotificationMode = NotificationMode.MINIMALIST
    ) {
        this.widgetUid = widgetUid
        this.notificationMode = mode
        this.isInitialized = true
        
        Log.d(TAG, "Initialized with widget: $widgetUid, mode: $mode")
        
        // Auto-register token if in minimalist mode
        if (mode == NotificationMode.MINIMALIST) {
            CoroutineScope(Dispatchers.IO).launch {
                val token = getFirebaseToken()
                token?.let { sendTokenToYourGPT(it) }
                Log.d(TAG, "Token,: $token",)
            }
        }
    }
    
    /**
     * Check if a RemoteMessage is from YourGPT
     * 
     * @param remoteMessage The Firebase message to check
     * @return true if message is from YourGPT
     */
    @JvmStatic
    fun isYourGPTNotification(remoteMessage: RemoteMessage): Boolean {
        val data = remoteMessage.data
        
        // Check for new format with project_uid and type: conversation
        if (data.containsKey("project_uid") && data["type"] == "conversation") {
            // Compare project_uid with configured widgetUid
            return data["project_uid"] == widgetUid
        }
        
        // Fallback to old format for backward compatibility
        return data.containsKey("widget_uid") && 
               data["widget_uid"] == widgetUid &&
               data["type"] == "widget_message"
    }
    
    /**
     * Handle incoming notification in minimalist mode
     * Automatically shows notification and opens widget on tap
     * 
     * @param context Context
     * @param remoteMessage The Firebase message
     * @return true if handled, false otherwise
     */
    @JvmStatic
    fun handleNotification(context: Context, remoteMessage: RemoteMessage): Boolean {
        if (!isInitialized) {
            Log.w(TAG, "NotificationClient not initialized")
            return false
        }
        
        if (notificationMode == NotificationMode.DISABLED) {
            return false
        }
        
        if (!isYourGPTNotification(remoteMessage)) {
            return false
        }
        
        when (notificationMode) {
            NotificationMode.MINIMALIST -> {
                // Auto-handle everything
                showNotificationAndHandleClick(context, remoteMessage)
                return true
            }
            NotificationMode.ADVANCED -> {
                // Let the app handle it via callbacks
                return false
            }
            NotificationMode.DISABLED -> {
                return false
            }
        }
    }
    
    /**
     * Send FCM token to YourGPT backend
     * 
     * @param token The FCM token to register
     */
    @JvmStatic
    suspend fun sendTokenToYourGPT(token: String) {
        if (!isInitialized || widgetUid == null) {
            Log.w(TAG, "Cannot send token - not initialized properly")
            return
        }
        
        if (!isFirebaseAvailable()) {
            Log.w(TAG, "Firebase not available - skipping token registration")
            return
        }
        
        try {
            // Subscribe to widget topic for easy targeting
            FirebaseMessaging.getInstance()
                .subscribeToTopic("widget_$widgetUid")
                .await()
            
            Log.d(TAG, "Token registered for widget: $widgetUid")
            
            // TODO: Send token to YourGPT backend API
            // This would be implemented when backend API is ready
            // Example:
            // YourGPTAPI.registerToken(widgetUid, token, "android")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send token to YourGPT", e)
        }
    }
    
    /**
     * Get current Firebase token
     */
    @JvmStatic
    suspend fun getFirebaseToken(): String? {
        return try {
            // Check if Firebase is initialized
            if (!isFirebaseAvailable()) {
                Log.w(TAG, "Firebase is not initialized. Notifications will not work.")
                return null
            }
            FirebaseMessaging.getInstance().token.await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get Firebase token", e)
            null
        }
    }
    
    /**
     * Check if Firebase is available and initialized
     */
    @JvmStatic
    private fun isFirebaseAvailable(): Boolean {
        return try {
            com.google.firebase.FirebaseApp.getInstance()
            true
        } catch (e: IllegalStateException) {
            false
        }
    }
    
    /**
     * Reset token and re-register with backend
     * Useful when user logs out/in
     */
    @JvmStatic
    fun resetToken(context: Context) {
        if (!isFirebaseAvailable()) {
            Log.w(TAG, "Firebase not available - cannot reset token")
            return
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Delete current token
                FirebaseMessaging.getInstance().deleteToken().await()
                
                // Get new token
                val newToken = getFirebaseToken()
                newToken?.let { sendTokenToYourGPT(it) }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reset token", e)
            }
        }
    }
    
    /**
     * Handle notification click to open widget
     * 
     * @param activity The activity to open widget from
     * @param intent The notification intent
     * @return true if handled
     */
    @JvmStatic
    fun handleNotificationClick(activity: FragmentActivity, intent: Intent): Boolean {
        if (!isInitialized || notificationMode != NotificationMode.MINIMALIST) {
            return false
        }
        
        val action = intent.action
        val widgetUid = intent.getStringExtra("widget_uid")
        
        if (action == "com.yourgpt.sdk.OPEN_WIDGET" && widgetUid == this.widgetUid) {
            openWidget(activity)
            return true
        }
        
        return false
    }
    
    /**
     * Open YourGPT widget
     * 
     * @param activity FragmentActivity to show widget in
     */
    @JvmStatic
    fun openWidget(activity: FragmentActivity) {
        if (!isInitialized || widgetUid == null) {
            Log.w(TAG, "Cannot open widget - not initialized")
            return
        }
        
        val config = YourGPTConfig(
            widgetUid = widgetUid!!,
            enableNotifications = true
        )
        
        YourGPTSDK.openChatbotBottomSheet(activity.supportFragmentManager, config)
    }
    
    /**
     * Set whether to auto-open widget on notification tap
     * Only applies in MINIMALIST mode
     */
    @JvmStatic
    fun setAutoOpenWidget(autoOpen: Boolean) {
        this.autoOpenWidget = autoOpen
    }
    
    /**
     * Get current notification mode
     */
    @JvmStatic
    fun getNotificationMode(): NotificationMode {
        return notificationMode
    }
    
    /**
     * Update notification mode
     */
    @JvmStatic
    fun setNotificationMode(mode: NotificationMode) {
        this.notificationMode = mode
        
        if (mode == NotificationMode.DISABLED && isFirebaseAvailable()) {
            // Unsubscribe from topics
            widgetUid?.let {
                FirebaseMessaging.getInstance().unsubscribeFromTopic("widget_$it")
            }
        }
    }
    
    /**
     * Check if client is initialized
     */
    @JvmStatic
    fun isInitialized(): Boolean {
        return isInitialized
    }
    
    private fun showNotificationAndHandleClick(context: Context, remoteMessage: RemoteMessage) {
        val data = remoteMessage.data
        val notification = remoteMessage.notification
        
        // Extract notification data based on format
        val messageContent: String
        val senderName: String
        val widgetUidToUse: String
        val conversationId: String?
        val messageId: String
        
        // Check if it's the new format
        if (data.containsKey("project_uid") && data["type"] == "conversation") {
            // New format - use notification payload for content
            messageContent = notification?.body ?: data["message_content"] ?: "New message"
            senderName = notification?.title ?: data["sender_name"] ?: "YourGPT"
            widgetUidToUse = data["project_uid"] ?: widgetUid ?: return
            conversationId = data["session_uid"] // Use session_uid as conversation identifier
            messageId = data["messageId"] ?: remoteMessage.messageId ?: System.currentTimeMillis().toString()
        } else {
            // Old format fallback
            messageContent = data["message_content"] ?: notification?.body ?: "New message"
            senderName = data["sender_name"] ?: notification?.title ?: "YourGPT"
            widgetUidToUse = data["widget_uid"] ?: widgetUid ?: return
            conversationId = data["conversation_id"]
            messageId = data["message_id"] ?: System.currentTimeMillis().toString()
        }
        
        // Create deep link intent
        val clickIntent = YourGPTNotificationHelper.createWidgetDeepLink(
            context = context,
            widgetUid = widgetUidToUse,
            conversationId = conversationId
        )
        
        val pendingIntent = YourGPTNotificationHelper.createClickPendingIntent(
            context = context,
            intent = clickIntent,
            requestCode = messageId.hashCode()
        )
        
        // Create and show notification using Helper
        val notificationBuilder = YourGPTNotificationHelper.createSimpleNotification(
            context = context,
            title = senderName,
            message = messageContent,
            clickIntent = pendingIntent
        )
        
        // Show the notification
        YourGPTNotificationHelper.showNotification(
            context = context,
            notificationId = messageId.hashCode(),
            builder = notificationBuilder
        )
    }
    
    /**
     * Quick setup method for one-line initialization
     * Perfect for simple integrations
     * 
     * Example:
     * YourGPTNotificationClient.quickSetup(this, "your-widget-uid")
     */
    @JvmStatic
    fun quickSetup(context: Context, widgetUid: String) {
        initialize(context, widgetUid, NotificationMode.MINIMALIST)
        
        // Create notification channel for Android 8.0+
        YourGPTNotificationHelper.createNotificationChannel(context)
        
        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (context is Activity) {
                // The activity should handle permission request
                Log.d(TAG, "Remember to request POST_NOTIFICATIONS permission for Android 13+")
            }
        }
    }
}