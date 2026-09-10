package com.devdooly.notificationedge.ui.settings

import com.devdooly.notificationedge.ui.theme.*

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devdooly.notificationedge.R
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
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            BehaviorToggle(
                title = stringResource(R.string.behavior_pause_youtube),
                description = stringResource(R.string.behavior_pause_youtube_description),
                checked = settings.pauseMediaOnOpen,
                onCheckedChange = onPauseMediaOnOpenChange
            )

            Spacer(modifier = Modifier.height(12.dp))

            BehaviorToggle(
                title = stringResource(R.string.behavior_haptic_feedback),
                description = stringResource(R.string.behavior_haptic_feedback_description),
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
            Text(description, color = TextMuted, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = EdgeCyan)
        )
    }
}
