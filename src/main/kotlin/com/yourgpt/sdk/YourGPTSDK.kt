package com.yourgpt.sdk

import android.content.Context
import android.content.Intent
import androidx.fragment.app.FragmentManager
import kotlinx.coroutines.flow.StateFlow

object YourGPTSDK {
    const val VERSION = "1.0.0"
    
    private val core = YourGPTSDKCore.getInstance()
    private var eventListener: YourGPTEventListener? = null
    private var applicationContext: Context? = null
    
    suspend fun initialize(context: Context, configuration: YourGPTConfig) {
        applicationContext = context.applicationContext
        core.initialize(configuration)
        
        // Initialize notifications based on mode
        if (configuration.enableNotifications) {
            when (configuration.notificationMode) {
                NotificationMode.MINIMALIST -> {
                    // Initialize NotificationClient for automatic handling
                    YourGPTNotificationClient.initialize(
                        context = context,
                        widgetUid = configuration.widgetUid,
                        mode = YourGPTNotificationClient.NotificationMode.MINIMALIST
                    )
                    
                    // Auto-register token if enabled
                    if (configuration.autoRegisterToken) {
                        val token = YourGPTNotificationClient.getFirebaseToken()
                        token?.let { YourGPTNotificationClient.sendTokenToYourGPT(it) }
                    }
                }
                NotificationMode.ADVANCED -> {
                    // Initialize for advanced mode with callbacks
                    val notificationConfig = configuration.notificationConfig ?: YourGPTNotificationConfig()
                    initializeNotifications(context, notificationConfig)
                    
                    // Initialize NotificationClient in advanced mode
                    YourGPTNotificationClient.initialize(
                        context = context,
                        widgetUid = configuration.widgetUid,
                        mode = YourGPTNotificationClient.NotificationMode.ADVANCED
                    )
                }
                NotificationMode.DISABLED -> {
                    // Notifications disabled, do nothing
                }
            }
        }
    }
    
    /**
     * Quick initialization for minimalist mode
     * Simplest way to get started with YourGPT SDK
     */
    suspend fun quickInitialize(context: Context, widgetUid: String) {
        val config = YourGPTConfig(
            widgetUid = widgetUid,
            enableNotifications = true,
            notificationMode = NotificationMode.MINIMALIST,
            autoRegisterToken = true
        )
        initialize(context, config)
    }
    
    private fun initializeNotifications(context: Context, notificationConfig: YourGPTNotificationConfig) {
        // Set up notification callbacks for advanced mode
        // The actual notification handling is done by YourGPTNotificationClient
        // This method is kept for backward compatibility but simplified
        
        // Notification events are now handled through the event listener
        // Token management is handled by YourGPTNotificationClient
    }
    
    fun setEventListener(listener: YourGPTEventListener?) {
        eventListener = listener
        ChatbotBottomSheetDialog.setEventListener(listener)
    }
    
    fun openChatbotBottomSheet(fragmentManager: FragmentManager, configuration: YourGPTConfig) {
        val bottomSheet = ChatbotBottomSheetDialog.newInstance(configuration)
        // Show the dialog but keep it hidden until loading is complete
        bottomSheet.show(fragmentManager, "ChatbotBottomSheet")
    }
    
    fun createChatbotBottomSheet(configuration: YourGPTConfig): ChatbotBottomSheetDialog {
        return ChatbotBottomSheetDialog.newInstance(configuration)
    }
    
    suspend fun setUserContext(context: Map<String, Any>) {
        core.setUserContext(context)
    }
    
    val isReady: Boolean
        get() = core.isReady
    
    val currentState: YourGPTSDKState
        get() = core.currentState
    
    val stateFlow: StateFlow<YourGPTSDKState>
        get() = core.state
    
    fun buildWidgetUrl(additionalParams: Map<String, String> = emptyMap()): String {
        return core.buildWidgetUrl(additionalParams)
    }
    
    fun on(event: String, callback: (Any?) -> Unit) {
        core.on(event, callback)
    }
    
    fun off(event: String, callback: (Any?) -> Unit) {
        core.off(event, callback)
    }
    
    fun destroy() {
        core.destroy()
    }
}