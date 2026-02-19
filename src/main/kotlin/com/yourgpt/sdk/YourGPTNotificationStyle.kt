package com.yourgpt.sdk

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.app.NotificationCompat

/**
 * Pre-defined notification styles for YourGPT notifications
 * Makes it easy to apply consistent styling across notifications
 */
sealed class YourGPTNotificationStyle {
    
    abstract fun applyTo(builder: NotificationCompat.Builder)
    
    /**
     * Simple text notification - minimal and clean
     */
    object Simple : YourGPTNotificationStyle() {
        override fun applyTo(builder: NotificationCompat.Builder) {
            builder.apply {
                priority = NotificationCompat.PRIORITY_DEFAULT
                setAutoCancel(true)
            }
        }
    }
    
    /**
     * Rich notification with expandable content
     */
    class Rich(
        private val bigText: String? = null,
        private val largeIcon: Bitmap? = null,
        private val imageUrl: String? = null
    ) : YourGPTNotificationStyle() {
        override fun applyTo(builder: NotificationCompat.Builder) {
            builder.apply {
                priority = NotificationCompat.PRIORITY_HIGH
                setAutoCancel(true)
                
                // Add expandable big text
                bigText?.let {
                    setStyle(NotificationCompat.BigTextStyle().bigText(it))
                }
                
                // Add large icon
                largeIcon?.let {
                    setLargeIcon(it)
                }
            }
        }
    }
    
    /**
     * Conversation style for chat messages
     */
    class Conversation(
        private val conversationTitle: String,
        private val messages: List<Message>
    ) : YourGPTNotificationStyle() {
        
        data class Message(
            val text: String,
            val timestamp: Long,
            val sender: String,
            val isUser: Boolean = false
        )
        
        override fun applyTo(builder: NotificationCompat.Builder) {
            val messagingStyle = NotificationCompat.MessagingStyle("Me")
                .setConversationTitle(conversationTitle)
            
            messages.forEach { message ->
                messagingStyle.addMessage(
                    message.text,
                    message.timestamp,
                    message.sender
                )
            }
            
            builder.apply {
                setStyle(messagingStyle)
                priority = NotificationCompat.PRIORITY_HIGH
                setAutoCancel(true)
            }
        }
    }
    
    /**
     * Inbox style for multiple notifications
     */
    class Inbox(
        private val lines: List<String>,
        private val summaryText: String? = null
    ) : YourGPTNotificationStyle() {
        override fun applyTo(builder: NotificationCompat.Builder) {
            val inboxStyle = NotificationCompat.InboxStyle()
            
            lines.forEach { line ->
                inboxStyle.addLine(line)
            }
            
            summaryText?.let {
                inboxStyle.setSummaryText(it)
            }
            
            builder.apply {
                setStyle(inboxStyle)
                priority = NotificationCompat.PRIORITY_DEFAULT
                setAutoCancel(true)
            }
        }
    }
    
    /**
     * Material Design styled notification
     */
    class Material(
        private val accentColor: Int = Color.parseColor("#2196F3"),
        private val showTimestamp: Boolean = true,
        private val showActions: Boolean = true
    ) : YourGPTNotificationStyle() {
        override fun applyTo(builder: NotificationCompat.Builder) {
            builder.apply {
                color = accentColor
                setShowWhen(showTimestamp)
                priority = NotificationCompat.PRIORITY_DEFAULT
                setAutoCancel(true)
                setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            }
        }
    }
    
    /**
     * Minimal notification - least intrusive
     */
    object Minimal : YourGPTNotificationStyle() {
        override fun applyTo(builder: NotificationCompat.Builder) {
            builder.apply {
                priority = NotificationCompat.PRIORITY_LOW
                setAutoCancel(true)
                setShowWhen(false)
                setOnlyAlertOnce(true)
            }
        }
    }
    
    /**
     * Urgent notification - high priority with sound and vibration
     */
    class Urgent(
        private val vibrationPattern: LongArray = longArrayOf(0, 500, 100, 500),
        private val ledColor: Int = Color.RED
    ) : YourGPTNotificationStyle() {
        override fun applyTo(builder: NotificationCompat.Builder) {
            builder.apply {
                priority = NotificationCompat.PRIORITY_HIGH
                setAutoCancel(true)
                setVibrate(vibrationPattern)
                setLights(ledColor, 300, 3000)
                setDefaults(NotificationCompat.DEFAULT_SOUND)
            }
        }
    }
    
    /**
     * Custom style with all options configurable
     */
    class Custom(
        private val priority: Int = NotificationCompat.PRIORITY_DEFAULT,
        private val autoCancel: Boolean = true,
        private val showWhen: Boolean = true,
        private val color: Int? = null,
        private val vibrationPattern: LongArray? = null,
        private val ledColor: Int? = null,
        private val ledOnMs: Int = 300,
        private val ledOffMs: Int = 3000,
        private val sound: Boolean = false,
        private val onlyAlertOnce: Boolean = false,
        private val visibility: Int = NotificationCompat.VISIBILITY_PRIVATE,
        private val category: String? = null,
        private val group: String? = null,
        private val groupSummary: Boolean = false,
        private val sortKey: String? = null
    ) : YourGPTNotificationStyle() {
        override fun applyTo(builder: NotificationCompat.Builder) {
            builder.apply {
                setPriority(priority)
                setAutoCancel(autoCancel)
                setShowWhen(showWhen)
                setOnlyAlertOnce(onlyAlertOnce)
                setVisibility(visibility)
                
                color?.let { setColor(it) }
                vibrationPattern?.let { setVibrate(it) }
                ledColor?.let { setLights(it, ledOnMs, ledOffMs) }
                category?.let { setCategory(it) }
                group?.let { setGroup(it) }
                sortKey?.let { setSortKey(it) }
                
                if (sound) {
                    setDefaults(NotificationCompat.DEFAULT_SOUND)
                }
                
                setGroupSummary(groupSummary)
            }
        }
    }
}

/**
 * Builder for creating styled notifications easily
 */
class StyledNotificationBuilder(
    private val style: YourGPTNotificationStyle
) {
    private var title: String = ""
    private var content: String = ""
    private var smallIcon: Int = android.R.drawable.ic_dialog_info
    private val actions = mutableListOf<NotificationCompat.Action>()
    
    fun setTitle(title: String) = apply { this.title = title }
    fun setContent(content: String) = apply { this.content = content }
    fun setSmallIcon(icon: Int) = apply { this.smallIcon = icon }
    
    fun addAction(action: NotificationCompat.Action) = apply {
        actions.add(action)
    }
    
    fun build(context: android.content.Context, channelId: String): NotificationCompat.Builder {
        val builder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(smallIcon)
        
        // Apply the style
        style.applyTo(builder)
        
        // Add actions
        actions.forEach { builder.addAction(it) }
        
        return builder
    }
}

/**
 * Pre-configured notification styles for common use cases
 */
object NotificationStyles {
    
    /**
     * Default YourGPT notification style
     */
    val DEFAULT = YourGPTNotificationStyle.Material()
    
    /**
     * Style for new message notifications
     */
    fun newMessage(senderName: String, messageText: String) = 
        YourGPTNotificationStyle.Rich(bigText = messageText)
    
    /**
     * Style for conversation notifications
     */
    fun conversation(title: String, messages: List<YourGPTNotificationStyle.Conversation.Message>) =
        YourGPTNotificationStyle.Conversation(title, messages)
    
    /**
     * Style for summary notifications (multiple messages)
     */
    fun summary(messages: List<String>, count: Int) =
        YourGPTNotificationStyle.Inbox(
            lines = messages,
            summaryText = "$count new messages"
        )
    
    /**
     * Style for urgent/important notifications
     */
    val URGENT = YourGPTNotificationStyle.Urgent()
    
    /**
     * Style for silent/background notifications
     */
    val SILENT = YourGPTNotificationStyle.Minimal
    
    /**
     * Material Design compliant style
     */
    fun material(color: Int) = YourGPTNotificationStyle.Material(accentColor = color)
}