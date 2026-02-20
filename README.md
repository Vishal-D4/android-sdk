# YourGPT Android SDK

A Kotlin SDK for integrating YourGPT chatbot widget as a full-screen view in Android applications.

## Quick Start

### Installation

Add the dependency to your app's `build.gradle` file:

```gradle
dependencies {
    implementation 'com.yourgpt:android-sdk:1.0.0'
    implementation 'androidx.webkit:webkit:1.8.0'
    implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.7.0' // For coroutines support
}
```

## Integration Guide

Follow these steps to integrate YourGPT SDK into your Android application:

### Step 1: Update `build.gradle` (App Module)

Add required dependencies to your app's `build.gradle` file:

```gradle
dependencies {
    implementation "org.jetbrains.kotlin:kotlin-stdlib:1.8.20"
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.7.0'
    implementation 'androidx.webkit:webkit:1.8.0'

    // YourGPT SDK
    implementation 'com.yourgpt:android-sdk:1.0.0'
}
```

### Step 2: Update `AndroidManifest.xml`

Add required permissions:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Required permissions -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <application
        android:allowBackup="true"
        android:icon="@drawable/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.AppCompat.Light.DarkActionBar">

        <!-- Your main activity -->
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

### Step 3: Initialize and Open the Chat Widget

```kotlin
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.yourgpt.sdk.*
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Initialize SDK
        val config = YourGPTConfig(widgetUid = "your-widget-uid-here")

        lifecycleScope.launch {
            YourGPTSDK.initialize(this@MainActivity, config)
        }

        // 2. Open chat on button click
        findViewById<View>(R.id.btn_open_chat).setOnClickListener {
            YourGPTSDK.show(this)
        }
    }
}
```

That's it. The SDK handles the WebView, loading states, and lifecycle internally.

## Configuration Options

### YourGPTConfig

```kotlin
val configuration = YourGPTConfig(
    widgetUid = "your-widget-uid",    // Required: Your YourGPT widget UID
    debug = true                      // Optional: Enable debug logs (default: false)
)
```

## SDK Methods

### Initialize SDK

```kotlin
lifecycleScope.launch {
    try {
        YourGPTSDK.initialize(this@MainActivity, configuration)
    } catch (error: Exception) {
        // Handle initialization error
    }
}
```

### Open Chatbot

```kotlin
// Simplest — uses config from initialize()
YourGPTSDK.show(this)

// Or with explicit config and FragmentManager
val config = YourGPTConfig(widgetUid = "your-widget-uid")
YourGPTSDK.openChatbotBottomSheet(supportFragmentManager, config)
```

### Set Event Listener

```kotlin
YourGPTSDK.setEventListener(this) // 'this' implements YourGPTEventListener
```

### Observe SDK State

```kotlin
lifecycleScope.launch {
    YourGPTSDK.stateFlow.collect { state ->
        when (state.connectionState) {
            YourGPTConnectionState.CONNECTED -> {
                // SDK is ready
            }
            YourGPTConnectionState.CONNECTING -> {
                // SDK is connecting
            }
            YourGPTConnectionState.ERROR -> {
                // Handle error: state.error
            }
            YourGPTConnectionState.DISCONNECTED -> {
                // SDK is disconnected
            }
        }
    }
}
```

## Event Listener Interface

Implement `YourGPTEventListener` to receive SDK events:

```kotlin
interface YourGPTEventListener {
    // Required — widget events
    fun onMessageReceived(message: Map<String, Any>)
    fun onChatOpened()
    fun onChatClosed()
    fun onError(error: String)
    fun onLoadingStarted()
    fun onLoadingFinished()

    // Optional — notification events (default no-op implementations provided)
    fun onFCMTokenReceived(token: String) {}
    fun onPushMessageReceived(data: Map<String, Any>) {}
    fun onNotificationClicked(extras: Map<String, String>) {}
    fun onWidgetOpenRequested(widgetUid: String) {}
    fun onNotificationPermissionGranted() {}
    fun onNotificationPermissionDenied() {}
}
```

## SDK States

The SDK provides real-time state updates through `YourGPTSDKState`:

- **CONNECTED**: SDK is connected and ready to use
- **CONNECTING**: SDK is initializing/connecting
- **DISCONNECTED**: SDK is disconnected
- **ERROR**: An error occurred (check `state.error` for details)

## Requirements

- Android API level 21 (Android 5.0) or higher
- Kotlin 1.8.0 or higher
- AndroidX libraries

## ProGuard/R8

If you're using code obfuscation, add these rules to your `proguard-rules.pro`:

```proguard
-keep class com.yourgpt.sdk.** { *; }
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
```
