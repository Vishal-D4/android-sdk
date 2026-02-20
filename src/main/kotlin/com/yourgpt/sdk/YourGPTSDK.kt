package com.yourgpt.sdk

import android.content.Context
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
                    // Initialize NotificationClient for automatic handling.
                    // Token is fetched and cached during initialize().
                    // It will be sent to the backend via WebView JS bridge
                    // when the widget is opened — no public API needed.
                    YourGPTNotificationClient.initialize(
                        context = context,
                        widgetUid = configuration.widgetUid,
                        mode = YourGPTNotificationClient.NotificationMode.MINIMALIST
                    )
                }
                NotificationMode.ADVANCED -> {
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

    fun setEventListener(listener: YourGPTEventListener?) {
        eventListener = listener
        ChatbotBottomSheetDialog.setEventListener(listener)
    }
    
    fun openChatbotBottomSheet(fragmentManager: FragmentManager, configuration: YourGPTConfig) {
        val bottomSheet = ChatbotBottomSheetDialog.newInstance(configuration)
        // Show the dialog but keep it hidden until loading is complete
        bottomSheet.show(fragmentManager, "ChatbotBottomSheet")
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