package com.devdooly.notificationedge.service

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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

        // NotificationChannel 및 Ticker 정보 추출 (카카오톡 단체방 제목 등 조회용)
        val channelName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ranking = Ranking()
            if (currentRanking?.getRanking(sbn.key, ranking) == true) {
                ranking.channel?.name?.toString()?.trim()
            } else null
        } else null
        val tickerText = notification.tickerText?.toString()?.trim()

        val parsed = com.devdooly.notificationedge.util.MessengerNotificationParser.parse(
            sbn = sbn,
            channelName = channelName,
            tickerText = tickerText
        )

        // 내용이 없는 빈 알림은 무시
        if (parsed.roomTitle.isBlank() && parsed.cleanText.isBlank() && parsed.messages.isEmpty()) return

        val formattedTitle = if (parsed.isGroupChat || parsed.roomTitle.contains(",")) {
            com.devdooly.notificationedge.util.NotificationTextCleaner.formatGroupTitle(parsed.roomTitle)
        } else {
            parsed.roomTitle
        }

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

        val finalTitle = if (formattedTitle.isNotBlank()) formattedTitle else appName

        val extrasDump = dumpExtras(extras, sbn)

        val edgeNotification = EdgeNotification(
            key = sbn.key,
            id = sbn.id,
            packageName = packageName,
            appName = appName,
            appIcon = appIcon,
            title = finalTitle,
            text = parsed.cleanText,
            subText = parsed.groupRoomName ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString(),
            messages = parsed.messages,
            timestamp = sbn.postTime,
            contentIntent = contentIntent,
            actions = actionsList,
            isClearable = sbn.isClearable,
            isGroupChat = parsed.isGroupChat,
            debugExtrasDump = extrasDump
        )

        NotificationRepository.addOrUpdateNotification(edgeNotification)
    }

    private fun dumpExtras(extras: Bundle, sbn: StatusBarNotification): String {
        val sb = StringBuilder()
        sb.append("=== [Notification Extras Debug Dump] ===\n")
        sb.append("Package: ").append(sbn.packageName).append("\n")
        sb.append("Key: ").append(sbn.key).append("\n")
        sb.append("Id: ").append(sbn.id).append("\n")
        sb.append("PostTime: ").append(sbn.postTime).append("\n")
        sb.append("Tag: ").append(sbn.tag).append("\n")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            sb.append("ChannelId: ").append(sbn.notification.channelId).append("\n")
            val ranking = Ranking()
            if (currentRanking?.getRanking(sbn.key, ranking) == true) {
                sb.append("ChannelName: ").append(ranking.channel?.name).append("\n")
            }
        }
        if (sbn.notification.tickerText != null) {
            sb.append("TickerText: \"").append(sbn.notification.tickerText).append("\"\n")
        }
        sb.append("--- Extras Keys & Values ---\n")

        for (key in extras.keySet()) {
            val value = extras.get(key)
            when (value) {
                is CharSequence -> sb.append("• ").append(key).append(" (CharSequence): \"").append(value).append("\"\n")
                is Array<*> -> {
                    sb.append("• ").append(key).append(" (Array[").append(value.size).append("]):\n")
                    value.forEachIndexed { i, item ->
                        if (item is Bundle) {
                            sb.append("    [").append(i).append("] (Bundle): { ")
                            for (bKey in item.keySet()) {
                                val bVal = item.get(bKey)
                                if (bVal is Bundle) {
                                    sb.append(bKey).append("={ ")
                                    for (innerKey in bVal.keySet()) {
                                        sb.append(innerKey).append("=\"").append(bVal.get(innerKey)).append("\", ")
                                    }
                                    sb.append("}, ")
                                } else {
                                    sb.append(bKey).append("=\"").append(bVal).append("\", ")
                                }
                            }
                            sb.append("}\n")
                        } else {
                            sb.append("    [").append(i).append("]: \"").append(item).append("\"\n")
                        }
                    }
                }
                is Bundle -> {
                    sb.append("• ").append(key).append(" (Bundle): { ")
                    for (bKey in value.keySet()) {
                        sb.append(bKey).append("=\"").append(value.get(bKey)).append("\", ")
                    }
                    sb.append("}\n")
                }
                else -> sb.append("• ").append(key).append(" (").append(value?.javaClass?.simpleName ?: "null").append("): \"").append(value).append("\"\n")
            }
        }
        sb.append("=========================================")
        return sb.toString()
    }
}
