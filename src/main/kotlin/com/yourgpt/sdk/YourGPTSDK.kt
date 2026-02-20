package com.yourgpt.sdk

import android.content.Context
import androidx.fragment.app.FragmentActivity
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
        if (configuration.enableNotifications && configuration.notificationMode != NotificationMode.DISABLED) {
            YourGPTNotificationClient.initialize(
                context = context,
                widgetUid = configuration.widgetUid,
                mode = configuration.notificationMode
            )
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
        bottomSheet.show(fragmentManager, "ChatbotBottomSheet")
    }

    /**
     * Open the chatbot widget using the configuration from initialize().
     * Simplest way to show the widget — one-liner after initialization.
     *
     * Example: YourGPTSDK.show(this)
     */
    fun show(activity: FragmentActivity) {
        val config = core.currentConfig
            ?: throw IllegalStateException("SDK not initialized. Call initialize() first.")
        openChatbotBottomSheet(activity.supportFragmentManager, config)
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