package com.devdooly.notificationedge.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devdooly.notificationedge.R
import com.devdooly.notificationedge.ui.theme.*
@Composable
internal fun PermissionStatusCard(
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
        shape = RoundedCornerShape(20.dp),
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
                Row(modifier = Modifier.weight(1f).padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (allGranted) Icons.Default.VerifiedUser else Icons.Default.Security,
                        contentDescription = null,
                        tint = if (allGranted) EdgeGreen else (if (allRequiredGranted) EdgeCyan else Color(0xFFFF5252)),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = stringResource(R.string.settings_permissions_title),
                                modifier = Modifier.fillMaxWidth(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (allGranted) EdgeGreen.copy(alpha = 0.18f) else (if (allRequiredGranted) EdgeCyan.copy(alpha = 0.18f) else Color(0x33FF5252).copy(alpha = 0.2f)),
                                border = androidx.compose.foundation.BorderStroke(
                                    0.5.dp,
                                    if (allGranted) EdgeGreen else (if (allRequiredGranted) EdgeCyan else Color(0xFFFF5252))
                                )
                            ) {
                                Text(
                                    text = stringResource(if (allGranted) R.string.settings_permissions_all_granted else if (allRequiredGranted) R.string.settings_permissions_required_granted else R.string.settings_permissions_needed),
                                    color = if (allGranted) EdgeGreen else (if (allRequiredGranted) EdgeCyan else Color(0xFFFF5252)),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = stringResource(if (isExpanded) R.string.settings_collapse else R.string.settings_expand),
                    tint = TextMuted,
                    modifier = Modifier.size(22.dp)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))

                    PermissionItem(
                        title = stringResource(R.string.settings_overlay_permission_title),
                        desc = stringResource(R.string.settings_overlay_permission_description),
                        isGranted = hasOverlay,
                        onClick = onGrantOverlay
                    )
                    HorizontalDivider(color = GlassBorder, modifier = Modifier.padding(vertical = 8.dp))

                    PermissionItem(
                        title = stringResource(R.string.settings_notification_permission_title),
                        desc = stringResource(R.string.settings_notification_permission_description),
                        isGranted = hasNotification,
                        onClick = onGrantNotification
                    )
                    HorizontalDivider(color = GlassBorder, modifier = Modifier.padding(vertical = 8.dp))

                    PermissionItem(
                        title = stringResource(R.string.settings_battery_permission_title),
                        desc = stringResource(R.string.settings_battery_permission_description),
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
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(desc, color = TextMuted, fontSize = 11.sp)
        }
        if (isGranted) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = EdgeGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.settings_permission_granted), color = EdgeGreen, fontSize = 12.sp)
            }
        } else {
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = EdgeCyan),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.heightIn(min = 32.dp)
            ) {
                Text(stringResource(R.string.settings_permission_grant), color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
