# Advanced Notification Usage Guide

This guide shows how to implement custom notification handling for advanced use cases using the YourGPT SDK.

## Prerequisites

To use custom Firebase Messaging Service, you need to add Firebase dependencies to your app's `build.gradle`:

```gradle
dependencies {
    // Firebase dependencies
    implementation platform('com.google.firebase:firebase-bom:32.5.0')
    implementation 'com.google.firebase:firebase-messaging-ktx'
    
    // YourGPT SDK
    implementation project(':yourgpt-sdk')
}
```

Also apply the Google Services plugin:
```gradle
plugins {
    id 'com.google.gms.google-services'
}
```

## Custom FirebaseMessagingService Example

Here's a complete example of a custom notification service for advanced notification handling:

```kotlin
package com.yourgpt.sdk.example

import android.app.PendingIntent
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.yourgpt.sdk.YourGPTNotificationClient
import com.yourgpt.sdk.YourGPTNotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Custom FirebaseMessagingService for advanced notification handling
 * 
 * This example shows how to:
 * 1. Filter YourGPT notifications from other notifications
 * 2. Customize notification appearance
 * 3. Handle different types of messages
 * 4. Add custom actions to notifications
 */
class CustomNotificationService : FirebaseMessagingService() {
    
    companion object {
        private const val TAG = "CustomNotificationService"
        private const val YOUR_WIDGET_UID = "your-widget-uid"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize YourGPT NotificationClient in ADVANCED mode
        YourGPTNotificationClient.initialize(
            context = applicationContext,
            widgetUid = YOUR_WIDGET_UID,
            mode = YourGPTNotificationClient.NotificationMode.ADVANCED
        )
    }
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")
        
        // Send token to YourGPT backend
        CoroutineScope(Dispatchers.IO).launch {
            YourGPTNotificationClient.sendTokenToYourGPT(token)
        }
        
        // You can also send the token to your own backend
        sendTokenToYourBackend(token)
    }
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Log.d(TAG, "Message received from: ${remoteMessage.from}")
        Log.d(TAG, "Message data: ${remoteMessage.data}")
        
        // Check if this is a YourGPT notification
        if (YourGPTNotificationClient.isYourGPTNotification(remoteMessage)) {
            handleYourGPTMessage(remoteMessage)
        } else {
            // Handle other notifications from your app
            handleOtherNotification(remoteMessage)
        }
    }
    
    private fun handleYourGPTMessage(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data
        
        // Extract YourGPT message details
        val messageContent = data["message_content"] ?: "New message"
        val senderName = data["sender_name"] ?: "YourGPT Assistant"
        val messageId = data["message_id"] ?: System.currentTimeMillis().toString()
        val conversationId = data["conversation_id"]
        
        // Customize based on message type
        when (data["message_type"]) {
            "urgent" -> showUrgentNotification(senderName, messageContent, messageId)
            "promotional" -> showPromotionalNotification(senderName, messageContent, messageId)
            else -> showCustomStyledNotification(senderName, messageContent, messageId, conversationId)
        }
    }
    
    private fun showCustomStyledNotification(
        senderName: String,
        messageContent: String,
        messageId: String,
        conversationId: String?
    ) {
        // Create a custom styled notification
        val clickIntent = YourGPTNotificationHelper.createWidgetDeepLink(
            context = this,
            widgetUid = YOUR_WIDGET_UID,
            conversationId = conversationId
        )
        
        val clickPendingIntent = YourGPTNotificationHelper.createClickPendingIntent(
            context = this,
            intent = clickIntent,
            requestCode = messageId.hashCode()
        )
        
        // Build rich notification with custom styling
        val notificationBuilder = YourGPTNotificationHelper.createRichNotification(
            context = this,
            title = senderName,
            message = messageContent,
            bigText = messageContent,
            clickIntent = clickPendingIntent
        ).apply {
            // Custom color
            color = getColor(R.color.colorPrimary)
            
            // Custom large icon
            val largeIcon = BitmapFactory.decodeResource(resources, R.drawable.ic_chat)
            setLargeIcon(largeIcon)
            
            // Add custom actions
            addAction(
                R.drawable.ic_chat,
                "Open Chat",
                clickPendingIntent
            )
        }
        
        // Show the notification
        YourGPTNotificationHelper.showNotification(
            context = this,
            notificationId = messageId.hashCode(),
            builder = notificationBuilder
        )
    }
    
    private fun showUrgentNotification(
        senderName: String,
        messageContent: String,
        messageId: String
    ) {
        // Create an urgent notification with high priority
        val notificationBuilder = YourGPTNotificationHelper.createSimpleNotification(
            context = this,
            title = "⚠️ Urgent: $senderName",
            message = messageContent
        ).apply {
            priority = NotificationCompat.PRIORITY_HIGH
            setVibrate(longArrayOf(0, 500, 100, 500))
            color = getColor(android.R.color.holo_red_dark)
        }
        
        YourGPTNotificationHelper.showNotification(
            context = this,
            notificationId = messageId.hashCode(),
            builder = notificationBuilder
        )
    }
    
    private fun showPromotionalNotification(
        senderName: String,
        messageContent: String,
        messageId: String
    ) {
        // Create a low-priority promotional notification
        val notificationBuilder = YourGPTNotificationHelper.createSimpleNotification(
            context = this,
            title = senderName,
            message = messageContent
        ).apply {
            priority = NotificationCompat.PRIORITY_LOW
            setAutoCancel(true)
        }
        
        YourGPTNotificationHelper.showNotification(
            context = this,
            notificationId = messageId.hashCode(),
            builder = notificationBuilder
        )
    }
    
    private fun handleOtherNotification(remoteMessage: RemoteMessage) {
        // Handle non-YourGPT notifications here
        Log.d(TAG, "Handling other notification type")
        
        val notification = remoteMessage.notification
        if (notification != null) {
            val notificationBuilder = YourGPTNotificationHelper.createSimpleNotification(
                context = this,
                title = notification.title ?: "Notification",
                message = notification.body ?: ""
            )
            
            YourGPTNotificationHelper.showNotification(
                context = this,
                notificationId = System.currentTimeMillis().toInt(),
                builder = notificationBuilder
            )
        }
    }
    
    private fun sendTokenToYourBackend(token: String) {
        // Send FCM token to your backend server
        Log.d(TAG, "Sending token to backend: $token")
        
        // Example:
        // YourBackendAPI.registerFCMToken(
        //     userId = getCurrentUserId(),
        //     token = token,
        //     platform = "android"
        // )
    }
}
```

## AndroidManifest.xml Configuration

To use the custom service, add this to your `AndroidManifest.xml`:

```xml
<service
    android:name=".CustomNotificationService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

## When to Use Custom Notification Service

Use a custom FirebaseMessagingService when you need:

1. **Custom notification styling** beyond what the default provides
2. **Different handling for different message types** (urgent, promotional, etc.)
3. **Integration with your own backend** alongside YourGPT
4. **Custom actions** on notifications
5. **Special filtering logic** for notifications
6. **Analytics or logging** of notification events

## Simple Alternative

If you don't need advanced customization, the simple integration is much easier:

```kotlin
// In your MainActivity.onCreate()
YourGPTNotificationClient.quickSetup(this, "your-widget-uid")
```

This one line handles everything automatically!

## Using Pre-defined Styles

For moderate customization without a custom service, use the built-in styles:

```kotlin
// Use pre-defined notification styles
val style = YourGPTNotificationStyle.Material(
    accentColor = Color.BLUE,
    showTimestamp = true
)

// Or use the urgent style
val urgentStyle = YourGPTNotificationStyle.Urgent(
    vibrationPattern = longArrayOf(0, 500, 100, 500),
    ledColor = Color.RED
)
```

## Troubleshooting

### Build Errors

If you get build errors about unresolved Firebase classes:

1. Ensure you've added Firebase dependencies to your `build.gradle`
2. Apply the Google Services plugin
3. Add `google-services.json` to your app directory
4. Sync your project

### Notifications Not Showing

1. Check if notifications are enabled in device settings
2. Verify Firebase is initialized correctly
3. Check if the notification channel is created
4. Ensure you have the required permissions

## Best Practices

1. **Start Simple**: Use `quickSetup()` first, only create custom service if needed
2. **Test Thoroughly**: Test with app in foreground, background, and killed states
3. **Handle Errors**: Always include error handling in custom implementations
4. **Follow Guidelines**: Respect user preferences and quiet hours
5. **Optimize Battery**: Don't wake the device unnecessarily

## Summary

The YourGPT SDK provides flexible notification handling:
- **Minimalist Mode**: One-line setup for automatic handling
- **Advanced Mode**: Full control with custom FirebaseMessagingService

Choose the approach that best fits your app's requirements!