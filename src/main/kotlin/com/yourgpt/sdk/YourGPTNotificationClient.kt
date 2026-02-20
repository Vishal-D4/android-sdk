package com.yourgpt.sdk

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.webkit.WebView
import androidx.fragment.app.FragmentActivity
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.messaging.FirebaseMessaging

/**
 * Simplified client for handling YourGPT notifications
 */
object YourGPTNotificationClient {
    
    private const val TAG = "YourGPTNotificationClient"
    private var widgetUid: String? = null
    private var notificationMode: NotificationMode = NotificationMode.MINIMALIST
    private var isInitialized = false
    private var cachedFcmToken: String? = null
    private var isTokenRegisteredViaWebView = false
    
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

        // Persist widgetUid so the service can self-initialize when the app is killed
        context.getSharedPreferences("yourgpt_sdk_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("widget_uid", widgetUid)
            .apply()

        Log.d(TAG, "Initialized with widget: $widgetUid, mode: $mode")
        
        // Auto-fetch and cache token if in minimalist mode
        if (mode == NotificationMode.MINIMALIST) {
            CoroutineScope(Dispatchers.IO).launch {
                val token = getFirebaseToken()
                token?.let { cacheToken(it) }
                Log.d(TAG, "FCM token cached: ${token != null}")
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

        Log.d(TAG, "isYourGPTNotification check — data keys: ${data.keys}  widgetUid=$widgetUid")

        // Priority 1: widget_uid (exact match with configured widgetUid)
        if (data.containsKey("widget_uid")) {
            val match = data["widget_uid"] == widgetUid
            Log.d(TAG, "Matched on widget_uid: widget_uid=${data["widget_uid"]}  match=$match")
            return match
        }

        // Priority 2: project_uid (fallback)
        if (data.containsKey("project_uid")) {
            val match = data["project_uid"] == widgetUid
            Log.d(TAG, "Matched on project_uid: project_uid=${data["project_uid"]}  match=$match")
            return match
        }

        Log.d(TAG, "Not a YourGPT notification — no matching keys found")
        return false
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
            Log.w(TAG, "handleNotification: client not initialized — skipping")
            return false
        }

        if (notificationMode == NotificationMode.DISABLED) {
            Log.i(TAG, "handleNotification: mode is DISABLED — notification suppressed")
            return false
        }

        val isYourGPT = isYourGPTNotification(remoteMessage)
        Log.i(TAG, "handleNotification: isYourGPTNotification=$isYourGPT  mode=$notificationMode  data=${remoteMessage.data}")

        if (!isYourGPT) {
            Log.d(TAG, "handleNotification: not a YourGPT notification — not handled here")
            return false
        }

        when (notificationMode) {
            NotificationMode.MINIMALIST -> {
                Log.i(TAG, "handleNotification: MINIMALIST mode — showing notification automatically")
                showNotificationAndHandleClick(context, remoteMessage)
                return true
            }
            NotificationMode.ADVANCED -> {
                Log.i(TAG, "handleNotification: ADVANCED mode — delegating to app callback")
                return false
            }
            NotificationMode.DISABLED -> {
                return false
            }
        }
    }
    
    /**
     * Cache FCM token locally. The token will be sent to the YourGPT backend
     * securely through the WebView JS bridge when the widget is opened,
     * avoiding the need for a public API endpoint.
     *
     * @param token The FCM token to cache
     */
    @JvmStatic
    fun cacheToken(token: String) {
        cachedFcmToken = token
        isTokenRegisteredViaWebView = false
        Log.d(TAG, "FCM token cached, will register via WebView when widget opens")
    }

    /**
     * Send the cached FCM token to the widget backend through the WebView JS bridge.
     * This is called automatically when the widget WebView finishes loading.
     *
     * @param webView The widget WebView instance
     */
    @JvmStatic
    fun registerTokenViaWebView(webView: WebView) {
        val token = cachedFcmToken
        val uid = widgetUid

        if (token == null || uid == null) {
            Log.d(TAG, "No cached token or widgetUid to register via WebView")
            return
        }

        if (isTokenRegisteredViaWebView) {
            Log.d(TAG, "Token already registered via WebView for this session")
            return
        }

        val script = """
            (function() {
                window.postMessage({
                    type: 'register_fcm_token',
                    payload: {
                        token: '$token',
                        platform: 'android',
                        widget_uid: '$uid'
                    }
                }, '*');
            })();
        """.trimIndent()

        Log.i(TAG, "Sending FCM token via WebView JS bridge — token=$token")
        webView.evaluateJavascript(script) { result ->
            isTokenRegisteredViaWebView = true
            Log.i(TAG, "FCM token successfully sent to widget backend — result=$result")
        }
    }

    /**
     * Get the cached FCM token
     */
    @JvmStatic
    fun getCachedToken(): String? = cachedFcmToken
    
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

                // Get new token and cache it
                val newToken = getFirebaseToken()
                newToken?.let { cacheToken(it) }

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
        
        // Detect YourGPT backend format by presence of widget_uid or project_uid
        val isBackendFormat = data.containsKey("widget_uid") || data.containsKey("project_uid")

        if (isBackendFormat) {
            // YourGPT backend format: reads title/body first, falls back to sender_name/message_content
            senderName = notification?.title ?: data["title"] ?: data["sender_name"] ?: "YourGPT"
            messageContent = notification?.body ?: data["body"] ?: data["message_content"] ?: "New message"
            widgetUidToUse = data["widget_uid"] ?: data["project_uid"] ?: widgetUid ?: return
            conversationId = data["session_uid"] ?: data["conversation_id"]
            messageId = data["messageId"] ?: data["message_id"] ?: remoteMessage.messageId ?: System.currentTimeMillis().toString()
        } else {
            // Unknown format fallback
            messageContent = data["message_content"] ?: notification?.body ?: "New message"
            senderName = data["sender_name"] ?: notification?.title ?: "YourGPT"
            widgetUidToUse = widgetUid ?: return
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
        
        // Group key based on session_uid so same-session notifications thread together
        val groupKey = if (conversationId != null) "yourgpt_session_$conversationId" else "yourgpt_messages"

        // Create and show notification using Helper
        val notificationBuilder = YourGPTNotificationHelper.createSimpleNotification(
            context = context,
            title = senderName,
            message = messageContent,
            clickIntent = pendingIntent
        ).apply {
            setGroup(groupKey)
        }

        // Show the notification
        YourGPTNotificationHelper.showNotification(
            context = context,
            notificationId = messageId.hashCode(),
            builder = notificationBuilder
        )

        // Show/update summary notification for this session thread
        if (conversationId != null) {
            val summaryBuilder = YourGPTNotificationHelper.createGroupSummary(
                context = context,
                groupKey = groupKey,
                summaryText = "New messages from $senderName"
            )
            YourGPTNotificationHelper.showNotification(
                context = context,
                notificationId = "summary_$conversationId".hashCode(),
                builder = summaryBuilder
            )
        }

        Log.i(TAG, "Notification displayed — title='$senderName'  body='$messageContent'  group='$groupKey'  id=${messageId.hashCode()}")
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