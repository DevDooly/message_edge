package com.devdooly.notificationedge.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devdooly.notificationedge.data.model.AppSettings
import com.devdooly.notificationedge.data.model.EdgeSide
import com.devdooly.notificationedge.ui.theme.*
@Composable
internal fun EdgeHandleSettingsCard(
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
internal fun EdgeLightingSettingsCard(
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
