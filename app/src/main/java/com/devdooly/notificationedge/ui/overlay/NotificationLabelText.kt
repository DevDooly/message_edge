package com.devdooly.notificationedge.ui.overlay

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.devdooly.notificationedge.R
import com.devdooly.notificationedge.data.model.NotificationLabel

/** 캐시된 생성 라벨도 화면의 현재 언어로 표시한다. 실제 알림 원문은 그대로 반환한다. */
@Composable
internal fun notificationLabelText(label: NotificationLabel?, original: String): String {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    return remember(label, original, configuration, context) {
        label?.resolve(context.resources) ?: original
    }
}

internal fun NotificationLabel.resolve(resources: Resources): String = when (this) {
    NotificationLabel.UnknownSender -> resources.getString(R.string.parser_unknown_sender)
    NotificationLabel.GroupChat -> resources.getString(R.string.parser_group_chat)
    is NotificationLabel.SenderSummary -> resources.getQuantityString(
        R.plurals.parser_sender_summary, remainingCount, visibleSenders.joinToString(", "), remainingCount
    )
}
