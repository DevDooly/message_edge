package com.devdooly.notificationedge.ui.overlay

import android.content.Context
import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.devdooly.notificationedge.data.model.EdgeNotification
import com.devdooly.notificationedge.data.model.EdgeSide
import com.devdooly.notificationedge.data.model.NotificationActionItem
import com.devdooly.notificationedge.data.repository.NotificationRepository
import com.devdooly.notificationedge.ui.theme.DarkCardBackground
import com.devdooly.notificationedge.ui.theme.EdgeCyan
import com.devdooly.notificationedge.ui.theme.GlassBackground
import com.devdooly.notificationedge.ui.theme.GlassBorder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EdgePanelContent(
    edgeSide: EdgeSide,
    panelWidthDp: Int = 280,
    autoDismissOnOpen: Boolean = true,
    onClose: () -> Unit,
    onOpenSettings: () -> Unit,
    onRequestFocus: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val notifications by NotificationRepository.notifications.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var dragOffsetX by remember { mutableFloatStateOf(0f) }

    // 현재 답장 입력창이 열려있는 알림의 Key 및 입력 텍스트
    var activeReplyKey by remember { mutableStateOf<String?>(null) }
    var replyText by remember { mutableStateOf("") }
    val replyFocusRequester = remember { FocusRequester() }

    val activeNotification = remember(activeReplyKey, notifications) {
        notifications.firstOrNull { it.key == activeReplyKey }
    }
    val activeReplyAction = remember(activeNotification) {
        activeNotification?.actions?.firstOrNull { it.isReply }
    }

    // 답장 활성화 시 윈도우 포커스 부여 후 키보드 팝업, 비활성화 시 윈도우 포커스 해제(유튜브 PiP 방지 유지)
    LaunchedEffect(activeReplyKey) {
        if (activeReplyKey != null) {
            onRequestFocus(true)
            delay(120)
            replyFocusRequester.requestFocus()
            keyboardController?.show()
        } else {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
            onRequestFocus(false)
            replyText = ""
        }
    }

    // 네비게이션 뒤로가기 제스처 / 버튼 처리 (키보드 및 답장 모드 먼저 닫기)
    BackHandler(enabled = true) {
        if (activeReplyKey != null) {
            // 1단계: 답장창/키보드가 열려있으면 포커스 해제 및 가상키보드 닫기
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
            activeReplyKey = null
        } else {
            // 2단계: 기본 상태에서는 엣지 패널 전체 닫기
            onClose()
        }
    }

    val rootFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        try {
            rootFocusRequester.requestFocus()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 전체 화면 배경 (완전 투명, 바깥 터치 및 왼쪽/오른쪽 드래그 시 닫기)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(rootFocusRequester)
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                if ((keyEvent.key == Key.Back || keyEvent.key == Key.Escape) && keyEvent.type == KeyEventType.KeyUp) {
                    if (activeReplyKey != null) {
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                        activeReplyKey = null
                    } else {
                        onClose()
                    }
                    true
                } else false
            }
            .background(Color.Transparent)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { dragOffsetX = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        dragOffsetX += dragAmount
                        if (dragOffsetX < -40f || dragOffsetX > 40f) {
                            focusManager.clearFocus(force = true)
                            keyboardController?.hide()
                            onClose()
                        }
                    },
                    onDragEnd = {
                        if (kotlin.math.abs(dragOffsetX) > 30f) {
                            focusManager.clearFocus(force = true)
                            keyboardController?.hide()
                            onClose()
                        }
                        dragOffsetX = 0f
                    }
                )
            }
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = {
                    focusManager.clearFocus(force = true)
                    keyboardController?.hide()
                    onClose()
                }
            )
    ) {
        // 사이드 슬라이드 패널 (상태바의 배터리/시계/알림 정보 및 네비바를 가리지 않도록 인셋 패딩 적용 및 키보드 imePadding 연동)
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(vertical = 6.dp)
                .width(panelWidthDp.dp)
                .align(if (edgeSide == EdgeSide.RIGHT) Alignment.CenterEnd else Alignment.CenterStart)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragOffsetX = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            dragOffsetX += dragAmount
                            if (dragOffsetX < -40f || dragOffsetX > 40f) {
                                onClose()
                            }
                        },
                        onDragEnd = {
                            if (kotlin.math.abs(dragOffsetX) > 30f) {
                                onClose()
                            }
                            dragOffsetX = 0f
                        }
                    )
                }
                .clickable(enabled = false) {}, // 클릭 전파 방지
            color = GlassBackground,
            shape = if (edgeSide == EdgeSide.RIGHT) {
                RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 4.dp, bottomEnd = 4.dp)
            } else {
                RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp, topStart = 4.dp, bottomStart = 4.dp)
            },
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 16.dp, horizontal = 14.dp)
            ) {
                // 상단 헤더
                PanelHeader(
                    notificationCount = notifications.size,
                    onClearAll = { NotificationRepository.clearAll() },
                    onOpenSettings = onOpenSettings,
                    onClose = onClose
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 알림 리스트 또는 빈 상태
                if (notifications.isEmpty()) {
                    EmptyNotificationView(onOpenSettings = onOpenSettings)
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        items(
                            items = notifications,
                            key = { it.key }
                        ) { notification ->
                            NotificationCard(
                                notification = notification,
                                isReplyActive = (activeReplyKey == notification.key),
                                onToggleReply = { open ->
                                    activeReplyKey = if (open) notification.key else null
                                    if (open) {
                                        val index = notifications.indexOfFirst { it.key == notification.key }
                                        if (index >= 0) {
                                            coroutineScope.launch {
                                                listState.animateScrollToItem(index)
                                            }
                                        }
                                    }
                                },
                                onDismiss = { NotificationRepository.dismissNotification(notification.key) },
                                onClick = {
                                    NotificationRepository.openNotificationApp(
                                        context = context,
                                        notification = notification,
                                        autoDismiss = autoDismissOnOpen,
                                        onClose = onClose
                                    )
                                }
                            )
                        }
                    }
                }

                // 가상 키보드 바로 상단에 착 달라붙는 플로팅 답장 바
                if (activeNotification != null && activeReplyAction != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    KeyboardFloatingReplyBar(
                        modifier = Modifier.fillMaxWidth(),
                        targetName = activeNotification.title.ifBlank { activeNotification.appName },
                        replyText = replyText,
                        focusRequester = replyFocusRequester,
                        onTextChange = { replyText = it },
                        onSend = {
                            if (replyText.isNotBlank()) {
                                NotificationRepository.sendQuickReply(
                                    context = context,
                                    notificationKey = activeNotification.key,
                                    action = activeReplyAction,
                                    replyText = replyText
                                )
                                replyText = ""
                                focusManager.clearFocus(force = true)
                                keyboardController?.hide()
                                activeReplyKey = null
                            }
                        },
                        onClose = {
                            replyText = ""
                            focusManager.clearFocus(force = true)
                            keyboardController?.hide()
                            activeReplyKey = null
                        }
                    )
                }
            }
        }
    }
}

/**
 * 가상 키보드 상단에 표시되는 전송 및 입력 바 (엄지손가락 접근성 최적화)
 */
@Composable
private fun KeyboardFloatingReplyBar(
    modifier: Modifier = Modifier,
    targetName: String,
    replyText: String,
    focusRequester: FocusRequester,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xF0181818),
        border = androidx.compose.foundation.BorderStroke(1.dp, EdgeCyan.copy(alpha = 0.5f)),
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            // 상단 답장 대상 안내 및 닫기 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Reply,
                        contentDescription = null,
                        tint = EdgeCyan,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "답장: $targetName",
                        color = EdgeCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "닫기",
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 입력 필드 + 바로 전송 버튼 (키보드 바로 위에서 엄지손가락으로 즉시 탭 가능)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF252525))
                        .border(0.8.dp, Color(0xFF3A3A3A), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (replyText.isEmpty()) {
                        Text(
                            text = "메시지 보내기...",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                    BasicTextField(
                        value = replyText,
                        onValueChange = onTextChange,
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        cursorBrush = SolidColor(EdgeCyan),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { onSend() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        singleLine = true
                    )
                }

                // 키보드 상단 전송 버튼 (메시지 보내기 버튼)
                Button(
                    onClick = onSend,
                    enabled = replyText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EdgeCyan,
                        disabledContainerColor = EdgeCyan.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "전송",
                        tint = if (replyText.isNotBlank()) Color.Black else Color.DarkGray,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "전송",
                        color = if (replyText.isNotBlank()) Color.Black else Color.DarkGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun PanelHeader(
    notificationCount: Int,
    onClearAll: () -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "알림 엣지",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
            if (notificationCount > 0) {
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = CircleShape,
                    color = EdgeCyan.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EdgeCyan)
                ) {
                    Text(
                        text = "$notificationCount",
                        color = EdgeCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // 설정 화면 열기 버튼
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "설정",
                    tint = Color.LightGray,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (notificationCount > 0) {
                Spacer(modifier = Modifier.width(2.dp))
                IconButton(
                    onClick = onClearAll,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ClearAll,
                        contentDescription = "모두 지우기",
                        tint = Color.LightGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(2.dp))

            IconButton(
                onClick = onClose,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "닫기",
                    tint = Color.LightGray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun NotificationCard(
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

                if (notification.isDismissed) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0x33888888)
                    ) {
                        Text(
                            text = "보관됨",
                            color = Color.LightGray,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
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
                    val isGroup = notification.isGroupChat ||
                            notification.subText != null ||
                            notification.title.contains("(") ||
                            notification.title.contains(",") ||
                            notification.messages.any { it.sender.isNotBlank() && it.sender != notification.title }

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

            // 알림 본문 또는 대화 내역 (과거 내역 확장 지원)
            if (notification.messages.isNotEmpty()) {
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

                    // 그룹 단체방 판별
                    val isGroupChat = remember(notification.isGroupChat, notification.title, notification.messages) {
                        notification.isGroupChat ||
                                notification.subText != null ||
                                notification.title.contains("(") ||
                                notification.title.contains(",") ||
                                notification.messages.map { it.sender.trim() }.filter { it.isNotBlank() && it != "나" }.distinct().size > 1
                    }

                    displayMessages.forEach { msg ->
                        val isMine = msg.isFromUser || msg.sender == "나"
                        val displayMsgText = remember(msg.text, msg.sender, notification.title) {
                            com.devdooly.notificationedge.util.NotificationTextCleaner.cleanMessageText(
                                msg.text,
                                notification.title,
                                msg.sender
                            )
                        }
                        // 단체방이면 무조건 보낸사람 표시, 1:1 대화에서는 타이틀과 다를 때 표시
                        val shouldShowSenderLabel = !isMine && msg.sender.isNotBlank() && (isGroupChat || msg.sender.trim() != notification.title.trim())

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
            } else if (notification.text.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                val displayText = remember(notification.text, notification.title) {
                    com.devdooly.notificationedge.util.NotificationTextCleaner.cleanMessageText(
                        notification.text,
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

                // 디버그 데이터 복사 버튼 (단체방/알림 원본 분석용)
                if (!notification.debugExtrasDump.isNullOrBlank()) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF2C2C2C))
                            .clickable {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Notification Debug Dump", notification.debugExtrasDump)
                                clipboard?.setPrimaryClip(clip)
                                android.widget.Toast.makeText(context, "알림 원본 데이터가 복사되었습니다! 채팅에 붙여넣어주세요.", android.widget.Toast.LENGTH_SHORT).show()
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
private fun EmptyNotificationView(
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
