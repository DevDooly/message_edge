package com.devdooly.notificationedge.ui.overlay

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.devdooly.notificationedge.data.model.EdgeNotification
import com.devdooly.notificationedge.ui.theme.DarkCardBackground
import com.devdooly.notificationedge.ui.theme.EdgeCyan
import com.devdooly.notificationedge.ui.theme.GlassBorder

@Composable
internal fun NotificationCard(
    notification: EdgeNotification,
    isReplyActive: Boolean,
    onToggleReply: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onClick: () -> Unit
) {
    val replyAction = remember(notification.actions) {
        notification.actions.firstOrNull { it.isReply }
    }
    var isExpandedMessages by remember { mutableStateOf(false) }

    val timeString = remember(notification.timestamp) {
        DateUtils.getRelativeTimeSpanString(
            notification.timestamp,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isDismissed) Color(0x991E1E1E) else DarkCardBackground
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            if (isReplyActive) EdgeCyan else (if (notification.isDismissed) Color(0x22FFFFFF) else GlassBorder)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
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
                        color = Color.LightGray,
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
                            text = "› $sub",
                            color = Color(0xFFAAAAAA),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Text(
                    text = timeString,
                    color = Color.Gray,
                    fontSize = 10.sp
                )

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "삭제",
                        tint = Color.Gray,
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
                                text = "단체방",
                                color = EdgeCyan,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(5.dp))
                    }
                    Text(
                        text = notification.title,
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
                        .background(Color(0xFF1E1E1E).copy(alpha = 0.6f))
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
                                text = if (isExpandedMessages) "▲ 최근 대화만 접기" else "▼ 이전 대화 ${notification.messages.size - 3}개 더보기",
                                color = EdgeCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "총 ${notification.messages.size}개",
                                color = Color.Gray,
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
                            val msgTime = remember(msg.timestamp, notification.timestamp) {
                                formatMessageTime(if (msg.timestamp > 0) msg.timestamp else notification.timestamp)
                            }

                            if (!isMine) {
                                Row(
                                    modifier = Modifier.weight(1f, fill = false),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    if (shouldShowSenderLabel) {
                                        Text(
                                            text = "${msg.sender}: ",
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
                                        color = Color(0xFFAAAAAA),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Normal,
                                        modifier = Modifier.padding(bottom = 1.dp)
                                    )
                                }
                            } else {
                                if (msgTime.isNotBlank()) {
                                    Text(
                                        text = msgTime,
                                        color = Color(0xFFAAAAAA),
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
                                            text = "나: ",
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
                    val notifTime = remember(notification.timestamp) {
                        formatMessageTime(notification.timestamp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = displayText,
                            color = Color(0xFFDDDDDD),
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
                                color = Color(0xFFAAAAAA),
                                fontSize = 9.sp,
                                modifier = Modifier.padding(bottom = 1.dp)
                            )
                        }
                    }
                }
            }

            // 하단 액션 버튼 영역 (빠른 답장 + 디버그 데이터 복사)
            val context = androidx.compose.ui.platform.LocalContext.current
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
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isReplyActive) EdgeCyan else EdgeCyan.copy(alpha = 0.15f))
                            .clickable { onToggleReply(!isReplyActive) }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Reply,
                            contentDescription = "답장",
                            tint = if (isReplyActive) Color.Black else EdgeCyan,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isReplyActive) "답장 작성 중..." else "답장",
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
                                    "Notification Debug Dump",
                                    notification.debugExtrasDump.orEmpty()
                                )
                                android.widget.Toast.makeText(context, "알림 진단 데이터가 복사되었습니다.", android.widget.Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 7.dp, vertical = 3.5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "데이터 복사",
                            tint = Color.Gray,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "데이터 복사",
                            color = Color.LightGray,
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
                tint = Color.Gray.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "수신된 알림이 없습니다",
                color = Color.Gray,
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
                Text("설정 열기", color = Color.White, fontSize = 12.sp)
            }
        }
    }
}

/**
 * 메시지 수신 시각을 깔끔한 한국어 12시간제('오후 3:24' 또는 'M/d a h:mm')로 포맷팅
 */
private fun formatMessageTime(timestamp: Long): String {
    if (timestamp <= 0) return ""
    val now = System.currentTimeMillis()

    val calNow = java.util.Calendar.getInstance().apply { timeInMillis = now }
    val calMsg = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }

    val isToday = calNow.get(java.util.Calendar.YEAR) == calMsg.get(java.util.Calendar.YEAR) &&
            calNow.get(java.util.Calendar.DAY_OF_YEAR) == calMsg.get(java.util.Calendar.DAY_OF_YEAR)

    return if (isToday) {
        java.text.SimpleDateFormat("a h:mm", java.util.Locale.KOREAN).format(java.util.Date(timestamp))
    } else {
        java.text.SimpleDateFormat("M/d a h:mm", java.util.Locale.KOREAN).format(java.util.Date(timestamp))
    }
}
