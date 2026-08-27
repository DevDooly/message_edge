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
    fun parse(
        sbn: StatusBarNotification,
        channelName: String? = null,
        tickerText: String? = null,
        viewTexts: List<String> = emptyList(),
        shortcutLabel: String? = null
    ): ParsedNotificationData {
        val packageName = sbn.packageName
        val notification = sbn.notification ?: return fallback(sbn)
        val extras = notification.extras ?: return fallback(sbn)

        val conversationTitle = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()?.trim()
            ?: extras.getCharSequence("android.hiddenConversationTitle")?.toString()?.trim()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim()
        val summaryText = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()?.trim()
        val isGroupConversation = extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false)
        val hasIsGroupKey = extras.containsKey(Notification.EXTRA_IS_GROUP_CONVERSATION)

        val selfDisplayName = extras.getCharSequence(Notification.EXTRA_SELF_DISPLAY_NAME)?.toString()?.trim()
            ?: extras.getBundle("android.messagingStyleUser")?.getString("name")?.trim()
            ?: ""

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
                packageName.contains("instagram") ||
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
                hasIsGroupKey = hasIsGroupKey,
                selfDisplayName = selfDisplayName,
                channelName = channelName,
                tickerText = tickerText,
                viewTexts = viewTexts,
                shortcutLabel = shortcutLabel,
                extras = extras,
                postTime = sbn.postTime
            )
        }

        // 2. 일반 메신저 (인스타그램 / 메시지 / Telegram / Line 등) 분석
        if (isMessagingStyle || isMessengerApp) {
            return parseGenericMessenger(
                packageName = packageName,
                rawTitle = rawTitle,
                rawText = rawText,
                subText = subText,
                summaryText = summaryText,
                conversationTitle = conversationTitle,
                isGroupConversation = isGroupConversation,
                hasIsGroupKey = hasIsGroupKey,
                selfDisplayName = selfDisplayName,
                channelName = channelName,
                tickerText = tickerText,
                viewTexts = viewTexts,
                shortcutLabel = shortcutLabel,
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
     * 카카오톡 전용 정밀 파서 (Shortcut / NotificationChannel / Ticker / RemoteViews / Extras 덤프 기반)
     */
    private fun parseKakaoTalk(
        rawTitle: String,
        rawText: String,
        subText: String?,
        summaryText: String?,
        conversationTitle: String?,
        isGroupConversation: Boolean,
        hasIsGroupKey: Boolean,
        selfDisplayName: String,
        channelName: String?,
        tickerText: String?,
        viewTexts: List<String>,
        shortcutLabel: String?,
        extras: Bundle,
        postTime: Long
    ): ParsedNotificationData {
        var groupName: String? = null
        var senderName: String = rawTitle
        var messageBody: String = rawText

        // 0) ShortcutLabel (안드로이드 One UI / LauncherApps에 등록된 실제 채팅방 이름, 예: "11단톡")
        if (!shortcutLabel.isNullOrBlank() && !isInvalidChannelName(shortcutLabel) && shortcutLabel != rawTitle) {
            groupName = shortcutLabel
            senderName = rawTitle
        }
        // A) conversationTitle이 존재하는 경우
        else if (!conversationTitle.isNullOrBlank()) {
            groupName = conversationTitle
            senderName = if (rawTitle.isNotBlank() && rawTitle != conversationTitle) rawTitle else "상대방"
        }
        // B) subText가 존재하는 경우 -> 카카오톡은 명명된 단체방일 때 subText에 방 이름을 넣음
        else if (!subText.isNullOrBlank() && subText != rawTitle) {
            groupName = subText
            senderName = rawTitle
        }
        // C) NotificationChannel 이름이 유효한 채팅방 이름인 경우 (예: "11단톡")
        else if (!isInvalidChannelName(channelName) && channelName != rawTitle) {
            groupName = channelName
            senderName = rawTitle
        }
        // D) TickerText에서 단체방 이름 추출 (예: "[11단톡] 김동관: ...")
        else if (!tickerText.isNullOrBlank()) {
            val tickerGroup = extractGroupNameFromTicker(tickerText, rawTitle)
            if (tickerGroup != null) {
                groupName = tickerGroup
                senderName = rawTitle
            }
        }
        // E) summaryText가 존재하는 경우
        else if (!summaryText.isNullOrBlank() && summaryText != rawTitle) {
            groupName = summaryText
            senderName = rawTitle
        }
        // F) rawTitle에 괄호로 단체방 또는 발신자가 묶여있는 경우 (예: "홍길동 (가족방)" 또는 "가족방 (5)")
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
        // G) rawText 본문 안에 "[단체방이름] 발신자: 내용" 패턴이 있는 경우
        if (groupName == null) {
            val bracketMatch = Regex("""^\[([^\]\n]{2,30})\]\s*(.*)$""", RegexOption.DOT_MATCHES_ALL).find(rawText)
            if (bracketMatch != null) {
                val candidateGroup = bracketMatch.groupValues[1].trim()
                val rest = bracketMatch.groupValues[2].trim()
                if (!isInvalidChannelName(candidateGroup) && candidateGroup != "Web발신" && candidateGroup != "알림") {
                    groupName = candidateGroup
                    messageBody = rest
                }
            }
        }

        // H) MessagingStyle 메시지 내부의 bundle extras에서 방 이름 탐색
        if (groupName == null) {
            @Suppress("DEPRECATION")
            val rawMessages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
            if (rawMessages != null) {
                for (raw in rawMessages) {
                    if (raw is Bundle) {
                        val innerExtras = raw.getBundle("extras")
                        if (innerExtras != null) {
                            for (key in innerExtras.keySet()) {
                                val v = innerExtras.getString(key)?.trim()
                                if (!v.isNullOrBlank() && !isInvalidChannelName(v) && v != rawTitle) {
                                    if (key.contains("room", ignoreCase = true) ||
                                        key.contains("title", ignoreCase = true) ||
                                        key.contains("chat", ignoreCase = true) ||
                                        key.contains("group", ignoreCase = true)) {
                                        groupName = v
                                        break
                                    }
                                }
                            }
                        }
                        if (groupName != null) break
                    }
                }
            }
        }

        // I) RemoteViews 렌더링 텍스트 계층에서 방 이름 탐색 (One UI 상태창에 표시된 실제 방 이름)
        if (groupName == null && viewTexts.isNotEmpty()) {
            for (vt in viewTexts) {
                if (vt.isNotBlank() && !isInvalidChannelName(vt) && vt != rawTitle && vt != rawText && !rawText.startsWith(vt)) {
                    groupName = vt
                    break
                }
            }
        }

        // MessagingStyle 메시지 리스트 추출 (본인 메시지 isFromUser = true 태깅)
        val messagesList = extractMessagingStyleMessages(extras, groupName ?: rawTitle, senderName, postTime, selfDisplayName)

        // 본인(나)을 제외한 상대방 고유 발신자 목록 추출 (참여자 목록)
        val otherSenders = messagesList.filter { !it.isFromUser && it.sender != "나" && (selfDisplayName.isBlank() || !it.sender.equals(selfDisplayName, ignoreCase = true)) }
            .map { it.sender.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        // H) 카카오톡 단체방 판별 (안드로이드 OS 명시 플래그 최우선 적용)
        val isGroup = if (hasIsGroupKey) {
            isGroupConversation
        } else {
            otherSenders.size >= 2 || rawTitle.contains(",") || (groupName != null && groupName != rawTitle)
        }

        // 1:1 개인 대화방인 경우 단체방 이름 무효화
        if (!isGroup) {
            groupName = null
        }

        // 명시적 단체방 이름이 없을 때 참여자 목록으로 방 이름 자동 합성
        if (groupName == null && isGroup) {
            groupName = if (otherSenders.isNotEmpty()) {
                buildGroupRoomTitleFromSenders(otherSenders, rawTitle)
            } else if (rawTitle.isNotBlank()) {
                rawTitle
            } else {
                "그룹 채팅방"
            }
        }

        // 단체방 이름 및 대표 타이틀 결정
        val finalRoomTitle = groupName ?: rawTitle
        val latestMsg = messagesList.lastOrNull { !it.isFromUser } ?: messagesList.lastOrNull()
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
                    timestamp = postTime,
                    isFromUser = false
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
     * 일반 메신저 (인스타그램, 문자 등) 파서
     */
    private fun parseGenericMessenger(
        packageName: String,
        rawTitle: String,
        rawText: String,
        subText: String?,
        summaryText: String?,
        conversationTitle: String?,
        isGroupConversation: Boolean,
        hasIsGroupKey: Boolean,
        selfDisplayName: String,
        channelName: String?,
        tickerText: String?,
        viewTexts: List<String>,
        shortcutLabel: String?,
        extras: Bundle,
        postTime: Long
    ): ParsedNotificationData {
        val messagesList = extractMessagingStyleMessages(extras, conversationTitle ?: rawTitle, rawTitle, postTime, selfDisplayName)

        // 본인(나)을 제외한 순수 상대방 발신자 목록
        val otherSenders = messagesList.filter { !it.isFromUser && it.sender != "나" && (selfDisplayName.isBlank() || !it.sender.equals(selfDisplayName, ignoreCase = true)) }
            .map { it.sender.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        // 1. 단체방 여부 판별 (OS 명시 플래그 최우선 적용)
        val isGroup = if (hasIsGroupKey) {
            // android.isGroupConversation이 명시되어 있으면 OS 값을 100% 신뢰
            isGroupConversation
        } else {
            otherSenders.size >= 2 || rawTitle.contains(",")
        }

        // 2. 타이틀 정제 (인스타그램 1:1 대화의 "계정명: 상대방" 패턴 정제)
        var parsedRoomTitle: String = when {
            isGroup && !shortcutLabel.isNullOrBlank() && !isInvalidChannelName(shortcutLabel) && shortcutLabel != rawTitle -> shortcutLabel
            isGroup && !conversationTitle.isNullOrBlank() -> conversationTitle
            isGroup && !subText.isNullOrBlank() && subText != rawTitle -> subText
            isGroup && !isInvalidChannelName(channelName) && channelName != rawTitle -> channelName ?: rawTitle
            else -> rawTitle
        }

        // 1:1 대화에서 "내계정ID: 상대방이름" (인스타그램) 패턴 정제
        // 단, 상대방이름 자리에 본인 이름(selfDisplayName)이 오는 경우(나에게 온 답장 알림)는 앞의 계정ID를 상대방으로 취함
        if (!isGroup && parsedRoomTitle.contains(":")) {
            val parts = parsedRoomTitle.split(":").map { it.trim() }.filter { it.isNotBlank() }
            val candidate = parts.lastOrNull()
            if (candidate != null && selfDisplayName.isNotBlank() && candidate.equals(selfDisplayName, ignoreCase = true)) {
                parsedRoomTitle = parts.first()
            } else if (!candidate.isNullOrBlank()) {
                parsedRoomTitle = candidate
            }
        }

        var groupName: String? = null
        if (isGroup) {
            groupName = when {
                !shortcutLabel.isNullOrBlank() && !isInvalidChannelName(shortcutLabel) && shortcutLabel != rawTitle -> shortcutLabel
                !conversationTitle.isNullOrBlank() -> conversationTitle
                !subText.isNullOrBlank() && subText != rawTitle -> subText
                !isInvalidChannelName(channelName) && channelName != rawTitle -> channelName
                otherSenders.isNotEmpty() -> buildGroupRoomTitleFromSenders(otherSenders, rawTitle)
                else -> null
            }
        }

        val finalRoomTitle: String = (groupName ?: parsedRoomTitle).ifBlank { rawTitle }
        val latestMsg = messagesList.lastOrNull { !it.isFromUser } ?: messagesList.lastOrNull()
        val cleanSender: String = latestMsg?.sender?.ifBlank { finalRoomTitle } ?: finalRoomTitle

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
                    timestamp = postTime,
                    isFromUser = false
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
        postTime: Long,
        selfDisplayName: String
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

                    // 본인(나)이 보낸 답장 메시지인지 판별
                    val isSelfMatch = selfDisplayName.isNotBlank() &&
                            !msgSender.isNullOrBlank() &&
                            msgSender.equals(selfDisplayName, ignoreCase = true) &&
                            !selfDisplayName.equals(roomTitle, ignoreCase = true) &&
                            !selfDisplayName.equals(defaultSender, ignoreCase = true)

                    val isNullSenderSelf = msgSender.isNullOrBlank() && (rawMessages.size > 1 || selfDisplayName == "나")

                    val isFromUser = msgSender == "나" || isSelfMatch || isNullSenderSelf

                    val finalSender = when {
                        isFromUser -> "나"
                        !msgSender.isNullOrBlank() -> msgSender
                        else -> defaultSender
                    }

                    val cleanedMsgText = NotificationTextCleaner.cleanMessageText(
                        text = msgText,
                        title = roomTitle,
                        sender = finalSender
                    )
                    val msgTime = raw.getLong("time", postTime)

                    if (cleanedMsgText.isNotBlank()) {
                        list.add(
                            MessageItem(
                                sender = finalSender,
                                text = cleanedMsgText,
                                timestamp = msgTime,
                                isFromUser = isFromUser
                            )
                        )
                    }
                }
            }
        }
        return list
    }

    private fun isInvalidChannelName(channelName: String?): Boolean {
        if (channelName.isNullOrBlank()) return true
        val lower = channelName.lowercase().trim()
        return lower.contains("알림") ||
                lower.contains("메시지") ||
                lower.contains("message") ||
                lower.contains("notification") ||
                lower.contains("카카오톡") ||
                lower.contains("kakaotalk") ||
                lower.contains("kakao") ||
                lower.contains("기타") ||
                lower.contains("기본") ||
                lower.contains("default") ||
                lower.contains("미분류") ||
                lower.contains("채널") ||
                lower.contains("direct") ||
                lower.contains("대화")
    }

    private fun extractGroupNameFromTicker(tickerText: String?, rawTitle: String): String? {
        if (tickerText.isNullOrBlank()) return null
        // 1. [단체방이름] 발신자: 내용
        val bracketMatch = Regex("""^\[([^\]\n]{2,30})\]""").find(tickerText)
        if (bracketMatch != null) {
            val candidate = bracketMatch.groupValues[1].trim()
            if (!isInvalidChannelName(candidate) && candidate != rawTitle) {
                return candidate
            }
        }
        // 2. 단체방이름: 발신자: 내용
        val parts = tickerText.split(":")
        if (parts.size >= 3) {
            val candidate = parts[0].trim()
            if (!isInvalidChannelName(candidate) && candidate != rawTitle) {
                return candidate
            }
        }
        return null
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
