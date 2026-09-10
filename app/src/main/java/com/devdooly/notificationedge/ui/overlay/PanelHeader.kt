package com.devdooly.notificationedge.ui.overlay

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devdooly.notificationedge.R
import com.devdooly.notificationedge.ui.theme.*

@Composable
internal fun PanelHeader(
    notificationCount: Int,
    onClearAll: () -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit
) {
    // 좁은 패널에서도 세 버튼의 터치 영역과 제목을 겹치지 않게 유지한다.
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val compact = maxWidth < 300.dp
        val actions: @Composable () -> Unit = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (notificationCount > 0) {
                    IconButton(onClick = onClearAll, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.ClearAll, stringResource(R.string.panel_clear_all), tint = TextSecondary)
                    }
                }
                IconButton(onClick = onOpenSettings, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Settings, stringResource(R.string.panel_settings), tint = TextSecondary)
                }
                IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Close, stringResource(R.string.panel_close), tint = TextPrimary)
                }
            }
        }
        Column {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.panel_title), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    if (notificationCount > 0) {
                        Spacer(Modifier.width(8.dp))
                        Surface(shape = RoundedCornerShape(8.dp), color = ActionBlue) {
                            Text(
                                stringResource(R.string.panel_count, notificationCount),
                                color = Graphite950, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
                if (!compact) actions()
            }
            if (compact) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { actions() }
        }
    }
}
