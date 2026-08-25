package com.devdooly.notificationedge.ui.settings

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devdooly.notificationedge.data.model.AppSettings
import com.devdooly.notificationedge.data.model.EdgeNotification
import com.devdooly.notificationedge.data.model.EdgeSide
import com.devdooly.notificationedge.data.repository.NotificationRepository
import com.devdooly.notificationedge.data.repository.SettingsRepository
import com.devdooly.notificationedge.service.EdgeOverlayService
import com.devdooly.notificationedge.service.NotificationListener
import com.devdooly.notificationedge.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsRepo = remember { SettingsRepository(context) }
    val settings by settingsRepo.settingsFlow.collectAsState(initial = AppSettings())

    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var hasNotificationPermission by remember { mutableStateOf(isNotificationServiceEnabled(context)) }
    var isIgnoringBatteryOptimizations by remember { mutableStateOf(isBatteryOptimized(context)) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = Settings.canDrawOverlays(context)
                hasNotificationPermission = isNotificationServiceEnabled(context)
                isIgnoringBatteryOptimizations = isBatteryOptimized(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Notification Edge", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = EdgeCyan.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, EdgeCyan)
                        ) {
                            Text(
                                text = "v1.2.7",
                                color = EdgeCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 마스터 토글
            MasterSwitchCard(
                enabled = settings.isServiceEnabled,
                onCheckedChange = { enabled ->
                    scope.launch {
                        settingsRepo.updateServiceEnabled(enabled)
                        if (enabled && hasOverlayPermission) {
                            startOverlayService(context)
                        }
                    }
                }
            )

            // 권한 가이드 카드
            PermissionStatusCard(
                hasOverlay = hasOverlayPermission,
                hasNotification = hasNotificationPermission,
                hasBatteryOpt = isIgnoringBatteryOptimizations,
                onGrantOverlay = {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                },
                onGrantNotification = {
                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    context.startActivity(intent)
                },
                onGrantBattery = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        try {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            try {
                                val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                context.startActivity(fallbackIntent)
                            } catch (e2: Exception) {
                                e2.printStackTrace()
                            }
                        }
                    }
                }
            )

            // 삼성 Good Lock 연동 가이드 카드
            GoodLockIntegrationCard(
                launchDirectToPanel = settings.launchDirectToPanel,
                onToggleLaunchDirect = { scope.launch { settingsRepo.updateLaunchDirectToPanel(it) } },
                onTestOpenPanel = {
                    val intent = Intent(context, com.devdooly.notificationedge.ui.OpenPanelActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            )

            // 엣지 핸들 및 패널 설정
            EdgeHandleSettingsCard(
                settings = settings,
                onSideChange = { scope.launch { settingsRepo.updateEdgeSide(it) } },
                onPositionChange = { scope.launch { settingsRepo.updateHandlePositionRatio(it) } },
                onWidthChange = { scope.launch { settingsRepo.updateHandleWidthDp(it) } },
                onHeightChange = { scope.launch { settingsRepo.updateHandleHeightDp(it) } },
                onPanelWidthChange = { scope.launch { settingsRepo.updatePanelWidthDp(it) } },
                onAutoDismissToggle = { scope.launch { settingsRepo.updateAutoDismissOnOpen(it) } },
                onColorChange = { scope.launch { settingsRepo.updateHandleColor(it) } },
                onAlphaChange = { scope.launch { settingsRepo.updateHandleAlpha(it) } },
                onVisibleToggle = { scope.launch { settingsRepo.updateHandleVisible(it) } }
            )

            // 엣지 라이팅 설정
            EdgeLightingSettingsCard(
                settings = settings,
                onLightingToggle = { scope.launch { settingsRepo.updateEdgeLightingEnabled(it) } },
                onColorChange = { scope.launch { settingsRepo.updateEdgeLightingColor(it) } },
                onCornerRadiusChange = { scope.launch { settingsRepo.updateEdgeLightingCornerRadiusDp(it) } },
                onTestTrigger = {
                    // 가상 테스트 알림 방출
                    NotificationRepository.addOrUpdateNotification(
                        EdgeNotification(
                            key = "test_notification_${System.currentTimeMillis()}",
                            id = 999,
                            packageName = context.packageName,
                            appName = "메시지",
                            title = "홍길동",
                            text = "안녕하세요! Notification Edge 테스트 알림입니다.",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            )

            // 글꼴 및 폰트 설정 (한영 혼용 가독성 최적화)
            FontSettingsCard(
                selectedFontId = settings.selectedFont,
                onFontSelected = { scope.launch { settingsRepo.updateSelectedFont(it) } }
            )

            // 햅틱 진동 피드백
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("햅틱 진동 피드백", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text("핸들 터치 및 알림 시 진동", color = Color.Gray, fontSize = 12.sp)
                    }
                    Switch(
                        checked = settings.hapticFeedbackEnabled,
                        onCheckedChange = { scope.launch { settingsRepo.updateHapticEnabled(it) } },
                        colors = SwitchDefaults.colors(checkedThumbColor = EdgeCyan)
                    )
                }
            }

            // 인앱 자동 업데이트 확인 및 설치 카드
            AppUpdateCard(currentVersionName = "1.2.7")

            // 앱 버전 및 시스템 정보 카드
            AppInfoCard()
        }
    }
}

@Composable
private fun MasterSwitchCard(
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) EdgeCyan.copy(alpha = 0.15f) else DarkSurface
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (enabled) EdgeCyan.copy(alpha = 0.5f) else Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Notification Edge 서비스",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Text(
                    text = if (enabled) "화면 가장자리에서 활성화됨" else "서비스 비활성화됨",
                    color = if (enabled) EdgeCyan else Color.Gray,
                    fontSize = 13.sp
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = EdgeCyan,
                    checkedTrackColor = EdgeCyan.copy(alpha = 0.3f)
                )
            )
        }
    }
}

@Composable
private fun GoodLockIntegrationCard(
    launchDirectToPanel: Boolean,
    onToggleLaunchDirect: (Boolean) -> Unit,
    onTestOpenPanel: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, EdgeCyan.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = null,
                    tint = EdgeCyan,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "삼성 Good Lock (제스처) 연동",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "One Hand Operation +의 제스처에 '알림 엣지(Notification Edge)'를 등록하면 화면에 핸들을 안 띄우고도 순정처럼 제스처로 알림 패널을 열 수 있습니다.",
                color = Color.LightGray,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 앱 실행 시 알림 엣지 바로 열기 스위치
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF252525))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("앱 실행 시 알림 엣지 바로 열기", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("Good Lock 제스처나 앱 실행 시 설정창 대신 알림 패널만 즉시 엽니다", color = Color.Gray, fontSize = 11.sp)
                }
                Switch(
                    checked = launchDirectToPanel,
                    onCheckedChange = onToggleLaunchDirect,
                    colors = SwitchDefaults.colors(checkedThumbColor = EdgeCyan)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onTestOpenPanel,
                colors = ButtonDefaults.buttonColors(containerColor = EdgeCyan),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(6.dp))
                Text("알림 엣지 즉시 열기 테스트", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AppInfoCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF181818)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF333333))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Notification Edge",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "버전 1.2.7 (Build 27) | Target Android 14",
                color = EdgeCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "패키지: com.devdooly.notificationedge",
                color = Color.Gray,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun AppUpdateCard(currentVersionName: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var updateStatus by remember { mutableStateOf<UpdateUIState>(UpdateUIState.Idle) }
    var releaseInfo by remember { mutableStateOf<com.devdooly.notificationedge.data.updater.ReleaseInfo?>(null) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (updateStatus is UpdateUIState.UpdateAvailable) EdgeCyan else Color(0xFF333333)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = EdgeCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "앱 업데이트",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Text(
                    text = "현재 v$currentVersionName",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            when (val state = updateStatus) {
                is UpdateUIState.Idle -> {
                    Text(
                        text = "GitHub Release의 최신 APK 및 릴리즈 노트를 확인하고 터치 한 번으로 바로 업데이트할 수 있습니다.",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            updateStatus = UpdateUIState.Checking
                            scope.launch {
                                val result = com.devdooly.notificationedge.data.updater.AppUpdateManager.checkForUpdate(currentVersionName)
                                result.onSuccess { info ->
                                    releaseInfo = info
                                    if (info.hasUpdate) {
                                        updateStatus = UpdateUIState.UpdateAvailable(info)
                                    } else {
                                        updateStatus = UpdateUIState.UpToDate(info.tagName)
                                    }
                                }.onFailure { error ->
                                    updateStatus = UpdateUIState.Error(error.message ?: "업데이트 확인 실패")
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2C)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = EdgeCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("최신 업데이트 확인", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                is UpdateUIState.Checking -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = EdgeCyan,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("GitHub Releases 최신 버전 확인 중...", color = Color.LightGray, fontSize = 13.sp)
                    }
                }

                is UpdateUIState.UpToDate -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E332A))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = EdgeGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "현재 최신 버전(${state.latestVersion})을 사용하고 있습니다!",
                            color = Color(0xFFB9F6CA),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(
                        onClick = {
                            updateStatus = UpdateUIState.Checking
                            scope.launch {
                                val result = com.devdooly.notificationedge.data.updater.AppUpdateManager.checkForUpdate(currentVersionName)
                                result.onSuccess { info ->
                                    releaseInfo = info
                                    updateStatus = if (info.hasUpdate) UpdateUIState.UpdateAvailable(info) else UpdateUIState.UpToDate(info.tagName)
                                }.onFailure {
                                    updateStatus = UpdateUIState.Error(it.message ?: "확인 실패")
                                }
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("다시 확인", color = EdgeCyan, fontSize = 12.sp)
                    }
                }

                is UpdateUIState.UpdateAvailable -> {
                    val info = state.info
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = EdgeCyan.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, EdgeCyan)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = EdgeCyan
                                ) {
                                    Text(
                                        text = "NEW ${info.tagName}",
                                        color = Color.Black,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = info.title,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (info.releaseNotes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = info.releaseNotes,
                                    color = Color(0xFFDDDDDD),
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    maxLines = 6
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            updateStatus = UpdateUIState.Downloading
                            downloadProgress = 0f
                            scope.launch {
                                val result = com.devdooly.notificationedge.data.updater.AppUpdateManager.downloadApk(
                                    context = context,
                                    downloadUrl = info.downloadUrl,
                                    onProgress = { downloadProgress = it }
                                )
                                result.onSuccess { apkFile ->
                                    updateStatus = UpdateUIState.Downloaded(apkFile)
                                    com.devdooly.notificationedge.data.updater.AppUpdateManager.installApk(context, apkFile)
                                }.onFailure { error ->
                                    updateStatus = UpdateUIState.Error("다운로드 실패: ${error.message}")
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EdgeCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("지금 다운로드 및 바로 업데이트", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                is UpdateUIState.Downloading -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("GitHub에서 최신 APK 다운로드 중...", color = Color.LightGray, fontSize = 12.sp)
                            Text("${(downloadProgress * 100).toInt()}%", color = EdgeCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            color = EdgeCyan,
                            trackColor = Color(0xFF333333),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    }
                }

                is UpdateUIState.Downloaded -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = EdgeGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("다운로드 완료! 설치 창을 열었습니다.", color = Color.White, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                com.devdooly.notificationedge.data.updater.AppUpdateManager.installApk(context, state.apkFile)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EdgeGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("설치 화면 다시 열기", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                is UpdateUIState.Error -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "오류: ${state.message}",
                            color = Color(0xFFFF5252),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                updateStatus = UpdateUIState.Idle
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("다시 시도", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

private sealed interface UpdateUIState {
    data object Idle : UpdateUIState
    data object Checking : UpdateUIState
    data class UpToDate(val latestVersion: String) : UpdateUIState
    data class UpdateAvailable(val info: com.devdooly.notificationedge.data.updater.ReleaseInfo) : UpdateUIState
    data object Downloading : UpdateUIState
    data class Downloaded(val apkFile: java.io.File) : UpdateUIState
    data class Error(val message: String) : UpdateUIState
}

@Composable
private fun PermissionStatusCard(
    hasOverlay: Boolean,
    hasNotification: Boolean,
    hasBatteryOpt: Boolean,
    onGrantOverlay: () -> Unit,
    onGrantNotification: () -> Unit,
    onGrantBattery: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "필수 권한 설정",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            PermissionItem(
                title = "다른 앱 위에 표시 권한",
                desc = "엣지 핸들 및 알림 패널 오버레이 표시",
                isGranted = hasOverlay,
                onClick = onGrantOverlay
            )
            Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))

            PermissionItem(
                title = "알림 접근 권한",
                desc = "수신되는 앱 알림 감지 및 패널에 표시",
                isGranted = hasNotification,
                onClick = onGrantNotification
            )
            Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))

            PermissionItem(
                title = "배터리 최적화 예외 (선택)",
                desc = "백그라운드에서 상시 안정적 실행 유지",
                isGranted = hasBatteryOpt,
                onClick = onGrantBattery
            )
        }
    }
}

@Composable
private fun PermissionItem(
    title: String,
    desc: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(desc, color = Color.Gray, fontSize = 11.sp)
        }
        if (isGranted) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "허용됨",
                    tint = EdgeGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("허용됨", color = EdgeGreen, fontSize = 12.sp)
            }
        } else {
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = EdgeCyan),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("권한 허용", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EdgeHandleSettingsCard(
    settings: AppSettings,
    onSideChange: (EdgeSide) -> Unit,
    onPositionChange: (Float) -> Unit,
    onWidthChange: (Int) -> Unit,
    onHeightChange: (Int) -> Unit,
    onPanelWidthChange: (Int) -> Unit,
    onAutoDismissToggle: (Boolean) -> Unit,
    onColorChange: (Long) -> Unit,
    onAlphaChange: (Float) -> Unit,
    onVisibleToggle: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "엣지 핸들 및 패널 레이아웃",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(14.dp))

            // 패널 가로 너비 (5dp 단위 조절)
            Text(
                "알림 패널 가로 너비 (${settings.panelWidthDp} dp)",
                color = Color.LightGray,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Slider(
                value = settings.panelWidthDp.toFloat(),
                onValueChange = { onPanelWidthChange(it.toInt()) },
                valueRange = 220f..360f,
                steps = 27,
                colors = SliderDefaults.colors(
                    thumbColor = EdgeCyan,
                    activeTrackColor = EdgeCyan
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 채팅방 이동 시 해당 알림 자동 삭제 (기본값 ON)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("채팅방 이동 시 알림 자동 삭제", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("메시지를 터치해 해당 앱/채팅방으로 이동하면 알림 목록에서 자동으로 삭제합니다", color = Color.Gray, fontSize = 11.sp)
                }
                Switch(
                    checked = settings.autoDismissOnOpen,
                    onCheckedChange = onAutoDismissToggle,
                    colors = SwitchDefaults.colors(checkedThumbColor = EdgeCyan)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 핸들 보이기 / 숨기기 (제스처 전용)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("핸들 바 화면 표시", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("끄면 핸들이 투명해지며 스와이프 터치만 작동합니다 (기본 엣지와 간섭 방지)", color = Color.Gray, fontSize = 11.sp)
                }
                Switch(
                    checked = settings.isHandleVisible,
                    onCheckedChange = onVisibleToggle,
                    colors = SwitchDefaults.colors(checkedThumbColor = EdgeCyan)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 좌 / 우 선택
            Text("핸들 위치 (사이드)", color = Color.LightGray, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onSideChange(EdgeSide.LEFT) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (settings.edgeSide == EdgeSide.LEFT) EdgeCyan else DarkSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "왼쪽 (Left - 추천)",
                        color = if (settings.edgeSide == EdgeSide.LEFT) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                Button(
                    onClick = { onSideChange(EdgeSide.RIGHT) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (settings.edgeSide == EdgeSide.RIGHT) EdgeCyan else DarkSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "오른쪽 (Right)",
                        color = if (settings.edgeSide == EdgeSide.RIGHT) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 상하 위치 (Y 비율)
            Text(
                "상하 위치 조절 (${(settings.handlePositionRatio * 100).toInt()}%)",
                color = Color.LightGray,
                fontSize = 13.sp
            )
            Slider(
                value = settings.handlePositionRatio,
                onValueChange = onPositionChange,
                valueRange = 0.1f..0.9f,
                colors = SliderDefaults.colors(
                    thumbColor = EdgeCyan,
                    activeTrackColor = EdgeCyan
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 핸들 크기 (높이, 5dp 단위 조절)
            Text(
                "핸들 높이 길이 (${settings.handleHeightDp} dp)",
                color = Color.LightGray,
                fontSize = 13.sp
            )
            Slider(
                value = settings.handleHeightDp.toFloat(),
                onValueChange = { onHeightChange(it.toInt()) },
                valueRange = 50f..200f,
                steps = 29,
                colors = SliderDefaults.colors(
                    thumbColor = EdgeCyan,
                    activeTrackColor = EdgeCyan
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 핸들 너비 (두께 조절)
            Text(
                "핸들 가로 너비 / 두께 (${settings.handleWidthDp} dp)",
                color = Color.LightGray,
                fontSize = 13.sp
            )
            Slider(
                value = settings.handleWidthDp.toFloat(),
                onValueChange = { onWidthChange(it.toInt()) },
                valueRange = 4f..30f,
                steps = 25,
                colors = SliderDefaults.colors(
                    thumbColor = EdgeCyan,
                    activeTrackColor = EdgeCyan
                )
            )

            if (settings.isHandleVisible) {
                Spacer(modifier = Modifier.height(8.dp))

                // 투명도
                Text(
                    "핸들 투명도 (${(settings.handleAlpha * 100).toInt()}%)",
                    color = Color.LightGray,
                    fontSize = 13.sp
                )
                Slider(
                    value = settings.handleAlpha,
                    onValueChange = onAlphaChange,
                    valueRange = 0.0f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = EdgeCyan,
                        activeTrackColor = EdgeCyan
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 색상 팔레트
                Text("핸들 색상", color = Color.LightGray, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                ColorPaletteRow(
                    selectedColor = settings.handleColor,
                    onSelectColor = onColorChange
                )
            }
        }
    }
}

@Composable
private fun EdgeLightingSettingsCard(
    settings: AppSettings,
    onLightingToggle: (Boolean) -> Unit,
    onColorChange: (Long) -> Unit,
    onCornerRadiusChange: (Int) -> Unit,
    onTestTrigger: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "엣지 라이팅 (Edge Lighting)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "알림 수신 시 화면 테두리 빛남 효과",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
                Switch(
                    checked = settings.isEdgeLightingEnabled,
                    onCheckedChange = onLightingToggle,
                    colors = SwitchDefaults.colors(checkedThumbColor = EdgeCyan)
                )
            }

            if (settings.isEdgeLightingEnabled) {
                Spacer(modifier = Modifier.height(14.dp))
                Text("라이팅 색상", color = Color.LightGray, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                ColorPaletteRow(
                    selectedColor = settings.edgeLightingColor,
                    onSelectColor = onColorChange
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 모서리 둥글기 (곡률) 조절 (0dp 직각 ~ 50dp 둥근 모서리)
                Text(
                    text = "화면 모서리 곡률 / 둥글기 (${settings.edgeLightingCornerRadiusDp} dp)",
                    color = Color.LightGray,
                    fontSize = 13.sp
                )
                Text(
                    text = if (settings.edgeLightingCornerRadiusDp == 0) "0 dp: 직각 디스플레이 (Galaxy Ultra 등)" else "스마트폰 모서리 곡률에 맞춰 조절하세요",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
                Slider(
                    value = settings.edgeLightingCornerRadiusDp.toFloat(),
                    onValueChange = { onCornerRadiusChange(it.toInt()) },
                    valueRange = 0f..50f,
                    steps = 50,
                    colors = SliderDefaults.colors(
                        thumbColor = EdgeCyan,
                        activeTrackColor = EdgeCyan
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onTestTrigger,
                    colors = ButtonDefaults.buttonColors(containerColor = EdgeCyan.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = EdgeCyan)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("엣지 라이팅 & 알림 동작 테스트", color = EdgeCyan, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ColorPaletteRow(
    selectedColor: Long,
    onSelectColor: (Long) -> Unit
) {
    val colors = listOf(
        0xFF82D8D0, // Aqueous Aqua (Design Master)
        0xFFA9A6EA, // Quiet Periwinkle (Design Master)
        0xFF00E5FF, // Electric Cyan
        0xFF00E676, // Neon Emerald
        0xFFFF4081, // Vivid Pink
        0xFFFFD600, // Amber Yellow
        0xFFF0EEE9  // Cloud Dancer White
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        colors.forEach { colorHex ->
            val isSelected = selectedColor == colorHex
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(colorHex))
                    .clickable { onSelectColor(colorHex) }
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) Color.White else Color.Transparent,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "선택됨",
                        tint = if (colorHex == 0xFFFFFFFF) Color.Black else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FontSettingsCard(
    selectedFontId: String,
    onFontSelected: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val currentFont = AppFont.fromId(selectedFontId)

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "글꼴 및 폰트 설정 (Font)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "현재: ${currentFont.displayName}",
                        color = EdgeCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "접기" else "더보기",
                        tint = Color.LightGray
                    )
                }
            }

            // 한영 혼용 정렬 보정 안내 문구
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = DarkBackground,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, GlassBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.TextFields,
                        contentDescription = null,
                        tint = EdgeCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "한글/영문 혼용 시 수직 기준선(Baseline)과 패딩을 보정하여 고르게 표시합니다.",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppFont.entries.forEach { fontOption ->
                        val isSelected = fontOption.id == selectedFontId
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) EdgeCyan.copy(alpha = 0.12f) else DarkBackground,
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) EdgeCyan else Color(0xFF333B4A)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onFontSelected(fontOption.id) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = fontOption.displayName,
                                        color = if (isSelected) EdgeCyan else Color.White,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = fontOption.description,
                                        color = Color.Gray,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    // 폰트 실시간 적용 미리보기 샘플
                                    Text(
                                        text = "Notification 알림 123 (Aa 한글 폰트)",
                                        color = if (isSelected) CloudDancer else Color.LightGray,
                                        fontSize = 12.sp,
                                        fontFamily = fontOption.toFontFamily()
                                    )
                                }
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onFontSelected(fontOption.id) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = EdgeCyan,
                                        unselectedColor = Color.Gray
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun isNotificationServiceEnabled(context: Context): Boolean {
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return flat != null && flat.contains(context.packageName)
}

private fun isBatteryOptimized(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }
    return true
}

private fun startOverlayService(context: Context) {
    val intent = Intent(context, EdgeOverlayService::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}
