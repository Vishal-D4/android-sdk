# YourGPT SDK - Crisp-Style Notification System

## Overview

The YourGPT SDK now features a Crisp-inspired notification system that offers both **minimalist** and **advanced** modes, making push notification integration incredibly simple while maintaining flexibility for complex use cases.

## Quick Start (3-Line Integration) 🚀

The simplest way to add YourGPT notifications to your app:

```kotlin
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Quick setup - handles everything automatically
        YourGPTNotificationClient.quickSetup(this, "your-widget-uid")
        
        // 2. Request notification permission (Android 13+)
        requestNotificationPermission()
        
        // 3. Handle notification clicks
        YourGPTNotificationClient.handleNotificationClick(this, intent)
    }
}
```

That's it! Your app now receives notifications when there are new messages in the YourGPT widget.

## Notification Modes

### 1. Minimalist Mode (Default) - Automatic Everything

Perfect for developers who want notifications to "just work" without any configuration.

**Features:**
- Automatic token registration
- Auto-open widget on notification tap
- Default notification styling
- No custom code needed

**Setup:**
```kotlin
YourGPTNotificationClient.quickSetup(
    context = this,
    widgetUid = "your-widget-uid"
)
```

### 2. Advanced Mode - Full Control

For developers who need custom notification handling, styling, and behavior.

**Features:**
- Custom notification styling
- Filter YourGPT vs other notifications
- Custom actions and behaviors
- Full control over notification display

**Setup:**
```kotlin
// Initialize in advanced mode
YourGPTNotificationClient.initialize(
    context = this,
    widgetUid = "your-widget-uid",
    mode = YourGPTNotificationClient.NotificationMode.ADVANCED
)

// Create custom FirebaseMessagingService (see CustomNotificationService.kt)
```

### 3. Disabled Mode

Completely disable notifications while keeping the widget functional.

```kotlin
YourGPTNotificationClient.setNotificationMode(
    YourGPTNotificationClient.NotificationMode.DISABLED
)
```

## Architecture Comparison

### Traditional Approach (Complex)
```
App → YourGPTSDK → YourGPTConfig → NotificationManager → NotificationService → FCM
     ↓
  EventListener → Handle callbacks → Show notification → Handle clicks
```

### Crisp-Style Approach (Simple)
```
App → YourGPTNotificationClient.quickSetup() → Done! ✅
```

## Key Components

### 1. YourGPTNotificationClient
The main entry point for notification handling. Provides static methods for easy integration.

**Key Methods:**
- `quickSetup()` - One-line setup
- `handleNotification()` - Process incoming notifications
- `sendTokenToYourGPT()` - Register FCM token
- `isYourGPTNotification()` - Check if notification is from YourGPT

### 2. YourGPTNotificationHelper
Utility class with helper methods for common notification tasks.

**Features:**
- Pre-built notification templates
- Deep linking support
- Reply action handling
- Notification grouping

### 3. YourGPTNotificationStyle
Pre-defined notification styles for consistent UI.

**Available Styles:**
- `Simple` - Basic text notification
- `Rich` - Expandable with images
- `Conversation` - Chat-style messages
- `Material` - Material Design compliant
- `Urgent` - High priority with vibration
- `Custom` - Fully customizable

## Implementation Examples

### Example 1: Basic Integration (Minimalist)

```kotlin
class MainActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Complete notification setup in one line
        YourGPTNotificationClient.quickSetup(this, "widget-uid")
        
        // That's it! Notifications work automatically
    }
}
```

### Example 2: Custom Styling (Advanced)

```kotlin
class CustomNotificationService : FirebaseMessagingService() {
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if (YourGPTNotificationClient.isYourGPTNotification(remoteMessage)) {
            // Custom handling for YourGPT messages
            showCustomStyledNotification(remoteMessage)
        }
    }
    
    private fun showCustomStyledNotification(message: RemoteMessage) {
        val style = YourGPTNotificationStyle.Material(
            accentColor = Color.BLUE,
            showTimestamp = true
        )
        
        val builder = StyledNotificationBuilder(style)
            .setTitle(message.data["sender_name"] ?: "YourGPT")
            .setContent(message.data["message_content"] ?: "New message")
            .build(this, "channel_id")
        
        YourGPTNotificationHelper.showNotification(this, 123, builder)
    }
}
```

### Example 3: Handling Different Message Types

```kotlin
override fun onMessageReceived(remoteMessage: RemoteMessage) {
    when (remoteMessage.data["message_type"]) {
        "urgent" -> showUrgentNotification(remoteMessage)
        "promotional" -> showPromotionalNotification(remoteMessage)
        else -> YourGPTNotificationClient.handleNotification(this, remoteMessage)
    }
}
```

## Migration Guide

### From Old Implementation to Crisp-Style

**Before (Complex):**
```kotlin
// Old way - multiple steps, complex configuration
val notificationConfig = YourGPTNotificationConfig.builder()
    .setNotificationsEnabled(true)
    .setSmallIcon(R.drawable.ic_notification)
    .setVibrationEnabled(true)
    .setSoundEnabled(true)
    .build()

val config = YourGPTConfig(
    widgetUid = "widget-uid",
    enableNotifications = true,
    notificationConfig = notificationConfig
)

lifecycleScope.launch {
    YourGPTSDK.initialize(this@MainActivity, config)
    YourGPTSDK.subscribeToWidgetTopic(config.widgetUid)
}

YourGPTSDK.setEventListener(object : YourGPTEventListener {
    override fun onFCMTokenReceived(token: String) { }
    override fun onNotificationClicked(extras: Map<String, String>) { }
    // ... more callbacks
})
```

**After (Simple):**
```kotlin
// New way - one line!
YourGPTNotificationClient.quickSetup(this, "widget-uid")
```

## Best Practices

### 1. Choose the Right Mode

- **Use Minimalist Mode when:**
  - You want the fastest integration
  - Default behavior is acceptable
  - You don't need custom notification styling

- **Use Advanced Mode when:**
  - You need custom notification appearance
  - You want to filter notification types
  - You have existing notification infrastructure

### 2. Permission Handling

Always request notification permission for Android 13+:

```kotlin
private val permissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
) { isGranted ->
    if (isGranted) {
        // Permission granted
    }
}

// Request permission
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
}
```

### 3. Testing Notifications

```kotlin
// Enable debug logging
YourGPTNotificationClient.initialize(
    context = this,
    widgetUid = "widget-uid",
    mode = NotificationMode.MINIMALIST
)

// Test with Firebase Console
// 1. Go to Firebase Console > Cloud Messaging
// 2. Send test message with data:
//    - widget_uid: "your-widget-uid"
//    - type: "widget_message"
//    - message_content: "Test message"
```

## Troubleshooting

### Notifications Not Received

1. **Check initialization:**
```kotlin
if (YourGPTNotificationClient.isInitialized()) {
    // Client is ready
}
```

2. **Verify Firebase setup:**
- Ensure `google-services.json` is in place
- Check Firebase Console for errors

3. **Check permissions:**
```kotlin
val enabled = YourGPTNotificationHelper.areNotificationsEnabled(context)
```

### Widget Not Opening on Click

Ensure you're handling notification intents:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    YourGPTNotificationClient.handleNotificationClick(this, intent)
}

override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    intent?.let { YourGPTNotificationClient.handleNotificationClick(this, it) }
}
```

## API Reference

### YourGPTNotificationClient

| Method | Description |
|--------|-------------|
| `quickSetup(context, widgetUid)` | Complete setup in one line |
| `initialize(context, widgetUid, mode)` | Initialize with specific mode |
| `handleNotification(context, message)` | Process incoming notification |
| `handleNotificationClick(activity, intent)` | Handle notification tap |
| `isYourGPTNotification(message)` | Check if from YourGPT |
| `sendTokenToYourGPT(token)` | Register FCM token |
| `resetToken(context)` | Reset and re-register token |
| `setNotificationMode(mode)` | Change notification mode |

### NotificationMode Enum

| Mode | Description |
|------|-------------|
| `MINIMALIST` | Automatic handling (default) |
| `ADVANCED` | Custom handling with callbacks |
| `DISABLED` | Notifications disabled |

## Comparison with Crisp SDK

| Feature | Crisp SDK | YourGPT SDK |
|---------|-----------|-------------|
| Minimalist Mode | ✅ | ✅ |
| Advanced Mode | ✅ | ✅ |
| One-line Setup | ✅ | ✅ |
| Auto Token Registration | ✅ | ✅ |
| Custom Styling | ✅ | ✅ |
| Notification Filtering | ✅ | ✅ |
| Pre-built Styles | ❌ | ✅ |
| Helper Utilities | Limited | ✅ Extensive |

## Summary

The new Crisp-style notification system makes YourGPT SDK integration incredibly simple while maintaining the flexibility needed for complex use cases. With just 3 lines of code, developers can add full notification support to their apps, while advanced users retain complete control over notification behavior and appearance.

## Support

For issues or questions:
- GitHub Issues: [YourGPT SDK Issues](https://github.com/yourgpt/android-sdk)
- Documentation: [Full SDK Docs](https://docs.yourgpt.ai/sdk/android)