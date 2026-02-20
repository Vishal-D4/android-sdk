package com.yourgpt.sdk.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.yourgpt.sdk.YourGPTNotificationClient

/**
 * MainActivity demonstrating the simplest integration of YourGPT SDK
 * with push notifications
 */
class MainActivity : AppCompatActivity() {
    
    // Notification permission launcher for Android 13+
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Notifications enabled!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Notifications disabled. You won't receive messages when app is closed.", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // ===== SIMPLE 3-LINE INTEGRATION =====
        // This is all you need for basic notification support!
        
        // 1. Quick setup with YourGPT widget (Minimalist mode - handles everything automatically)
        YourGPTNotificationClient.quickSetup(
            context = this,
            widgetUid = "cad24e4c-6ad9-41ef-b535-3731b48dfa71"
        )
        
        // 2. Request notification permission for Android 13+
        requestNotificationPermission()
        
        // 3. Handle notification clicks when app opens from notification
        handleNotificationIntent(intent)
        
        // ===== END OF SIMPLE INTEGRATION =====
        
        // Navigate to the main app screen
        startActivity(Intent(this, HomeScreenActivity::class.java))
        finish()
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // Handle notification clicks if app was already running
        intent?.let { handleNotificationIntent(it) }
    }
    
    private fun handleNotificationIntent(intent: Intent) {
        // Let the NotificationClient handle it in minimalist mode
        // It will automatically open the widget if needed
        YourGPTNotificationClient.handleNotificationClick(this, intent)
    }
    
    private fun requestNotificationPermission() {
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
}

/**
 * ALTERNATIVE: Advanced Mode Example
 * 
 * If you need more control over notifications, you can use advanced mode:
 * 
 * class MainActivityAdvanced : AppCompatActivity() {
 *     
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *         
 *         // Initialize with advanced mode for custom handling
 *         YourGPTNotificationClient.initialize(
 *             context = this,
 *             widgetUid = "your-widget-uid",
 *             mode = YourGPTNotificationClient.NotificationMode.ADVANCED
 *         )
 *         
 *         // In advanced mode, you handle the notifications yourself
 *         // You can create a custom FirebaseMessagingService (see CustomNotificationService.kt)
 *         // and process notifications however you want
 *     }
 * }
 */