package com.devdooly.notificationedge.data.model

/** 원문과 달리 앱이 생성한 표시값만 식별한다. 병합·본문 정제용 원본 값은 별도로 유지한다. */
sealed interface NotificationLabel {
    data object UnknownSender : NotificationLabel
    data object GroupChat : NotificationLabel
    data class SenderSummary(val visibleSenders: List<String>, val remainingCount: Int) : NotificationLabel
}
