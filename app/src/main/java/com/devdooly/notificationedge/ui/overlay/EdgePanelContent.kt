package com.devdooly.notificationedge.ui.overlay

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.devdooly.notificationedge.data.model.EdgeSide
import com.devdooly.notificationedge.data.repository.NotificationRepository
import com.devdooly.notificationedge.ui.theme.GlassBackground
import com.devdooly.notificationedge.ui.theme.GlassBorder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EdgePanelContent(
    edgeSide: EdgeSide,
    panelWidthDp: Int = 260,
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
    val panelState = remember { EdgePanelUiState() }

    // 사이드 슬라이드 애니메이션 제어
    LaunchedEffect(Unit) {
        panelState.reveal()
    }

    val handleClose: () -> Unit = handleClose@{
        if (panelState.beginClose()) {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
            coroutineScope.launch {
                delay(150)
                onClose()
            }
        }
    }

    val replyFocusRequester = remember { FocusRequester() }

    val activeNotification = remember(panelState.activeReplyKey, notifications) {
        notifications.firstOrNull { it.key == panelState.activeReplyKey }
    }
    val activeReplyAction = remember(activeNotification) {
        activeNotification?.actions?.firstOrNull { it.isReply }
    }

    // 답장 활성화 시 윈도우 포커스 부여 후 키보드 팝업, 비활성화 시 윈도우 포커스 해제(유튜브 PiP 방지 유지)
    LaunchedEffect(panelState.activeReplyKey) {
        if (panelState.activeReplyKey != null) {
            onRequestFocus(true)
            delay(120)
            replyFocusRequester.requestFocus()
            keyboardController?.show()
        } else {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
            onRequestFocus(false)
        }
    }

    // 네비게이션 뒤로가기 제스처 / 버튼 처리 (키보드 및 답장 모드 먼저 닫기)
    BackHandler(enabled = true) {
        if (panelState.activeReplyKey != null) {
            // 1단계: 답장창/키보드가 열려있으면 포커스 해제 및 가상키보드 닫기
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
            panelState.closeReply()
        } else {
            // 2단계: 기본 상태에서는 엣지 패널 슬라이드 닫기
            handleClose()
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
                    if (panelState.activeReplyKey != null) {
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                        panelState.closeReply()
                    } else {
                        handleClose()
                    }
                    true
                } else false
            }
            .background(Color.Transparent)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { panelState.beginDrag() },
                    onHorizontalDrag = { _, dragAmount ->
                        panelState.dragBy(dragAmount)
                        if (panelState.isDragPastThreshold(40f)) {
                            handleClose()
                        }
                    },
                    onDragEnd = {
                        if (panelState.isDragPastThreshold(30f)) {
                            handleClose()
                        }
                        panelState.endDrag()
                    }
                )
            }
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = {
                    handleClose()
                }
            )
    ) {
        // 사이드 슬라이드 패널 (상태바의 배터리/시계/알림 정보 및 네비바를 가리지 않도록 인셋 패딩 적용 및 키보드 imePadding 연동)
        AnimatedVisibility(
            visible = panelState.isVisible,
            enter = slideInHorizontally(
                initialOffsetX = { fullWidth -> if (edgeSide == EdgeSide.RIGHT) fullWidth else -fullWidth },
                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(120)),
            exit = slideOutHorizontally(
                targetOffsetX = { fullWidth -> if (edgeSide == EdgeSide.RIGHT) fullWidth else -fullWidth },
                animationSpec = tween(durationMillis = 140, easing = FastOutLinearInEasing)
            ) + fadeOut(animationSpec = tween(100)),
            modifier = Modifier.align(if (edgeSide == EdgeSide.RIGHT) Alignment.CenterEnd else Alignment.CenterStart)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(vertical = 6.dp)
                    .width(panelWidthDp.dp)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { panelState.beginDrag() },
                            onHorizontalDrag = { _, dragAmount ->
                                panelState.dragBy(dragAmount)
                                if (panelState.isDragPastThreshold(40f)) {
                                    handleClose()
                                }
                            },
                            onDragEnd = {
                                if (panelState.isDragPastThreshold(30f)) {
                                    handleClose()
                                }
                                panelState.endDrag()
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
                        onClose = handleClose
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
                                isReplyActive = (panelState.activeReplyKey == notification.key),
                                onToggleReply = { open ->
                                    if (open) panelState.openReply(notification.key) else panelState.closeReply()
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
                                        onClose = handleClose
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
        }

        // 가상 키보드 가로 길이에 맞춰 화면 전체 폭(Full Width)으로 가상키보드 바로 위에 착 달라붙는 플로팅 답장 바
        if (activeNotification != null && activeReplyAction != null) {
            KeyboardFloatingReplyBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .imePadding()
                    .navigationBarsPadding(),
                targetName = activeNotification.title.ifBlank { activeNotification.appName },
                replyText = panelState.replyText,
                focusRequester = replyFocusRequester,
                onTextChange = panelState::updateReplyText,
                onSend = {
                    if (panelState.replyText.isNotBlank()) {
                        NotificationRepository.sendQuickReply(
                            context = context,
                            notificationKey = activeNotification.key,
                            action = activeReplyAction,
                            replyText = panelState.replyText
                        )
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                        panelState.closeReply()
                    }
                },
                onClose = {
                    focusManager.clearFocus(force = true)
                    keyboardController?.hide()
                    panelState.closeReply()
                }
            )
        }
    }
}
