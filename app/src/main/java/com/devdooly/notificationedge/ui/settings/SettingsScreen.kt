package com.devdooly.notificationedge.ui.settings

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import com.devdooly.notificationedge.data.model.AppSettings
import com.devdooly.notificationedge.data.model.EdgeNotification
import com.devdooly.notificationedge.data.model.EdgeSide
import com.devdooly.notificationedge.data.repository.NotificationRepository
import com.devdooly.notificationedge.data.repository.SettingsRepository
import com.devdooly.notificationedge.service.EdgeOverlayService
import com.devdooly.notificationedge.service.NotificationListener
import com.devdooly.notificationedge.ui.theme.*
import com.devdooly.notificationedge.util.CustomFontInfo
import com.devdooly.notificationedge.util.CustomFontManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsRepo = remember { SettingsRepository.getInstance(context) }
    val settings by settingsRepo.settingsFlow.collectAsStateWithLifecycle(initialValue = AppSettings())

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
                                text = "v1.3.13",
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
            // 마스터 토글 (화면 가장자리 엣지 핸들)
            MasterSwitchCard(
                enabled = settings.isServiceEnabled,
                onCheckedChange = { enabled ->
                    scope.launch {
                        settingsRepo.updateServiceEnabled(enabled)
                        if ((enabled || settings.isEdgeLightingEnabled) && hasOverlayPermission) {
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
                            e.printStackTrace()
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

            // 엣지 패널 및 핸들 외형 설정
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

            // 엣지 라이팅 설정 (핸들 제스처 활성화 여부와 무관하게 독립 작동)
            EdgeLightingSettingsCard(
                settings = settings,
                onLightingToggle = { 
                    scope.launch { 
                        settingsRepo.updateEdgeLightingEnabled(it)
                        if ((it || settings.isServiceEnabled) && hasOverlayPermission) {
                            startOverlayService(context)
                        }
                    } 
                },
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
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("패널 열릴 때 유튜브 일시 정지", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text("엣지 패널을 열 때 유튜브 영상을 자동으로 일시 정지합니다 (끄면 영상과 소리가 멈춤 없이 계속 재생됩니다)", color = Color.Gray, fontSize = 12.sp)
                        }
                        Switch(
                            checked = settings.pauseMediaOnOpen,
                            onCheckedChange = { scope.launch { settingsRepo.updatePauseMediaOnOpen(it) } },
                            colors = SwitchDefaults.colors(checkedThumbColor = EdgeCyan)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
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
            }

            // 알림 필터링 & 제외 관리 (수신된 앱별 제외 및 특정 키워드 차단)
            NotificationFilterSettingsCard(
                discoveredPackages = settings.discoveredAppPackages,
                excludedPackages = settings.excludedPackages,
                blockedKeywords = settings.blockedKeywords,
                onToggleExcludedPackage = { pkg, isExcluded ->
                    scope.launch { settingsRepo.setPackageExcluded(pkg, isExcluded) }
                },
                onClearDiscoveredPackages = {
                    scope.launch { settingsRepo.clearDiscoveredPackages() }
                },
                onAddBlockedKeyword = { kw ->
                    scope.launch { settingsRepo.addBlockedKeyword(kw) }
                },
                onRemoveBlockedKeyword = { kw ->
                    scope.launch { settingsRepo.removeBlockedKeyword(kw) }
                }
            )

            // 알림 원본 데이터(Bundle Extras) 실시간 덤프 및 복사 카드 (단체방/메신저 분석용)
            NotificationDebugDumpCard()

            // 인앱 자동 업데이트 확인 및 설치 카드
            AppUpdateCard(currentVersionName = "1.3.13")

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
                    text = "화면 가장자리 엣지 핸들",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Text(
                    text = if (enabled) "화면 가장자리 스와이프로 패널 열기 활성화" else "핸들 비활성화됨 (엣지 라이팅은 독립 작동)",
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
                text = "버전 1.3.13 (Build 143) | Target Android 14",
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
private fun NotificationDebugDumpCard() {
    val context = LocalContext.current
    val notifications by com.devdooly.notificationedge.data.repository.NotificationRepository.notifications.collectAsState()
    val dumpableList = remember(notifications) {
        notifications.filter { !it.debugExtrasDump.isNullOrBlank() }.take(10)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = EdgeCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "알림 원본 데이터 인스펙터",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                if (dumpableList.isNotEmpty()) {
                    Button(
                        onClick = {
                            val allDumps = dumpableList.joinToString("\n\n") { 
                                "[${it.appName}] ${it.title}\n${it.debugExtrasDump}" 
                            }
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("All Notification Dumps", allDumps)
                            clipboard?.setPrimaryClip(clip)
                            android.widget.Toast.makeText(context, "최근 ${dumpableList.size}개 알림 원본 데이터가 모두 복사되었습니다! 채팅에 붙여넣어주세요.", android.widget.Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EdgeCyan),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("전체 복사", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "카카오톡 단체방 알림이 올 때 알림 내부의 모든 Key-Value 원본 데이터를 확인하고 복사할 수 있습니다. 복사된 내용을 채팅창에 공유해주시면 즉시 완벽한 맞춤 파서를 제작해 드립니다.",
                color = Color.Gray,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            if (dumpableList.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "(아직 수신된 알림이 없습니다. 카카오톡 메시지를 받으신 후 확인해주세요)",
                    color = Color.DarkGray,
                    fontSize = 11.sp
                )
            } else {
                Spacer(modifier = Modifier.height(10.dp))
                dumpableList.take(3).forEach { notif ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E1E1E),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF3A3A3A)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "[${notif.appName}] ${notif.title}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                                Button(
                                    onClick = {
                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("Notification Debug Dump", notif.debugExtrasDump)
                                        clipboard?.setPrimaryClip(clip)
                                        android.widget.Toast.makeText(context, "'${notif.title}' 알림 데이터가 복사되었습니다! 채팅에 붙여넣어주세요.", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Text("복사", color = EdgeCyan, fontSize = 11.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = notif.debugExtrasDump?.take(180) ?: "",
                                color = Color.Gray,
                                fontSize = 10.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                val info = releaseInfo
                                if (info != null) {
                                    updateStatus = UpdateUIState.Downloading
                                    scope.launch {
                                        com.devdooly.notificationedge.data.updater.AppUpdateManager.downloadApk(
                                            context = context,
                                            downloadUrl = info.downloadUrl,
                                            onProgress = { progress ->
                                                downloadProgress = progress
                                            }
                                        ).onSuccess { apkFile ->
                                            updateStatus = UpdateUIState.Downloaded(apkFile)
                                            com.devdooly.notificationedge.data.updater.AppUpdateManager.installApk(context, apkFile)
                                        }.onFailure { error ->
                                            updateStatus = UpdateUIState.Error(error.message ?: "다운로드 실패")
                                        }
                                    }
                                }
                            }
                        ) {
                            Text("최신 APK 직접 재설치", color = Color.Gray, fontSize = 11.sp)
                        }

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
                            }
                        ) {
                            Text("다시 확인", color = EdgeCyan, fontSize = 12.sp)
                        }
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
    val allRequiredGranted = hasOverlay && hasNotification
    val allGranted = hasOverlay && hasNotification && hasBatteryOpt
    var isExpanded by remember(allRequiredGranted) { mutableStateOf(!allRequiredGranted) }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (allGranted) EdgeGreen.copy(alpha = 0.3f) else (if (!allRequiredGranted) Color(0x66FF5252) else GlassBorder)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 헤더 (클릭 시 펼치기/접기)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (allGranted) Icons.Default.VerifiedUser else Icons.Default.Security,
                        contentDescription = null,
                        tint = if (allGranted) EdgeGreen else (if (allRequiredGranted) EdgeCyan else Color(0xFFFF5252)),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "필수 권한 설정",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (allGranted) EdgeGreen.copy(alpha = 0.18f) else (if (allRequiredGranted) EdgeCyan.copy(alpha = 0.18f) else Color(0x33FF5252).copy(alpha = 0.2f)),
                                border = androidx.compose.foundation.BorderStroke(
                                    0.5.dp,
                                    if (allGranted) EdgeGreen else (if (allRequiredGranted) EdgeCyan else Color(0xFFFF5252))
                                )
                            ) {
                                Text(
                                    text = if (allGranted) "모두 허용됨" else (if (allRequiredGranted) "필수 허용됨" else "권한 필요"),
                                    color = if (allGranted) EdgeGreen else (if (allRequiredGranted) EdgeCyan else Color(0xFFFF5252)),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "접기" else "펼치기",
                    tint = Color.Gray,
                    modifier = Modifier.size(22.dp)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))

                    PermissionItem(
                        title = "다른 앱 위에 표시 권한",
                        desc = "엣지 핸들 및 알림 패널 오버레이 표시",
                        isGranted = hasOverlay,
                        onClick = onGrantOverlay
                    )
                    HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))

                    PermissionItem(
                        title = "알림 접근 권한",
                        desc = "수신되는 앱 알림 감지 및 패널에 표시",
                        isGranted = hasNotification,
                        onClick = onGrantNotification
                    )
                    HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))

                    PermissionItem(
                        title = "배터리 최적화 예외 (선택)",
                        desc = "백그라운드에서 상시 안정적 실행 유지",
                        isGranted = hasBatteryOpt,
                        onClick = onGrantBattery
                    )
                }
            }
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
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    var customFonts by remember { mutableStateOf(CustomFontManager.getCustomFonts(context)) }

    // 파일 선택 런처 (.ttf, .otf, .ttc)
    val fontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val result = CustomFontManager.saveCustomFont(context, uri)
            result.onSuccess { fontInfo ->
                customFonts = CustomFontManager.getCustomFonts(context)
                onFontSelected(fontInfo.id)
                Toast.makeText(context, "폰트가 추가되었습니다: ${fontInfo.displayName}", Toast.LENGTH_SHORT).show()
            }.onFailure { error ->
                Toast.makeText(context, "폰트 등록 실패: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val currentDisplayName = if (selectedFontId.startsWith("custom:")) {
        val fileName = selectedFontId.removePrefix("custom:")
        customFonts.find { it.fileName == fileName }?.displayName ?: fileName
    } else {
        AppFont.fromId(selectedFontId).displayName
    }

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
                        text = "현재: $currentDisplayName",
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

                // 폰트 파일 업로드 버튼
                Button(
                    onClick = {
                        fontPickerLauncher.launch(
                            arrayOf(
                                "font/*",
                                "application/x-font-ttf",
                                "application/x-font-opentype",
                                "application/octet-stream",
                                "*/*"
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EdgeCyan.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = EdgeCyan)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "내 기기에서 폰트 파일(.ttf, .otf) 불러오기",
                        color = EdgeCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // 내가 추가한 폰트 목록
                if (customFonts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "내가 추가한 커스텀 폰트 (${customFonts.size})",
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        customFonts.forEach { customFont ->
                            val isSelected = customFont.id == selectedFontId
                            val customFamily = CustomFontManager.loadFontFamily(context, customFont.id) ?: androidx.compose.ui.text.font.FontFamily.Default

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) EdgeCyan.copy(alpha = 0.12f) else DarkBackground,
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isSelected) 1.5.dp else 0.5.dp,
                                    color = if (isSelected) EdgeCyan else Color(0xFF333B4A)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onFontSelected(customFont.id) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = customFont.displayName,
                                            color = if (isSelected) EdgeCyan else Color.White,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = customFont.fileName,
                                            color = Color.Gray,
                                            fontSize = 11.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Notification 알림 123 (Aa 가나다)",
                                            color = if (isSelected) CloudDancer else Color.LightGray,
                                            fontSize = 12.sp,
                                            fontFamily = customFamily
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                CustomFontManager.deleteCustomFont(context, customFont.fileName)
                                                customFonts = CustomFontManager.getCustomFonts(context)
                                                if (selectedFontId == customFont.id) {
                                                    onFontSelected("default")
                                                }
                                                Toast.makeText(context, "폰트가 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "삭제",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { onFontSelected(customFont.id) },
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

                // 기본 제공 폰트 프리셋 목록
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "기본 제공 프리셋 폰트",
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

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

/**
 * 알림 필터링 & 제외 관리 카드 (수신된 앱 목록별 제외 및 특정 키워드 차단)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NotificationFilterSettingsCard(
    discoveredPackages: Set<String>,
    excludedPackages: Set<String>,
    blockedKeywords: Set<String>,
    onToggleExcludedPackage: (String, Boolean) -> Unit,
    onClearDiscoveredPackages: () -> Unit,
    onAddBlockedKeyword: (String) -> Unit,
    onRemoveBlockedKeyword: (String) -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var newKeywordText by remember { mutableStateOf("") }

    // 패키지 매니저를 통해 발견된 앱 정보 로드
    val pm = remember { context.packageManager }
    val discoveredAppList = remember(discoveredPackages) {
        discoveredPackages.map { pkg ->
            val appName = try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                pkg
            }
            val appIcon = try {
                pm.getApplicationIcon(pkg)
            } catch (e: Exception) {
                null
            }
            Triple(pkg, appName, appIcon)
        }.sortedBy { it.second.lowercase() }
    }

    var isExpanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 헤더 (클릭 시 접기/펼치기)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    tint = EdgeCyan,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "알림 필터링 & 제외 관리",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = if (isExpanded) "알림 수신된 앱별 제외 및 특정 키워드 차단" else "수신 앱 ${discoveredAppList.size}개 · 차단 키워드 ${blockedKeywords.size}개",
                        color = if (isExpanded) Color.Gray else EdgeCyan,
                        fontSize = 12.sp
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "접기" else "펼치기",
                    tint = Color.Gray,
                    modifier = Modifier.size(22.dp)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))

                    // ==========================================
                    // 1. 수신된 앱별 알림 제외 관리 섹션
                    // ==========================================
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "수신 기록된 앱 (${discoveredAppList.size}개)",
                            color = EdgeCyan,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        if (discoveredAppList.isNotEmpty()) {
                            TextButton(
                                onClick = onClearDiscoveredPackages,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("기록 비우기", color = Color.Gray, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (discoveredAppList.isEmpty()) {
                        Surface(
                            color = Color(0x33000000),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "아직 수신된 알림이 없습니다. 새로운 알림이 도착하면 여기에 해당 앱이 자동으로 등록되어 간편하게 알림을 끄거나 켤 수 있습니다.",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            discoveredAppList.forEach { (pkg, appName, appIcon) ->
                                val isExcluded = excludedPackages.contains(pkg)
                                val iconBitmap = remember(appIcon) {
                                    try {
                                        appIcon?.toBitmap(72, 72)?.asImageBitmap()
                                    } catch (e: Exception) {
                                        null
                                    }
                                }

                                Surface(
                                    color = if (isExcluded) Color(0x22111111) else Color(0x33282828),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        0.5.dp,
                                        if (isExcluded) Color(0x33FF5252) else Color(0x22FFFFFF)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            if (iconBitmap != null) {
                                                Image(
                                                    bitmap = iconBitmap,
                                                    contentDescription = appName,
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = appName,
                                                    color = if (isExcluded) Color.Gray else Color.White,
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 13.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = pkg,
                                                    color = if (isExcluded) Color(0xFF884444) else Color.DarkGray,
                                                    fontSize = 10.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = if (isExcluded) "알림 제외됨" else "알림 수신",
                                                color = if (isExcluded) Color(0xFFFF6B6B) else EdgeCyan,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Switch(
                                                checked = !isExcluded,
                                                onCheckedChange = { isEnabled ->
                                                    onToggleExcludedPackage(pkg, !isEnabled)
                                                },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = EdgeCyan,
                                                    checkedTrackColor = EdgeCyan.copy(alpha = 0.3f),
                                                    uncheckedThumbColor = Color.Gray,
                                                    uncheckedTrackColor = Color.DarkGray
                                                ),
                                                modifier = Modifier.scale(0.8f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0x22FFFFFF), thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    // ==========================================
                    // 2. 특정 키워드 차단 관리 섹션
                    // ==========================================
                    Text(
                        text = "차단 키워드 (${blockedKeywords.size}개)",
                        color = EdgeCyan,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "제목, 본문, 메시지에 포함 시 알림을 표시하지 않습니다.",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 키워드 입력 필드 + 추가 버튼
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newKeywordText,
                            onValueChange = { newKeywordText = it },
                            placeholder = { Text("차단할 키워드 (예: 광고, 특가, 스팸)", color = Color.Gray, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EdgeCyan,
                                unfocusedBorderColor = Color(0x44FFFFFF),
                                cursorColor = EdgeCyan
                            ),
                            shape = RoundedCornerShape(10.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (newKeywordText.isNotBlank()) {
                                        onAddBlockedKeyword(newKeywordText)
                                        newKeywordText = ""
                                        keyboardController?.hide()
                                    }
                                }
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                if (newKeywordText.isNotBlank()) {
                                    onAddBlockedKeyword(newKeywordText)
                                    newKeywordText = ""
                                    keyboardController?.hide()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EdgeCyan),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Text("추가", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 등록된 키워드 태그(Chip) 목록
                    if (blockedKeywords.isEmpty()) {
                        Text(
                            text = "등록된 차단 키워드가 없습니다.",
                            color = Color.DarkGray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            blockedKeywords.forEach { keyword ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = EdgeCyan.copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, EdgeCyan.copy(alpha = 0.6f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = keyword,
                                            color = EdgeCyan,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(
                                            onClick = { onRemoveBlockedKeyword(keyword) },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "삭제",
                                                tint = EdgeCyan,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
