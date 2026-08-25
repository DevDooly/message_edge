package com.devdooly.notificationedge.data.repository

import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.devdooly.notificationedge.data.model.EdgeNotification
import com.devdooly.notificationedge.data.model.MessageItem
import com.devdooly.notificationedge.data.model.NotificationActionItem
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object NotificationRepository {

    private const val MAX_HISTORY_COUNT = 150

    private val _notifications = MutableStateFlow<List<EdgeNotification>>(emptyList())
    val notifications: StateFlow<List<EdgeNotification>> = _notifications.asStateFlow()

    // 알림 수신 이벤트 (Edge Lighting 트리거용)
    private val _newNotificationEvent = MutableSharedFlow<EdgeNotification>(extraBufferCapacity = 5)
    val newNotificationEvent: SharedFlow<EdgeNotification> = _newNotificationEvent.asSharedFlow()

    // 리스너 서비스 인스턴스 콜백
    var cancelNotificationCallback: ((String) -> Unit)? = null
    var cancelAllNotificationsCallback: (() -> Unit)? = null

    fun addOrUpdateNotification(notification: EdgeNotification) {
        _notifications.update { list ->
            val existing = list.firstOrNull { 
                it.key == notification.key || 
                (it.packageName == notification.packageName && it.title == notification.title && notification.title.isNotBlank())
            }

            val mergedMessages = if (existing != null) {
                val combined = existing.messages.toMutableList()
                if (notification.messages.isNotEmpty()) {
                    combined.addAll(notification.messages)
                } else if (notification.text.isNotBlank()) {
                    combined.add(
                        MessageItem(
                            sender = notification.title,
                            text = notification.text,
                            timestamp = notification.timestamp
                        )
                    )
                }
                combined.distinctBy { "${it.timestamp}_${it.text}" }
                    .sortedBy { it.timestamp }
                    .takeLast(50)
            } else {
                if (notification.messages.isNotEmpty()) {
                    notification.messages.takeLast(50)
                } else if (notification.text.isNotBlank()) {
                    listOf(
                        MessageItem(
                            sender = notification.title,
                            text = notification.text,
                            timestamp = notification.timestamp
                        )
                    )
                } else {
                    emptyList()
                }
            }

            val updatedNotification = notification.copy(
                messages = mergedMessages,
                isDismissed = false
            )

            val filteredList = list.filterNot { 
                it.key == notification.key || 
                (it.packageName == notification.packageName && it.title == notification.title && notification.title.isNotBlank())
            }

            (listOf(updatedNotification) + filteredList).take(MAX_HISTORY_COUNT)
        }
        _newNotificationEvent.tryEmit(notification)
    }

    /**
     * 시스템 상태바에서 알림이 지워졌을 때 호출: 목록에서 삭제하지 않고 과거 히스토리로 보존
     */
    fun markAsDismissed(key: String) {
        _notifications.update { list ->
            list.map {
                if (it.key == key) it.copy(isDismissed = true) else it
            }
        }
    }

    /**
     * 사용자가 엣지 패널에서 명시적으로 삭제 버튼을 눌렀을 때
     */
    fun removeNotification(key: String) {
        cancelNotificationCallback?.invoke(key)
        _notifications.update { list ->
            list.filterNot { it.key == key }
        }
    }

    /**
     * 모두 지우기
     */
    fun clearAll() {
        _notifications.value = emptyList()
        cancelAllNotificationsCallback?.invoke()
    }

    fun dismissNotification(key: String) {
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
