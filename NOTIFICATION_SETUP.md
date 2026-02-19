# YourGPT Widget SDK - Push Notification Setup

This guide explains how to set up push notifications for the YourGPT Widget SDK, allowing your app to receive notifications when there are new messages in the widget, even when the app is closed.

## Features

- **Background Notifications**: Receive notifications when app is closed or in background
- **Rich Notifications**: Display message preview with sender information
- **Click Actions**: Tap notification to open widget directly
- **Customizable**: Configure sound, vibration, LED, and quiet hours
- **Reply Actions**: Quick reply buttons in notifications
- **Notification Grouping**: Stack multiple messages intelligently

## Prerequisites

1. Firebase project configured for your app
2. Google Play Services on the device
3. Android 5.0 (API 21) or higher
4. YourGPT widget UID

## Setup Instructions

### Step 1: Firebase Configuration

1. Create a Firebase project at [Firebase Console](https://console.firebase.google.com)
2. Add your Android app to the Firebase project
3. Download the `google-services.json` file
4. Place `google-services.json` in your app module directory (`/app/`)

### Step 2: Update Build Configuration

Add the Google Services plugin to your app-level `build.gradle`:

```gradle
plugins {
    id 'com.android.application'
    id 'com.google.gms.google-services'
}
```

The SDK already includes Firebase dependencies, so no additional dependencies are needed.

### Step 3: Initialize SDK with Notifications

```kotlin
import com.yourgpt.sdk.*

class MainActivity : AppCompatActivity(), YourGPTEventListener {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configure notifications
        val notificationConfig = YourGPTNotificationConfig.builder()
            .setNotificationsEnabled(true)
            .setSmallIcon(R.drawable.ic_notification)
            .setShowReplyAction(true)
            .setVibrationEnabled(true)
            .setSoundEnabled(true)
            .setMessagePreview(true, 150)
            .setQuietHours(false, 22, 8) // Optional: 10 PM to 8 AM
            .build()
        
        // Initialize SDK with notifications
        val config = YourGPTConfig(
            widgetUid = "YOUR_WIDGET_UID",
            enableNotifications = true,
            notificationConfig = notificationConfig
        )
        
        lifecycleScope.launch {
            YourGPTSDK.initialize(this@MainActivity, config)
            
            // Subscribe to widget notifications
            YourGPTSDK.subscribeToWidgetTopic(config.widgetUid)
        }
        
        // Set event listener
        YourGPTSDK.setEventListener(this)
    }
    
    // Handle notification events
    override fun onFCMTokenReceived(token: String) {
        // FCM token received - send to your backend if needed
        Log.d("MainActivity", "FCM Token: $token")
    }
    
    override fun onNotificationClicked(extras: Map<String, String>) {
        // Handle notification click
        val widgetUid = extras["widget_uid"]
        val messageId = extras["message_id"]
        
        // Open the widget
        openYourGPTWidget()
    }
    
    override fun onWidgetOpenRequested(widgetUid: String) {
        // Auto-open widget when requested
        openYourGPTWidget()
    }
}
```

### Step 4: Request Notification Permission (Android 13+)

For Android 13 (API 33) and higher, request notification permission:

```kotlin
private val notificationPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
) { isGranted: Boolean ->
    if (isGranted) {
        // Permission granted
    } else {
        // Permission denied
    }
}

private fun checkNotificationPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission already granted
            }
            else -> {
                // Request permission
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
```

### Step 5: Handle Notification Intents

Handle notification clicks when app is launched from notification:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    handleNotificationIntent(intent)
}

override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    intent?.let { handleNotificationIntent(it) }
}

private fun handleNotificationIntent(intent: Intent) {
    if (YourGPTSDK.handleNotificationIntent(intent)) {
        // SDK handled the notification
        return
    }
    
    // Custom handling if needed
    when (intent.action) {
        "com.yourgpt.sdk.OPEN_WIDGET" -> {
            // Open widget
            openYourGPTWidget()
        }
    }
}
```

## Notification Configuration Options

### Basic Configuration

```kotlin
YourGPTNotificationConfig.builder()
    .setNotificationsEnabled(true)              // Enable/disable notifications
    .setSmallIcon(R.drawable.ic_notification)   // Notification icon
    .setAutoCancel(true)                        // Auto-dismiss on click
    .build()
```

### Sound & Vibration

```kotlin
YourGPTNotificationConfig.builder()
    .setSoundEnabled(true)                      // Enable sound
    .setSoundUri(customSoundUri)                // Custom sound (optional)
    .setVibrationEnabled(true)                  // Enable vibration
    .setVibrationPattern(longArrayOf(0, 250, 250, 250))
    .build()
```

### LED Configuration

```kotlin
YourGPTNotificationConfig.builder()
    .setLedEnabled(true)                        // Enable LED
    .setLedColor(Color.BLUE)                    // LED color
    .setLedTiming(300, 3000)                    // On/off duration in ms
    .build()
```

### Message Preview

```kotlin
YourGPTNotificationConfig.builder()
    .setMessagePreview(true, 150)               // Show preview, max 150 chars
    .build()
```

### Quiet Hours

```kotlin
YourGPTNotificationConfig.builder()
    .setQuietHours(true, 22, 8)                 // No notifications 10 PM - 8 AM
    .build()
```

### Notification Grouping

```kotlin
YourGPTNotificationConfig.builder()
    .setGroupMessages(true)                     // Group notifications
    .setStackNotifications(true, 5)             // Stack up to 5 notifications
    .build()
```

## API Methods

### Subscribe/Unsubscribe Topics

```kotlin
// Subscribe to widget notifications
YourGPTSDK.subscribeToWidgetTopic("widget_uid")

// Unsubscribe from widget notifications
YourGPTSDK.unsubscribeFromWidgetTopic("widget_uid")
```

### Get FCM Token

```kotlin
lifecycleScope.launch {
    val token = YourGPTSDK.getFCMToken()
    // Use token as needed
}
```

### Clear Notifications

```kotlin
// Clear all notifications
YourGPTSDK.clearAllNotifications()
```

### Check Notification Status

```kotlin
val enabled = YourGPTSDK.areNotificationsEnabled()
```

### Update Notification Config

```kotlin
val newConfig = YourGPTNotificationConfig.builder()
    .setSoundEnabled(false)
    .build()

YourGPTSDK.updateNotificationConfig(newConfig)
```

## Testing Notifications

1. Install the app on a physical device (notifications don't work on emulators without Google Play)
2. Grant notification permission when prompted
3. Close the app completely
4. Send a test message through the YourGPT widget
5. You should receive a notification

## Troubleshooting

### Notifications not received

1. Check Firebase configuration is correct
2. Verify notification permissions are granted
3. Ensure Google Play Services are installed and updated
4. Check if device is connected to internet
5. Verify the widget UID is correct

### Notification permission issues

- For Android 13+, ensure you request `POST_NOTIFICATIONS` permission
- Check app notification settings in device settings
- Verify notification channel is not disabled

### Firebase setup issues

1. Ensure `google-services.json` is in the correct location
2. Verify package name matches Firebase configuration
3. Check Firebase Console for any error messages

## Backend Integration

To send notifications from your backend:

1. Store the FCM token received in `onFCMTokenReceived`
2. Send notification payload to FCM with the following structure:

```json
{
  "to": "FCM_TOKEN",
  "data": {
    "widget_uid": "YOUR_WIDGET_UID",
    "type": "widget_message",
    "message_id": "unique_message_id",
    "conversation_id": "conversation_id",
    "sender_name": "Assistant",
    "message_content": "Hello! How can I help you today?",
    "timestamp": "1234567890000"
  },
  "priority": "high"
}
```

## Support

For issues or questions, please contact YourGPT support or refer to the main SDK documentation.