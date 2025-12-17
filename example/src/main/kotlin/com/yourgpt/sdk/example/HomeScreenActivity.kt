package com.yourgpt.sdk.example

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_screen)
        
        setupUI()
        setupSDK()
        initializeSDK()
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
        val configuration = YourGPTConfig(
            widgetUid = "69dd8b5d-d4bf-444c-a40f-732d15248ae9",
        )
        
        lifecycleScope.launch {
            try {
                YourGPTSDK.initialize(configuration)
            } catch (error: Exception) {
                Toast.makeText(
                    this@HomeScreenActivity,
                    "SDK initialization failed: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
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
    
    // ViewPager adapter
    private inner class ViewPagerAdapter(
        activity: AppCompatActivity,
        private val fragments: List<Fragment>
    ) : FragmentStateAdapter(activity) {
        
        override fun getItemCount(): Int = fragments.size
        
        override fun createFragment(position: Int): Fragment = fragments[position]
    }
}