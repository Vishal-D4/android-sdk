# Migration Guide: Simplified Notification API

## Overview

We've simplified the YourGPT SDK notification system to reduce code duplication and improve developer experience. This guide will help you migrate from the old API to the new simplified API.

## What Changed

### Removed Components
- **YourGPTNotificationManager** - ❌ **REMOVED** - Functionality moved to `YourGPTNotificationClient` and `YourGPTNotificationHelper`
- **ChatbotActivity** - ❌ **REMOVED** - Was referenced in manifest but never implemented
- **Duplicate methods in YourGPTSDK** - Direct notification methods removed in favor of specialized classes

### New Components
- **YourGPTNotificationClient** - Main entry point for notification handling
- **YourGPTNotificationHelper** - Utility methods for common notification tasks

## Migration Steps

### 1. Replace SDK Notification Methods

#### Old Way:
```kotlin
// Getting FCM token
val token = YourGPTSDK.getFCMToken()

// Subscribing to topics
YourGPTSDK.subscribeToWidgetTopic("widget-uid")
YourGPTSDK.unsubscribeFromWidgetTopic("widget-uid")

// Handling notification intents
YourGPTSDK.handleNotificationIntent(intent)

// Clearing notifications
YourGPTSDK.clearAllNotifications()

// Checking if enabled
YourGPTSDK.areNotificationsEnabled()
```

#### New Way:
```kotlin
// Getting FCM token
val token = YourGPTNotificationClient.getFirebaseToken()

// Topics are handled automatically in minimalist mode, or:
// Manual subscription is handled internally by NotificationClient

// Handling notification intents
YourGPTNotificationClient.handleNotificationClick(activity, intent)

// Clearing notifications
YourGPTNotificationHelper.cancelAllNotifications(context)

// Checking if enabled
YourGPTNotificationHelper.areNotificationsEnabled(context)
```

### 2. Replace Removed NotificationManager Usage

#### Old Way (NO LONGER WORKS):
```kotlin
// ❌ These APIs have been removed
YourGPTNotificationManager.initialize(context, config)
YourGPTNotificationManager.setOnTokenReceivedListener { token -> }
YourGPTNotificationManager.setOnMessageReceivedListener { data -> }
YourGPTNotificationManager.setOnNotificationClickedListener { extras -> }
val token = YourGPTNotificationManager.getFCMToken()
YourGPTNotificationManager.handleNotificationIntent(intent)
```

#### New Way (Minimalist Mode):
```kotlin
// Simple one-line setup
YourGPTNotificationClient.quickSetup(context, "widget-uid")

// That's it! Everything is handled automatically
```

#### New Way (Advanced Mode):
```kotlin
// Initialize with advanced mode
YourGPTNotificationClient.initialize(
    context = context,
    widgetUid = "widget-uid",
    mode = YourGPTNotificationClient.NotificationMode.ADVANCED
)

// Token is retrieved automatically, or manually:
val token = YourGPTNotificationClient.getFirebaseToken()

// Handle clicks
YourGPTNotificationClient.handleNotificationClick(activity, intent)
```

### 3. Update Notification Display Code

#### Old Way:
```kotlin
// Creating and showing notifications manually
val notificationBuilder = NotificationCompat.Builder(context, channelId)
    .setSmallIcon(icon)
    .setContentTitle(title)
    .setContentText(message)
    // ... more configuration

notificationManager.notify(id, notificationBuilder.build())
```

#### New Way:
```kotlin
// Use Helper methods for common patterns
val builder = YourGPTNotificationHelper.createRichNotification(
    context = context,
    title = title,
    message = message,
    bigText = expandedMessage,
    clickIntent = pendingIntent
)

YourGPTNotificationHelper.showNotification(context, notificationId, builder)
```

## Quick Migration Checklist

- [ ] Replace `YourGPTSDK.getFCMToken()` with `YourGPTNotificationClient.getFirebaseToken()`
- [ ] Remove calls to `YourGPTSDK.subscribeToWidgetTopic()` (handled automatically)
- [ ] Replace `YourGPTSDK.handleNotificationIntent()` with `YourGPTNotificationClient.handleNotificationClick()`
- [ ] Replace `YourGPTSDK.clearAllNotifications()` with `YourGPTNotificationHelper.cancelAllNotifications()`
- [ ] Replace `YourGPTSDK.areNotificationsEnabled()` with `YourGPTNotificationHelper.areNotificationsEnabled()`
- [ ] Consider switching to `YourGPTNotificationClient.quickSetup()` for simpler integration
- [ ] ❌ **Remove any usage of `YourGPTNotificationManager`** - This class has been completely removed

## Benefits After Migration

1. **Simpler Integration**: 3-line setup vs 20+ lines
2. **Less Code Duplication**: Single source of truth for each function
3. **Better Separation**: Clear distinction between client operations and utilities
4. **Easier Testing**: Modular components are easier to test
5. **Future-Proof**: New architecture allows for easier feature additions

## Example: Complete Migration

### Before (Complex):
```kotlin
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Old complex setup
        val notificationConfig = YourGPTNotificationConfig.builder()
            .setNotificationsEnabled(true)
            .setSmallIcon(R.drawable.ic_notification)
            .build()
        
        val config = YourGPTConfig(
            widgetUid = "widget-uid",
            enableNotifications = true,
            notificationConfig = notificationConfig
        )
        
        lifecycleScope.launch {
            YourGPTSDK.initialize(this@MainActivity, config)
            YourGPTSDK.subscribeToWidgetTopic(config.widgetUid)
            
            val token = YourGPTSDK.getFCMToken()
            // Send token to backend...
        }
        
        // Handle notification clicks
        if (YourGPTSDK.handleNotificationIntent(intent)) {
            // Handled
        }
    }
}
```

### After (Simple):
```kotlin
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // New simple setup
        YourGPTNotificationClient.quickSetup(this, "widget-uid")
        
        // Handle clicks (optional, done automatically in minimalist mode)
        YourGPTNotificationClient.handleNotificationClick(this, intent)
    }
}
```

## Need Help?

If you encounter any issues during migration:
1. Review the example app for implementation patterns
2. Check the [NOTIFICATION_SETUP.md](NOTIFICATION_SETUP.md) for notification setup details
3. Report issues on GitHub

## Deprecation Timeline

- **Current Release**: Deprecated methods show warnings
- **Next Minor Release**: Methods will be marked with `@Deprecated(level = DeprecationLevel.ERROR)`
- **Next Major Release**: Deprecated methods will be removed

Start migrating now to avoid breaking changes in future releases!