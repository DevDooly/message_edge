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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devdooly.notificationedge.R
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
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.appearance_handle_layout),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(14.dp))

            // 패널 가로 너비 (5dp 단위 조절)
            Text(
                stringResource(R.string.appearance_panel_width, settings.panelWidthDp),
                color = TextSecondary,
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
                    Text(stringResource(R.string.appearance_auto_dismiss), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(stringResource(R.string.appearance_auto_dismiss_description), color = TextMuted, fontSize = 11.sp)
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
                    Text(stringResource(R.string.appearance_handle_visible), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(stringResource(R.string.appearance_handle_visible_description), color = TextMuted, fontSize = 11.sp)
                }
                Switch(
                    checked = settings.isHandleVisible,
                    onCheckedChange = onVisibleToggle,
                    colors = SwitchDefaults.colors(checkedThumbColor = EdgeCyan)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 좌 / 우 선택
            Text(stringResource(R.string.appearance_handle_side), color = TextSecondary, fontSize = 13.sp)
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
                        stringResource(R.string.appearance_side_left),
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
                        stringResource(R.string.appearance_side_right),
                        color = if (settings.edgeSide == EdgeSide.RIGHT) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 상하 위치 (Y 비율)
            Text(
                stringResource(R.string.appearance_vertical_position, (settings.handlePositionRatio * 100).toInt()),
                color = TextSecondary,
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
                stringResource(R.string.appearance_handle_height, settings.handleHeightDp),
                color = TextSecondary,
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
                stringResource(R.string.appearance_handle_width, settings.handleWidthDp),
                color = TextSecondary,
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
                    stringResource(R.string.appearance_handle_opacity, (settings.handleAlpha * 100).toInt()),
                    color = TextSecondary,
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
                Text(stringResource(R.string.appearance_handle_color), color = TextSecondary, fontSize = 13.sp)
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
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.appearance_lighting),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = stringResource(R.string.appearance_lighting_description),
                        color = TextMuted,
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
                Text(stringResource(R.string.appearance_lighting_color), color = TextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                ColorPaletteRow(
                    selectedColor = settings.edgeLightingColor,
                    onSelectColor = onColorChange
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 모서리 둥글기 (곡률) 조절 (0dp 직각 ~ 50dp 둥근 모서리)
                Text(
                    text = stringResource(R.string.appearance_corner_radius, settings.edgeLightingCornerRadiusDp),
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Text(
                    text = stringResource(
                        if (settings.edgeLightingCornerRadiusDp == 0) R.string.appearance_square_corners
                        else R.string.appearance_adjust_corners
                    ),
                    color = TextMuted,
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
                    Text(stringResource(R.string.appearance_test_lighting), color = EdgeCyan, fontWeight = FontWeight.Bold)
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
                        contentDescription = stringResource(R.string.appearance_selected),
                        tint = if (colorHex == 0xFFFFFFFF) Color.Black else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
