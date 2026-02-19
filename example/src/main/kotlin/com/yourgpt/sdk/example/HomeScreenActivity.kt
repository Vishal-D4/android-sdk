package com.yourgpt.sdk.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.yourgpt.sdk.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class HomeScreenActivity : AppCompatActivity(), YourGPTEventListener {
    
    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNavigationView: BottomNavigationView
    private var isSDKInitialized = false
    
    // Notification permission launcher
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            onNotificationPermissionGranted()
        } else {
            onNotificationPermissionDenied()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_screen)
        
        setupUI()
        setupSDK()
        checkNotificationPermission()
        initializeSDK()
        handleNotificationIntent(intent)
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleNotificationIntent(it) }
    }
    
    private fun setupUI() {
        viewPager = findViewById(R.id.viewPager)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        
        // Setup ViewPager with fragments
        val fragments = listOf(
            HomeFragment(),
            ExchangeFragment(),
            OrdersFragment(),
            SupportFragment()
        )
        
        val adapter = ViewPagerAdapter(this, fragments)
        viewPager.adapter = adapter
        viewPager.isUserInputEnabled = false
        
        // Connect bottom navigation with ViewPager
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    viewPager.currentItem = 0
                    true
                }
                R.id.navigation_exchange -> {
                    viewPager.currentItem = 1
                    true
                }
                R.id.navigation_orders -> {
                    viewPager.currentItem = 2
                    true
                }
                R.id.navigation_support -> {
                    viewPager.currentItem = 3
                    true
                }
                else -> false
            }
        }
        
        // Sync ViewPager with bottom navigation
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                when (position) {
                    0 -> bottomNavigationView.selectedItemId = R.id.navigation_home
                    1 -> bottomNavigationView.selectedItemId = R.id.navigation_exchange
                    2 -> bottomNavigationView.selectedItemId = R.id.navigation_orders
                    3 -> bottomNavigationView.selectedItemId = R.id.navigation_support
                }
            }
        })
        
        // Set Support tab as the default selected tab
        viewPager.setCurrentItem(3, false)
        bottomNavigationView.selectedItemId = R.id.navigation_support
    }
    
    private fun setupSDK() {
        YourGPTSDK.setEventListener(this)
        
        lifecycleScope.launch {
            YourGPTSDK.stateFlow.collect { state ->
                when (state.connectionState) {
                    YourGPTConnectionState.CONNECTED -> {
                        isSDKInitialized = true
                    }
                    YourGPTConnectionState.ERROR -> {
                        isSDKInitialized = false
                        state.error?.let { error ->
                            Toast.makeText(
                                this@HomeScreenActivity,
                                "SDK Error: $error",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    else -> {
                        isSDKInitialized = false
                    }
                }
            }
        }
    }
    
    private fun initializeSDK() {
        val notificationConfig = YourGPTNotificationConfig.builder()
            .setNotificationsEnabled(true)
            .setSmallIcon(R.drawable.ic_chat)
            .setShowReplyAction(true)
            .setVibrationEnabled(true)
            .setSoundEnabled(true)
            .setMessagePreview(true, 150)
            .setQuietHours(false)
            .build()
        
        val configuration = YourGPTConfig(
            widgetUid = "69dd8b5d-d4bf-444c-a40f-732d15248ae9",
            enableNotifications = true,
            notificationConfig = notificationConfig
        )
        
        lifecycleScope.launch {
            try {
                YourGPTSDK.initialize(this@HomeScreenActivity, configuration)
                // Topic subscription is now handled automatically by NotificationClient
            } catch (error: Exception) {
                Toast.makeText(
                    this@HomeScreenActivity,
                    "SDK initialization failed: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
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
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Show rationale to the user
                    Toast.makeText(
                        this,
                        "Notifications are needed to receive messages when the app is closed",
                        Toast.LENGTH_LONG
                    ).show()
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    // Request permission
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
    
    private fun handleNotificationIntent(intent: Intent) {
        // Handle notification click using the new NotificationClient
        if (YourGPTNotificationClient.handleNotificationClick(this, intent)) {
            // Notification was handled by SDK
            return
        }
        
        // Check for specific actions
        when (intent.action) {
            "com.yourgpt.sdk.OPEN_WIDGET" -> {
                // Auto-open the support tab and chat
                viewPager.setCurrentItem(3, true)
                bottomNavigationView.selectedItemId = R.id.navigation_support
                // Delay to ensure fragment is ready
                viewPager.postDelayed({
                    openSupportChat()
                }, 300)
            }
        }
    }
    
    private fun openSupportChat() {
        val configuration = YourGPTConfig(
            widgetUid = "69dd8b5d-d4bf-444c-a40f-732d15248ae9",
        )
        
        YourGPTSDK.openChatbotBottomSheet(supportFragmentManager, configuration)
    }
    
    // YourGPTEventListener implementation
    override fun onMessageReceived(message: Map<String, Any>) {
        // Handle message received from chatbot
    }
    
    override fun onChatOpened() {
        // Handle chat opened event
    }
    
    override fun onChatClosed() {
        // Handle chat closed event
    }
    
    override fun onError(error: String) {
        runOnUiThread {
            Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onLoadingStarted() {
        // Handle loading started
    }
    
    override fun onLoadingFinished() {
        // Handle loading finished
    }
    
    // Notification event handlers
    override fun onFCMTokenReceived(token: String) {
        // FCM token received, can be sent to your backend
        android.util.Log.d("HomeScreenActivity", "FCM Token: $token")
    }
    
    override fun onPushMessageReceived(data: Map<String, Any>) {
        // Handle push message data
        android.util.Log.d("HomeScreenActivity", "Push message received: $data")
    }
    
    override fun onNotificationClicked(extras: Map<String, String>) {
        // Handle notification click
        val widgetUid = extras["widget_uid"]
        val messageId = extras["message_id"]
        
        runOnUiThread {
            // Navigate to support tab
            viewPager.setCurrentItem(3, true)
            bottomNavigationView.selectedItemId = R.id.navigation_support
            
            // Open chat after a short delay
            viewPager.postDelayed({
                openSupportChat()
            }, 300)
        }
    }
    
    override fun onWidgetOpenRequested(widgetUid: String) {
        // Widget open requested from notification
        runOnUiThread {
            openSupportChat()
        }
    }
    
    override fun onNotificationPermissionGranted() {
        Toast.makeText(this, "Notifications enabled! You'll receive messages when app is closed.", Toast.LENGTH_LONG).show()
    }
    
    override fun onNotificationPermissionDenied() {
        Toast.makeText(this, "Notifications disabled. You won't receive messages when app is closed.", Toast.LENGTH_LONG).show()
    }
    
    // ViewPager adapter
    private inner class ViewPagerAdapter(
        activity: AppCompatActivity,
        private val fragments: List<Fragment>
    ) : FragmentStateAdapter(activity) {
        
        override fun getItemCount(): Int = fragments.size
        
        override fun createFragment(position: Int): Fragment = fragments[position]
    }
}