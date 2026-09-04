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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
