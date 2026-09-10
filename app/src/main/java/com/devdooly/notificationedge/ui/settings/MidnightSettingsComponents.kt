package com.devdooly.notificationedge.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devdooly.notificationedge.BuildConfig
import com.devdooly.notificationedge.R
import com.devdooly.notificationedge.data.model.AppSettings
import com.devdooly.notificationedge.data.model.EdgeSide
import com.devdooly.notificationedge.ui.theme.*

@Composable
internal fun SlivueSettingsBrand() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(Graphite900)
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(stringResource(R.string.app_name), color = TextPrimary, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            Text("v${BuildConfig.VERSION_NAME}", color = TextSecondary, fontSize = 12.sp)
        }
    }
}

/** 상세 조절값을 지우거나 초기화하지 않고 섹션의 표시만 접는다. */
@Composable
internal fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val stateLabel = stringResource(if (expanded) R.string.settings_collapse else R.string.settings_expand)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(color = DarkSurface, shape = RoundedCornerShape(20.dp)) {
            Row(
                Modifier.fillMaxWidth()
                    .semantics { stateDescription = stateLabel }
                    .clickable(role = Role.Button, onClickLabel = stateLabel) { expanded = !expanded }
                    .heightIn(min = 64.dp).padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = EdgeCyan, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(14.dp))
                Text(title, modifier = Modifier.weight(1f), color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = TextSecondary)
            }
        }
        if (expanded) content()
    }
}

@Composable
internal fun HandlePreviewCard(settings: AppSettings, onOpenPanel: () -> Unit) {
    val description = stringResource(
        R.string.design_handle_preview_description,
        stringResource(if (settings.edgeSide == EdgeSide.LEFT) R.string.design_side_left else R.string.design_side_right)
    )
    Card(colors = CardDefaults.cardColors(containerColor = DarkSurface), shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.design_handle_preview), color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(description, color = TextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onOpenPanel, contentPadding = PaddingValues(0.dp)) {
                    Text(stringResource(R.string.design_open_panel), color = EdgeCyan)
                }
            }
            Spacer(Modifier.width(12.dp))
            Canvas(Modifier.size(64.dp, 112.dp).semantics { contentDescription = description }) {
                drawRoundRect(Graphite950, cornerRadius = CornerRadius(12.dp.toPx()))
                repeat(3) { index ->
                    drawRoundRect(
                        Graphite700,
                        topLeft = Offset(size.width * .18f, size.height * (.24f + index * .16f)),
                        size = Size(size.width * .64f, size.height * .1f),
                        cornerRadius = CornerRadius(3.dp.toPx())
                    )
                }
                // 저장된 위치·크기·색·숨김 상태를 그대로 반영하는 미리보기.
                if (settings.isServiceEnabled && settings.isHandleVisible) {
                    val width = (settings.handleWidthDp / 4f).dp.toPx().coerceIn(2.dp.toPx(), size.width * .16f)
                    val height = size.height * (settings.handleHeightDp / 800f).coerceIn(.08f, .5f)
                    val x = if (settings.edgeSide == EdgeSide.LEFT) 0f else size.width - width
                    val y = (size.height - height) * settings.handlePositionRatio.coerceIn(0f, 1f)
                    drawRoundRect(Color(settings.handleColor).copy(alpha = settings.handleAlpha), Offset(x, y), Size(width, height), CornerRadius(width / 2))
                }
            }
        }
    }
}
