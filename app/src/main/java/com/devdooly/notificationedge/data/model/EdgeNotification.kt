package com.devdooly.notificationedge.data.model

import android.app.Notification
import android.app.PendingIntent
import android.graphics.drawable.Drawable

data class NotificationActionItem(
    val title: CharSequence,
    val actionIntent: PendingIntent?,
    val isReply: Boolean = false,
    val remoteInputKey: String? = null
)

data class MessageItem(
    val sender: String,
    val text: String,
    val timestamp: Long
)

data class EdgeNotification(
    val key: String,
    val id: Int,
    val packageName: String,
    val appName: String,
    val appIcon: Drawable? = null,
    val title: String,
    val text: String,
    val subText: String? = null,
    val messages: List<MessageItem> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val contentIntent: PendingIntent? = null,
    val actions: List<NotificationActionItem> = emptyList(),
    val isClearable: Boolean = true,
    val isGroupHeader: Boolean = false,
    val isDismissed: Boolean = false
)
