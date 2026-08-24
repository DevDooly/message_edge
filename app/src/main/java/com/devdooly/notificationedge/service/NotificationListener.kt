package com.devdooly.notificationedge.service

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.devdooly.notificationedge.data.model.EdgeNotification
import com.devdooly.notificationedge.data.model.NotificationActionItem
import com.devdooly.notificationedge.data.repository.NotificationRepository

class NotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        
        NotificationRepository.cancelNotificationCallback = { key ->
            try {
                cancelNotification(key)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        NotificationRepository.cancelAllNotificationsCallback = {
            try {
                cancelAllNotifications()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 연결 시점에 현재 쌓여있는 활성 알림들 로드
        try {
            activeNotifications?.forEach { sbn ->
                parseAndAddNotification(sbn)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        NotificationRepository.cancelNotificationCallback = null
        NotificationRepository.cancelAllNotificationsCallback = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return
        parseAndAddNotification(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn == null) return
        NotificationRepository.removeNotification(sbn.key)
    }

    private fun parseAndAddNotification(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        // 본인 앱의 포그라운드 서비스 알림 등은 제외
        if (packageName == applicationContext.packageName) return

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        // 지속적 알림(Ongoing) 필터링 - 포그라운드 서비스 등
        val isOngoing = (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0
        if (isOngoing) return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()
            ?: ""

        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString()
            ?: ""

        // 내용이 없는 빈 알림은 무시
        if (title.isBlank() && text.isBlank()) return

        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
        val pm = applicationContext.packageManager

        val appName = try {
            val appInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }

        val appIcon = try {
            pm.getApplicationIcon(packageName)
        } catch (e: Exception) {
            null
        }

        val contentIntent = notification.contentIntent

        // Actions 및 Quick Reply 파싱
        val actionsList = mutableListOf<NotificationActionItem>()
        notification.actions?.forEach { action ->
            var isReply = false
            var remoteInputKey: String? = null

            action.remoteInputs?.forEach { remoteInput ->
                if (remoteInput.allowFreeFormInput) {
                    isReply = true
                    remoteInputKey = remoteInput.resultKey
                }
            }

            actionsList.add(
                NotificationActionItem(
                    title = action.title ?: "",
                    actionIntent = action.actionIntent,
                    isReply = isReply,
                    remoteInputKey = remoteInputKey
                )
            )
        }

        val edgeNotification = EdgeNotification(
            key = sbn.key,
            id = sbn.id,
            packageName = packageName,
            appName = appName,
            appIcon = appIcon,
            title = title,
            text = text,
            subText = subText,
            timestamp = sbn.postTime,
            contentIntent = contentIntent,
            actions = actionsList,
            isClearable = sbn.isClearable
        )

        NotificationRepository.addOrUpdateNotification(edgeNotification)
    }
}
