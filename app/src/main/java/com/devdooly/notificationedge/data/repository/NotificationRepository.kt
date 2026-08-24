package com.devdooly.notificationedge.data.repository

import android.app.Notification
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.devdooly.notificationedge.data.model.EdgeNotification
import com.devdooly.notificationedge.data.model.NotificationActionItem
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object NotificationRepository {

    private val _notifications = MutableStateFlow<List<EdgeNotification>>(emptyList())
    val notifications: StateFlow<List<EdgeNotification>> = _notifications.asStateFlow()

    // 알림 수신 이벤트 (Edge Lighting 트리거용)
    private val _newNotificationEvent = MutableSharedFlow<EdgeNotification>(extraBufferCapacity = 5)
    val newNotificationEvent: SharedFlow<EdgeNotification> = _newNotificationEvent.asSharedFlow()

    // 리스너 서비스 인스턴스 참조
    var listenerServiceRef: (() -> Unit)? = null
    var cancelNotificationCallback: ((String) -> Unit)? = null
    var cancelAllNotificationsCallback: (() -> Unit)? = null

    fun addOrUpdateNotification(notification: EdgeNotification) {
        _notifications.update { list ->
            val existingIndex = list.indexOfFirst { it.key == notification.key }
            if (existingIndex >= 0) {
                list.toMutableList().apply {
                    set(existingIndex, notification)
                }
            } else {
                listOf(notification) + list
            }
        }
        _newNotificationEvent.tryEmit(notification)
    }

    fun removeNotification(key: String) {
        _notifications.update { list ->
            list.filterNot { it.key == key }
        }
    }

    fun clearAll() {
        _notifications.value = emptyList()
        cancelAllNotificationsCallback?.invoke()
    }

    fun dismissNotification(key: String) {
        cancelNotificationCallback?.invoke(key)
        removeNotification(key)
    }

    fun sendQuickReply(context: Context, action: NotificationActionItem, replyText: String) {
        if (action.actionIntent == null || action.remoteInputKey == null) return

        try {
            val intent = Intent()
            val bundle = Bundle()
            bundle.putCharSequence(action.remoteInputKey, replyText)

            val remoteInput = RemoteInput.Builder(action.remoteInputKey).build()
            RemoteInput.addResultsToIntent(arrayOf(remoteInput), intent, bundle)

            action.actionIntent.send(context, 0, intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
