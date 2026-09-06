package com.devdooly.notificationedge.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devdooly.notificationedge.BuildConfig
import com.devdooly.notificationedge.R
import com.devdooly.notificationedge.data.model.EdgeNotification
import com.devdooly.notificationedge.data.repository.NotificationRepository
import com.devdooly.notificationedge.ui.theme.*
import kotlinx.coroutines.launch
@Composable
internal fun MasterSwitchCard(
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
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = stringResource(R.string.settings_edge_handle_title),
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Text(
                    text = stringResource(if (enabled) R.string.settings_edge_handle_enabled else R.string.settings_edge_handle_disabled),
                    modifier = Modifier.fillMaxWidth(),
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
internal fun GoodLockIntegrationCard(
    launchDirectToPanel: Boolean,
    onToggleLaunchDirect: (Boolean) -> Unit,
    externalControlEnabled: Boolean,
    onToggleExternalControl: (Boolean) -> Unit,
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
                    text = stringResource(R.string.settings_good_lock_title),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.good_lock_integration_description),
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
                    Text(stringResource(R.string.settings_direct_launch_title), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.settings_direct_launch_description), color = Color.Gray, fontSize = 11.sp)
                }
                Switch(
                    checked = launchDirectToPanel,
                    onCheckedChange = onToggleLaunchDirect,
                    colors = SwitchDefaults.colors(checkedThumbColor = EdgeCyan)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
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
                    Text(stringResource(R.string.settings_external_control_title), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.settings_external_control_description), color = Color.Gray, fontSize = 11.sp)
                }
                Switch(
                    checked = externalControlEnabled,
                    onCheckedChange = onToggleExternalControl,
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
                Text(stringResource(R.string.settings_test_open_panel), color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun AppInfoCard() {
    val context = LocalContext.current
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
                text = stringResource(R.string.app_name),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.app_tagline),
                color = Color.LightGray,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.settings_version_details, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, context.applicationInfo.targetSdkVersion),
                color = EdgeCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.settings_package_details, "com.devdooly.notificationedge"),
                color = Color.Gray,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
internal fun NotificationDebugDumpCard(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit
) {
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
                Row(modifier = Modifier.weight(1f).padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = EdgeCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.settings_debug_title),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                    colors = SwitchDefaults.colors(checkedThumbColor = EdgeCyan)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(if (enabled) R.string.settings_debug_enabled_description else R.string.settings_debug_disabled_description),
                color = if (enabled) Color(0xFFFFCC80) else Color.Gray,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            if (enabled && dumpableList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = {
                            val allDumps = dumpableList.joinToString("\n\n") {
                                "[${it.appName}] ${it.title}\n${it.debugExtrasDump}"
                            }
                            com.devdooly.notificationedge.util.SecureClipboard.copySensitive(
                                context,
                                context.getString(R.string.settings_debug_clipboard_all),
                                allDumps
                            )
                            android.widget.Toast.makeText(context, context.resources.getQuantityString(R.plurals.settings_debug_copied_count, dumpableList.size, dumpableList.size), android.widget.Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EdgeCyan),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.heightIn(min = 30.dp)
                    ) {
                        Text(stringResource(R.string.settings_copy_all), color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (enabled && dumpableList.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.settings_debug_empty),
                    color = Color.DarkGray,
                    fontSize = 11.sp
                )
            } else if (enabled) {
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
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                                )
                                Button(
                                    onClick = {
                                        com.devdooly.notificationedge.util.SecureClipboard.copySensitive(
                                            context,
                                            context.getString(R.string.settings_debug_clipboard_single),
                                            notif.debugExtrasDump.orEmpty()
                                        )
                                        android.widget.Toast.makeText(context, context.getString(R.string.settings_debug_copied_title, notif.title), android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    modifier = Modifier.heightIn(min = 26.dp)
                                ) {
                                    Text(stringResource(R.string.settings_copy), color = EdgeCyan, fontSize = 11.sp)
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
