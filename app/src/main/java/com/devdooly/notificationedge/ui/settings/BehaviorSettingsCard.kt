package com.devdooly.notificationedge.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devdooly.notificationedge.data.model.AppSettings
import com.devdooly.notificationedge.ui.theme.DarkSurface
import com.devdooly.notificationedge.ui.theme.EdgeCyan

@Composable
internal fun BehaviorSettingsCard(
    settings: AppSettings,
    onPauseMediaOnOpenChange: (Boolean) -> Unit,
    onHapticFeedbackChange: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            BehaviorToggle(
                title = "패널 열릴 때 유튜브 일시 정지",
                description = "엣지 패널을 열 때 유튜브 영상을 자동으로 일시 정지합니다 (끄면 영상과 소리가 멈춤 없이 계속 재생됩니다)",
                checked = settings.pauseMediaOnOpen,
                onCheckedChange = onPauseMediaOnOpenChange
            )

            Spacer(modifier = Modifier.height(12.dp))

            BehaviorToggle(
                title = "햅틱 진동 피드백",
                description = "핸들 터치 및 알림 시 진동",
                checked = settings.hapticFeedbackEnabled,
                onCheckedChange = onHapticFeedbackChange
            )
        }
    }
}

@Composable
private fun BehaviorToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(description, color = Color.Gray, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = EdgeCyan)
        )
    }
}
