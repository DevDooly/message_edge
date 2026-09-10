package com.devdooly.notificationedge.ui.overlay

import com.devdooly.notificationedge.ui.theme.*

import android.content.Context
import android.icu.text.RelativeDateTimeFormatter
import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.devdooly.notificationedge.R
import com.devdooly.notificationedge.data.model.EdgeNotification
import com.devdooly.notificationedge.ui.theme.DarkCardBackground
import com.devdooly.notificationedge.ui.theme.EdgeCyan
import com.devdooly.notificationedge.ui.theme.GlassBorder
import java.util.Locale

@Composable
internal fun NotificationCard(
    notification: EdgeNotification,
    isReplyActive: Boolean,
    onToggleReply: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val is24Hour = DateFormat.is24HourFormat(context)
    val replyAction = remember(notification.actions) {
        notification.actions.firstOrNull { it.isReply }
    }
    var isExpandedMessages by remember { mutableStateOf(false) }

    val timeString = remember(notification.timestamp, locale) {
        formatPanelRelativeTime(context, notification.timestamp)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isDismissed) Graphite900 else DarkCardBackground
        ),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            if (isReplyActive) EdgeCyan else (if (notification.isDismissed) Color(0x22FFFFFF) else GlassBorder)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // 상단 앱 정보 및 닫기 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 앱 아이콘
                if (notification.appIcon != null) {
                    val bitmap = remember(notification.appIcon) {
                        try {
                            notification.appIcon.toBitmap(48, 48).asImageBitmap()
                        } catch (e: Exception) {
                            null
                        }
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = notification.appName,
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                }

                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.appName,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // 단체방이거나 subText가 있는 경우 상단에 방 태그 노출
                    val sub = notification.subText
                    if (!sub.isNullOrBlank() && sub != notification.title && sub != notification.appName) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "› ${notificationLabelText(notification.subTextLabel, sub)}",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Text(
                    text = timeString,
                    color = TextMuted,
                    fontSize = 10.sp
                )

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.panel_dismiss),
                        tint = TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 알림 제목 (단체방 이름 또는 발신자 이름)
            if (notification.title.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val isGroup = notification.isGroupChat

                    if (isGroup) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = EdgeCyan.copy(alpha = 0.18f),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, EdgeCyan.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = stringResource(R.string.panel_group_chat),
                                color = EdgeCyan,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(5.dp))
                    }
                    Text(
                        text = notificationLabelText(notification.titleLabel, notification.title),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 대화형 메신저 카드 판별: 답장 액션이 있거나 대화 내역이 2건 이상 누적된 경우
            val hasConversationalHistory = notification.messages.size > 1 || (notification.messages.isNotEmpty() && replyAction != null)

            // 알림 본문 또는 대화 내역 (과거 내역 확장 지원)
            if (hasConversationalHistory) {
                Spacer(modifier = Modifier.height(4.dp))
                val displayMessages = if (isExpandedMessages) {
                    notification.messages
                } else {
                    notification.messages.takeLast(3)
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Graphite900)
                        .clickable(onClick = onClick)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (notification.messages.size > 3) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isExpandedMessages = !isExpandedMessages }
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isExpandedMessages) {
                                    stringResource(R.string.panel_collapse_messages)
                                } else {
                                    pluralStringResource(
                                        R.plurals.panel_older_messages,
                                        notification.messages.size - 3,
                                        notification.messages.size - 3
                                    )
                                },
                                color = EdgeCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = pluralStringResource(
                                    R.plurals.panel_message_total,
                                    notification.messages.size,
                                    notification.messages.size
                                ),
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // 그룹 단체방 판별 (파서의 isGroupChat 결과 사용) 및 복수 발신자/종목 혼합 판별
                    val isGroupChat = notification.isGroupChat
                    val distinctOtherSenders = notification.messages
                        .filter { !it.isFromUser && it.sender != "나" && it.sender.isNotBlank() }
                        .map { it.sender }
                        .distinct()
                    val hasMultipleSenders = distinctOtherSenders.size > 1

                    displayMessages.forEach { msg ->
                        val isMine = msg.isFromUser || msg.sender == "나"
                        val displayMsgText = remember(msg.text, msg.sender, notification.title) {
                            com.devdooly.notificationedge.util.NotificationTextCleaner.cleanMessageText(
                                msg.text,
                                notification.title,
                                msg.sender
                            )
                        }
                        // 단체방이거나 여러 발신자/종목이 섞인 알림이면 일관되게 모든 항목에 발신자 라벨(종목명/발신자:) 표시
                        val isSameAsTitle = msg.sender.equals(notification.title, ignoreCase = true) || notification.title.contains(msg.sender, ignoreCase = true)
                        val shouldShowSenderLabel = !isMine && msg.sender.isNotBlank() && (isGroupChat || hasMultipleSenders || !isSameAsTitle)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 1.5.dp),
                            horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            val msgTime = remember(msg.timestamp, notification.timestamp, locale, is24Hour) {
                                formatMessageTime(
                                    if (msg.timestamp > 0) msg.timestamp else notification.timestamp,
                                    locale,
                                    is24Hour
                                )
                            }

                            if (!isMine) {
                                Row(
                                    modifier = Modifier.weight(1f, fill = false),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    if (shouldShowSenderLabel) {
                                        Text(
                                            text = "${notificationLabelText(msg.senderLabel, msg.sender)}: ",
                                            color = EdgeCyan,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Text(
                                        text = displayMsgText,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        maxLines = if (isExpandedMessages) 6 else 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (msgTime.isNotBlank()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = msgTime,
                                        color = TextSecondary,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Normal,
                                        modifier = Modifier.padding(bottom = 1.dp)
                                    )
                                }
                            } else {
                                if (msgTime.isNotBlank()) {
                                    Text(
                                        text = msgTime,
                                        color = TextSecondary,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Normal,
                                        modifier = Modifier.padding(bottom = 1.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Surface(
                                    shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, bottomEnd = 8.dp, topEnd = 2.dp),
                                    color = EdgeCyan.copy(alpha = 0.2f),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, EdgeCyan.copy(alpha = 0.5f))
                                ) {
                                    Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) {
                                        Text(
                                            text = stringResource(R.string.panel_me_prefix),
                                            color = EdgeCyan,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = displayMsgText,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            maxLines = if (isExpandedMessages) 6 else 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // 단방향 정보성/서비스 알림 (구글 Gemini 상태 알림, 시스템 알림 등)
                val rawBody = if (notification.text.isNotBlank()) notification.text else notification.messages.firstOrNull()?.text ?: ""
                if (rawBody.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    val displayText = remember(rawBody, notification.title) {
                        com.devdooly.notificationedge.util.NotificationTextCleaner.cleanMessageText(
                            rawBody,
                            notification.title
                        )
                    }
                    val notifTime = remember(notification.timestamp, locale, is24Hour) {
                        formatMessageTime(notification.timestamp, locale, is24Hour)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = displayText,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            lineHeight = 17.sp,
                            maxLines = 6,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (notifTime.isNotBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = notifTime,
                                color = TextSecondary,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(bottom = 1.dp)
                            )
                        }
                    }
                }
            }

            // 하단 액션 버튼 영역 (빠른 답장 + 디버그 데이터 복사)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (replyAction != null) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (isReplyActive) ActionBlue else DarkSurfaceVariant)
                            .clickable { onToggleReply(!isReplyActive) }
                            .heightIn(min = 48.dp)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Reply,
                            contentDescription = stringResource(R.string.panel_reply),
                            tint = if (isReplyActive) Color.Black else EdgeCyan,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(if (isReplyActive) R.string.panel_reply_active else R.string.panel_reply),
                            color = if (isReplyActive) Color.Black else EdgeCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                // 개인정보를 마스킹한 진단 데이터 복사 버튼
                if (!notification.debugExtrasDump.isNullOrBlank()) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF2C2C2C))
                            .clickable {
                                com.devdooly.notificationedge.util.SecureClipboard.copySensitive(
                                    context,
                                    context.getString(R.string.panel_diagnostics_clipboard_label),
                                    notification.debugExtrasDump.orEmpty()
                                )
                                android.widget.Toast.makeText(context, R.string.panel_diagnostics_copied, android.widget.Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 7.dp, vertical = 3.5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = stringResource(R.string.panel_copy_data),
                            tint = TextMuted,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.panel_copy_data),
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun EmptyNotificationView(
    onOpenSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 60.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.NotificationsNone,
                contentDescription = null,
                tint = TextMuted.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.panel_empty_notifications),
                color = TextMuted,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2C)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = EdgeCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.panel_open_settings), color = Color.White, fontSize = 12.sp)
            }
        }
    }
}

/**
 * 리소스 언어 및 기기의 12/24시간 설정에 맞춰 수신 시각을 표시한다.
 */
internal fun formatMessageTime(
    timestamp: Long,
    locale: Locale,
    is24Hour: Boolean,
    now: Long = System.currentTimeMillis()
): String {
    if (timestamp <= 0) return ""
    val calNow = java.util.Calendar.getInstance().apply { timeInMillis = now }
    val calMsg = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }

    val isToday = calNow.get(java.util.Calendar.YEAR) == calMsg.get(java.util.Calendar.YEAR) &&
            calNow.get(java.util.Calendar.DAY_OF_YEAR) == calMsg.get(java.util.Calendar.DAY_OF_YEAR)

    val skeleton = (if (isToday) "" else "Md") + if (is24Hour) "Hm" else "hm"
    val pattern = DateFormat.getBestDateTimePattern(locale, skeleton)
    return java.text.SimpleDateFormat(pattern, locale).format(java.util.Date(timestamp))
}

/** 앱 언어가 기기 기본 언어와 달라도 상대 시각에 같은 언어를 사용한다. */
internal fun formatPanelRelativeTime(
    context: Context,
    timestamp: Long,
    now: Long = System.currentTimeMillis()
): String {
    if (timestamp <= 0) return ""
    val elapsed = kotlin.math.abs(now - timestamp)
    if (elapsed < DateUtils.MINUTE_IN_MILLIS) return context.getString(R.string.panel_just_now)
    val locale = context.resources.configuration.locales[0]
    if (elapsed >= DateUtils.WEEK_IN_MILLIS) {
        return java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT, locale)
            .format(java.util.Date(timestamp))
    }
    val (duration, unit) = when {
        elapsed < DateUtils.HOUR_IN_MILLIS -> DateUtils.MINUTE_IN_MILLIS to RelativeDateTimeFormatter.RelativeUnit.MINUTES
        elapsed < DateUtils.DAY_IN_MILLIS -> DateUtils.HOUR_IN_MILLIS to RelativeDateTimeFormatter.RelativeUnit.HOURS
        else -> DateUtils.DAY_IN_MILLIS to RelativeDateTimeFormatter.RelativeUnit.DAYS
    }
    val direction = if (timestamp > now) RelativeDateTimeFormatter.Direction.NEXT else RelativeDateTimeFormatter.Direction.LAST
    return RelativeDateTimeFormatter.getInstance(locale).format((elapsed / duration).toDouble(), direction, unit)
}
