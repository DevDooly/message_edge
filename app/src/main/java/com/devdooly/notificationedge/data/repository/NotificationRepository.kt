package com.devdooly.notificationedge.data.repository

import android.app.ActivityOptions
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Build
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
    private const val MAX_MESSAGES_PER_NOTIFICATION = 50

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
                contentIntent = notification.contentIntent ?: existing?.contentIntent,
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

    /**
     * 채팅방으로 이동하거나 앱 실행
     */
    fun openNotificationApp(
        context: Context,
        notification: EdgeNotification,
        autoDismiss: Boolean = true,
        onClose: () -> Unit
    ) {
        var launched = false

        // 1. PendingIntent(특정 채팅방으로 이동) 실행 시도
        if (notification.contentIntent != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val options = ActivityOptions.makeBasic().apply {
                        pendingIntentBackgroundActivityStartMode = ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                    }
                    notification.contentIntent.send(context, 0, null, null, null, null, options.toBundle())
                } else {
                    notification.contentIntent.send()
                }
                launched = true
            } catch (e: Exception) {
                e.printStackTrace()
                launched = false
            }
        }

        // 2. PendingIntent가 실패하거나 없는 경우 해당 앱의 런처 실행
        if (!launched) {
            try {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(notification.packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                }
                if (launchIntent != null) {
                    context.startActivity(launchIntent)
                    launched = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 옵션이 켜져 있는 경우 해당 메시지 그룹을 알림 목록에서 자동 삭제
        if (autoDismiss) {
            removeNotification(notification.key)
        }

        onClose()
    }

    fun sendQuickReply(
        context: Context,
        notificationKey: String,
        action: NotificationActionItem,
        replyText: String
    ) {
        if (action.actionIntent == null || action.remoteInputKey == null) return

        try {
            val intent = Intent()
            val bundle = Bundle()
            bundle.putCharSequence(action.remoteInputKey, replyText)

            val remoteInput = RemoteInput.Builder(action.remoteInputKey).build()
            RemoteInput.addResultsToIntent(arrayOf(remoteInput), intent, bundle)

            action.actionIntent.send(context, 0, intent)

            // 내 답장 메시지를 해당 알림 카드의 대화 목록에 즉시 추가하여 채팅방처럼 대화 유지
            _notifications.update { list ->
                list.map { notif ->
                    if (notif.key == notificationKey) {
                        val userMsg = MessageItem(
                            sender = "나",
                            text = replyText,
                            timestamp = System.currentTimeMillis(),
                            isFromUser = true
                        )
                        // 기존 메시지 목록이 비어있었다면 기존 text를 상대방 메시지로 먼저 넣고 내 메시지 추가
                        val currentMsgs = if (notif.messages.isNotEmpty()) {
                            notif.messages
                        } else if (notif.text.isNotBlank()) {
                            listOf(
                                MessageItem(
                                    sender = notif.title.ifBlank { notif.appName },
                                    text = notif.text,
                                    timestamp = notif.timestamp,
                                    isFromUser = false
                                )
                            )
                        } else {
                            emptyList()
                        }
                        notif.copy(
                            messages = (currentMsgs + userMsg).takeLast(MAX_MESSAGES_PER_NOTIFICATION),
                            text = "나: $replyText",
                            timestamp = System.currentTimeMillis()
                        )
                    } else {
                        notif
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
