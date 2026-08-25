package com.devdooly.notificationedge.service

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.pm.PackageManager
import android.os.Build
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
        NotificationRepository.markAsDismissed(sbn.key)
    }

    private fun parseAndAddNotification(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        // 본인 앱의 포그라운드 서비스 알림 등은 제외
        if (packageName == applicationContext.packageName) return

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        // 1. 지속적 알림(Ongoing) 및 고정 알림 필터링
        val isOngoing = (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0 ||
                (notification.flags and Notification.FLAG_NO_CLEAR) != 0
        if (isOngoing) return

        // 2. 미디어 재생 제어 알림 필터링 (YouTube, YouTube Music, Spotify 등의 MediaSession / Transport)
        val isMediaTransport = notification.category == Notification.CATEGORY_TRANSPORT ||
                notification.category == Notification.CATEGORY_SERVICE ||
                extras.containsKey(Notification.EXTRA_MEDIA_SESSION) ||
                extras.containsKey("android.mediaSession")
        if (isMediaTransport) return

        val isGroupConversation = extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false)
        val conversationTitle = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()

        val rawTitle = conversationTitle
            ?: extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()
            ?: ""

        val rawText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString()
            ?: ""

        // 내용이 없는 빈 알림은 무시
        if (rawTitle.isBlank() && rawText.isBlank()) return

        val formattedTitle = if (conversationTitle != null || isGroupConversation || rawTitle.contains(",")) {
            com.devdooly.notificationedge.util.NotificationTextCleaner.formatGroupTitle(rawTitle)
        } else {
            rawTitle
        }

        val cleanedText = com.devdooly.notificationedge.util.NotificationTextCleaner.cleanMessageText(
            rawText,
            formattedTitle
        )

        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
        val pm = applicationContext.packageManager

        // 대화형 알림(카카오톡, 문자 등 MessagingStyle) 메시지 추출
        val messagesList = mutableListOf<com.devdooly.notificationedge.data.model.MessageItem>()
        val rawMessages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
        if (rawMessages != null) {
            for (raw in rawMessages) {
                if (raw is android.os.Bundle) {
                    val msgText = raw.getCharSequence("text")?.toString() ?: ""

                    // 발신자 이름 추출 (Android P Person 객체, bundle, sender 문자열 등 모두 지원)
                    var msgSender: String? = raw.getCharSequence("sender")?.toString()
                    if (msgSender == null) {
                        val personObj = raw.get("sender_person")
                        if (personObj is android.os.Bundle) {
                            msgSender = personObj.getCharSequence("name")?.toString()
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && personObj is android.app.Person) {
                            msgSender = personObj.name?.toString()
                        }
                    }
                    if (msgSender == null) {
                        msgSender = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: formattedTitle
                    }

                    val cleanedMsgText = com.devdooly.notificationedge.util.NotificationTextCleaner.cleanMessageText(
                        msgText,
                        formattedTitle,
                        msgSender
                    )
                    val msgTime = raw.getLong("time", System.currentTimeMillis())

                    if (cleanedMsgText.isNotBlank()) {
                        messagesList.add(
                            com.devdooly.notificationedge.data.model.MessageItem(
                                sender = msgSender,
                                text = cleanedMsgText,
                                timestamp = msgTime
                            )
                        )
                    }
                }
            }
        }

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
            title = formattedTitle,
            text = cleanedText,
            subText = subText,
            messages = messagesList,
            timestamp = sbn.postTime,
            contentIntent = contentIntent,
            actions = actionsList,
            isClearable = sbn.isClearable
        )

        NotificationRepository.addOrUpdateNotification(edgeNotification)
    }
}
