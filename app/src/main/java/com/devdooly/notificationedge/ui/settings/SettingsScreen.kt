package com.devdooly.notificationedge.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devdooly.notificationedge.BuildConfig
import com.devdooly.notificationedge.ui.theme.DarkBackground
import com.devdooly.notificationedge.ui.theme.EdgeCyan
import com.devdooly.notificationedge.util.AppLog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()

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
                                text = "v${BuildConfig.VERSION_NAME}",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "master-switch") {
                MasterSwitchCard(
                    enabled = settings.isServiceEnabled,
                    onCheckedChange = viewModel::updateServiceEnabled
                )
            }

            item(key = "permissions") {
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
                                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                AppLog.warning("SettingsScreen", "배터리 최적화 설정 화면 열기 실패", e)
                            }
                        }
                    }
                )
            }

            item(key = "good-lock") {
                GoodLockIntegrationCard(
                    launchDirectToPanel = settings.launchDirectToPanel,
                    onToggleLaunchDirect = viewModel::updateLaunchDirectToPanel,
                    externalControlEnabled = settings.externalControlEnabled,
                    onToggleExternalControl = viewModel::updateExternalControlEnabled,
                    onTestOpenPanel = {
                        val intent = Intent(context, com.devdooly.notificationedge.ui.OpenPanelActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                )
            }

            item(key = "edge-handle") {
                EdgeHandleSettingsCard(
                    settings = settings,
                    onSideChange = viewModel::updateEdgeSide,
                    onPositionChange = viewModel::updateHandlePositionRatio,
                    onWidthChange = viewModel::updateHandleWidthDp,
                    onHeightChange = viewModel::updateHandleHeightDp,
                    onPanelWidthChange = viewModel::updatePanelWidthDp,
                    onAutoDismissToggle = viewModel::updateAutoDismissOnOpen,
                    onColorChange = viewModel::updateHandleColor,
                    onAlphaChange = viewModel::updateHandleAlpha,
                    onVisibleToggle = viewModel::updateHandleVisible
                )
            }

            item(key = "edge-lighting") {
                EdgeLightingSettingsCard(
                    settings = settings,
                    onLightingToggle = viewModel::updateEdgeLightingEnabled,
                    onColorChange = viewModel::updateEdgeLightingColor,
                    onCornerRadiusChange = viewModel::updateEdgeLightingCornerRadiusDp,
                    onTestTrigger = viewModel::emitTestNotification
                )
            }

            item(key = "font") {
                FontSettingsCard(
                    selectedFontId = settings.selectedFont,
                    onFontSelected = viewModel::updateSelectedFont
                )
            }

            item(key = "behavior") {
                BehaviorSettingsCard(
                    settings = settings,
                    onPauseMediaOnOpenChange = viewModel::updatePauseMediaOnOpen,
                    onHapticFeedbackChange = viewModel::updateHapticEnabled
                )
            }

            item(key = "notification-filter") {
                NotificationFilterSettingsCard(
                    discoveredPackages = settings.discoveredAppPackages,
                    excludedPackages = settings.excludedPackages,
                    blockedKeywords = settings.blockedKeywords,
                    onToggleExcludedPackage = viewModel::setPackageExcluded,
                    onClearDiscoveredPackages = viewModel::clearDiscoveredPackages,
                    onAddBlockedKeyword = viewModel::addBlockedKeyword,
                    onRemoveBlockedKeyword = viewModel::removeBlockedKeyword
                )
            }

            item(key = "diagnostics") {
                NotificationDebugDumpCard(
                    enabled = settings.diagnosticModeEnabled,
                    onEnabledChange = viewModel::updateDiagnosticModeEnabled
                )
            }

            item(key = "app-update") {
                AppUpdateCard(currentVersionName = BuildConfig.VERSION_NAME)
            }

            item(key = "app-info") {
                AppInfoCard()
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
