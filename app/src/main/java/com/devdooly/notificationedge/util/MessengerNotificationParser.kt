package com.devdooly.notificationedge.util

import android.app.Notification
import android.os.Build
import android.os.Bundle
import android.service.notification.StatusBarNotification
import com.devdooly.notificationedge.data.model.MessageItem

/**
 * 카카오톡, 라인, 텔레그램, 기본 메시지 등 메신저별 알림 데이터 구조 파서
 */
data class ParsedNotificationData(
    val roomTitle: String,         // 단체방 이름 또는 1:1 상대방 이름
    val groupRoomName: String?,    // 단체방일 경우의 순수 방 이름 (예: "우리 가족방" 또는 참여자 목록)
    val isGroupChat: Boolean,      // 단체방 여부
    val currentSender: String,     // 현재 메시지를 보낸 사람
    val cleanText: String,         // 정제된 메시지 본문
    val messages: List<MessageItem>// 대화 목록
)

object MessengerNotificationParser {

    /**
     * StatusBarNotification의 extras를 분석하여 앱별 맞춤 단체방/발신자/메시지 데이터 추출
     */
    fun parse(sbn: StatusBarNotification): ParsedNotificationData {
        val packageName = sbn.packageName
        val notification = sbn.notification ?: return fallback(sbn)
        val extras = notification.extras ?: return fallback(sbn)

        val conversationTitle = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()?.trim()
            ?: extras.getCharSequence("android.hiddenConversationTitle")?.toString()?.trim()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim()
        val summaryText = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()?.trim()
        val isGroupConversation = extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false)

        val rawTitle = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()
            ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG)?.toString()?.trim()
            ?: ""

        val rawText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()
            ?: extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString()?.trim()
            ?: ""

        val isKakaoTalk = packageName == "com.kakao.talk"
        val template = extras.getCharSequence("android.template")?.toString()
            ?: extras.getCharSequence("androidx.core.app.extra.COMPAT_TEMPLATE")?.toString()
            ?: ""
        val hasMessages = extras.get("android.messages") != null
        val isMessagingStyle = template.contains("MessagingStyle") || hasMessages
        val isMessengerApp = isKakaoTalk ||
                packageName.contains("telegram") ||
                packageName.contains("line") ||
                packageName.contains("discord") ||
                packageName.contains("mms") ||
                packageName.contains("message") ||
                packageName.contains("chat")

        // 1. 카카오톡 특화 정밀 분석
        if (isKakaoTalk) {
            return parseKakaoTalk(
                rawTitle = rawTitle,
                rawText = rawText,
                subText = subText,
                summaryText = summaryText,
                conversationTitle = conversationTitle,
                isGroupConversation = isGroupConversation,
                extras = extras,
                postTime = sbn.postTime
            )
        }

        // 2. 일반 메신저 (MessagingStyle / Telegram / Line / SMS) 분석
        if (isMessagingStyle || isMessengerApp) {
            return parseGenericMessenger(
                rawTitle = rawTitle,
                rawText = rawText,
                subText = subText,
                summaryText = summaryText,
                conversationTitle = conversationTitle,
                isGroupConversation = isGroupConversation,
                extras = extras,
                postTime = sbn.postTime
            )
        }

        // 3. 일반 단일 알림 (토스증권, 알리익스프레스, 시스템 알림 등 - 대화형이 아님)
        return parseSingleNotification(
            rawTitle = rawTitle,
            rawText = rawText,
            subText = subText
        )
    }

    /**
     * 일반 단일 알림 파서 (중복 발신자 라벨 및 가짜 단체방 방지)
     */
    private fun parseSingleNotification(
        rawTitle: String,
        rawText: String,
        subText: String?
    ): ParsedNotificationData {
        val cleanText = NotificationTextCleaner.cleanMessageText(
            text = rawText,
            title = rawTitle,
            sender = ""
        )
        return ParsedNotificationData(
            roomTitle = rawTitle,
            groupRoomName = null,
            isGroupChat = false,
            currentSender = rawTitle,
            cleanText = cleanText,
            messages = emptyList()
        )
    }

    /**
     * 카카오톡 전용 정밀 파서 (실제 Notification Extras 덤프 기반)
     */
    private fun parseKakaoTalk(
        rawTitle: String,
        rawText: String,
        subText: String?,
        summaryText: String?,
        conversationTitle: String?,
        isGroupConversation: Boolean,
        extras: Bundle,
        postTime: Long
    ): ParsedNotificationData {
        var groupName: String? = null
        var senderName: String = rawTitle
        var messageBody: String = rawText

        // A) subText가 존재하는 경우 -> 카카오톡은 명명된 단체방일 때 subText에 방 이름을 넣음
        if (!subText.isNullOrBlank() && subText != rawTitle) {
            groupName = subText
            senderName = rawTitle
        }
        // B) conversationTitle이 존재하는 경우
        else if (!conversationTitle.isNullOrBlank()) {
            groupName = conversationTitle
            senderName = if (rawTitle.isNotBlank() && rawTitle != conversationTitle) rawTitle else "상대방"
        }
        // C) summaryText가 존재하는 경우
        else if (!summaryText.isNullOrBlank() && summaryText != rawTitle) {
            groupName = summaryText
            senderName = rawTitle
        }
        // D) rawTitle에 괄호로 단체방 또는 발신자가 묶여있는 경우 (예: "홍길동 (가족방)" 또는 "가족방 (5)")
        else if (rawTitle.contains("(") && rawTitle.contains(")")) {
            val parenMatch = Regex("""^(.*?)\s*\((.*?)\)$""").find(rawTitle)
            if (parenMatch != null) {
                val p1 = parenMatch.groupValues[1].trim()
                val p2 = parenMatch.groupValues[2].trim()
                if (p2.toIntOrNull() != null) {
                    groupName = rawTitle
                    senderName = ""
                } else {
                    senderName = p1
                    groupName = p2
                }
            }
        }
        // E) rawText 본문 안에 "[단체방이름] 발신자: 내용" 패턴이 있는 경우
        if (groupName == null) {
            val bracketMatch = Regex("""^\[([^\]\n]{2,30})\]\s*(.*)$""", RegexOption.DOT_MATCHES_ALL).find(rawText)
            if (bracketMatch != null) {
                val candidateGroup = bracketMatch.groupValues[1].trim()
                val rest = bracketMatch.groupValues[2].trim()
                if (candidateGroup != "Web발신" && candidateGroup != "알림") {
                    groupName = candidateGroup
                    messageBody = rest
                }
            }
        }

        // MessagingStyle 메시지 리스트 추출
        val messagesList = extractMessagingStyleMessages(extras, groupName ?: rawTitle, senderName, postTime)

        // 고유 발신자 목록 추출 (참여자 목록)
        val uniqueSenders = messagesList.map { it.sender.trim() }.filter { it.isNotBlank() }.distinct()

        // F) 카카오톡 무제(그룹) 단체방인 경우: android.isGroupConversation = true 이거나 참여자가 2명 이상인 경우
        val isGroup = isGroupConversation || groupName != null || uniqueSenders.size >= 2 || rawTitle.contains(",")

        // 명시적 단체방 이름이 없을 때 참여자 목록으로 방 이름 자동 합성
        if (groupName == null && isGroup) {
            groupName = if (uniqueSenders.isNotEmpty()) {
                buildGroupRoomTitleFromSenders(uniqueSenders, rawTitle)
            } else if (rawTitle.isNotBlank()) {
                "$rawTitle 외 (단체방)"
            } else {
                "그룹 채팅방"
            }
        }

        // 단체방 이름 및 대표 타이틀 결정
        val finalRoomTitle = groupName ?: rawTitle
        val latestMsg = messagesList.lastOrNull()
        val cleanSender = if (latestMsg != null && latestMsg.sender.isNotBlank()) {
            latestMsg.sender
        } else if (senderName.isNotBlank()) {
            senderName
        } else if (isGroup) {
            "상대방"
        } else {
            finalRoomTitle
        }

        // 메시지 본문 정제
        val cleanedText = NotificationTextCleaner.cleanMessageText(
            text = latestMsg?.text ?: messageBody,
            title = finalRoomTitle,
            sender = cleanSender
        )

        if (messagesList.isEmpty() && cleanedText.isNotBlank()) {
            messagesList.add(
                MessageItem(
                    sender = cleanSender,
                    text = cleanedText,
                    timestamp = postTime
                )
            )
        }

        return ParsedNotificationData(
            roomTitle = finalRoomTitle,
            groupRoomName = groupName,
            isGroupChat = isGroup,
            currentSender = cleanSender,
            cleanText = cleanedText,
            messages = messagesList
        )
    }

    /**
     * 일반 메신저 및 앱 공통 파서
     */
    private fun parseGenericMessenger(
        rawTitle: String,
        rawText: String,
        subText: String?,
        summaryText: String?,
        conversationTitle: String?,
        isGroupConversation: Boolean,
        extras: Bundle,
        postTime: Long
    ): ParsedNotificationData {
        val messagesList = extractMessagingStyleMessages(extras, conversationTitle ?: rawTitle, rawTitle, postTime)
        val uniqueSenders = messagesList.map { it.sender.trim() }.filter { it.isNotBlank() }.distinct()

        val isGroup = !conversationTitle.isNullOrBlank() ||
                isGroupConversation ||
                (!subText.isNullOrBlank() && subText != rawTitle) ||
                uniqueSenders.size >= 2 ||
                rawTitle.contains(",")

        var groupName = when {
            !conversationTitle.isNullOrBlank() -> conversationTitle
            !subText.isNullOrBlank() && isGroup -> subText
            !summaryText.isNullOrBlank() && isGroup -> summaryText
            else -> null
        }

        if (groupName == null && isGroup && uniqueSenders.isNotEmpty()) {
            groupName = buildGroupRoomTitleFromSenders(uniqueSenders, rawTitle)
        }

        val finalRoomTitle = groupName ?: rawTitle
        val latestMsg = messagesList.lastOrNull()
        val cleanSender = latestMsg?.sender?.ifBlank { rawTitle } ?: finalRoomTitle

        val cleanedText = NotificationTextCleaner.cleanMessageText(
            text = latestMsg?.text ?: rawText,
            title = finalRoomTitle,
            sender = cleanSender
        )

        if (messagesList.isEmpty() && cleanedText.isNotBlank()) {
            messagesList.add(
                MessageItem(
                    sender = cleanSender,
                    text = cleanedText,
                    timestamp = postTime
                )
            )
        }

        return ParsedNotificationData(
            roomTitle = finalRoomTitle,
            groupRoomName = groupName,
            isGroupChat = isGroup,
            currentSender = cleanSender,
            cleanText = cleanedText,
            messages = messagesList
        )
    }

    /**
     * 참여자 목록(Senders)을 기반으로 안드로이드 One UI / 카카오톡 시스템 알림 표준 방 제목 생성
     */
    private fun buildGroupRoomTitleFromSenders(senders: List<String>, fallbackTitle: String): String {
        return when {
            senders.isEmpty() -> fallbackTitle.ifBlank { "그룹 채팅방" }
            senders.size == 1 -> senders[0]
            senders.size == 2 -> "${senders[0]}, ${senders[1]}"
            senders.size == 3 -> "${senders[0]}, ${senders[1]}, ${senders[2]}"
            else -> "${senders[0]}, ${senders[1]}, ${senders[2]} 외 ${senders.size - 3}명"
        }
    }

    private fun extractMessagingStyleMessages(
        extras: Bundle,
        roomTitle: String,
        defaultSender: String,
        postTime: Long
    ): MutableList<MessageItem> {
        val list = mutableListOf<MessageItem>()
        @Suppress("DEPRECATION")
        val rawMessages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
        if (rawMessages != null) {
            for (raw in rawMessages) {
                if (raw is Bundle) {
                    val msgText = raw.getCharSequence("text")?.toString() ?: ""
                    var msgSender: String? = raw.getCharSequence("sender")?.toString()?.trim()
                    if (msgSender.isNullOrBlank()) {
                        @Suppress("DEPRECATION")
                        val personObj = raw.get("sender_person")
                        if (personObj is Bundle) {
                            msgSender = personObj.getCharSequence("name")?.toString()?.trim()
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && personObj is android.app.Person) {
                            msgSender = personObj.name?.toString()?.trim()
                        }
                    }
                    if (msgSender.isNullOrBlank()) {
                        msgSender = defaultSender
                    }

                    val cleanedMsgText = NotificationTextCleaner.cleanMessageText(
                        text = msgText,
                        title = roomTitle,
                        sender = msgSender
                    )
                    val msgTime = raw.getLong("time", postTime)

                    if (cleanedMsgText.isNotBlank()) {
                        list.add(
                            MessageItem(
                                sender = msgSender,
                                text = cleanedMsgText,
                                timestamp = msgTime
                            )
                        )
                    }
                }
            }
        }
        return list
    }

    private fun fallback(sbn: StatusBarNotification): ParsedNotificationData {
        return ParsedNotificationData(
            roomTitle = "",
            groupRoomName = null,
            isGroupChat = false,
            currentSender = "",
            cleanText = "",
            messages = emptyList()
        )
    }
}
