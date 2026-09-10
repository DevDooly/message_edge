package com.devdooly.notificationedge.ui.overlay

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
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
    // 기본/최소 패널 폭에서도 제목·개수·동작을 항상 같은 줄에 배치한다.
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val compact = maxWidth < 300.dp
        val actionWidth = when {
            maxWidth < 220.dp -> 36.dp
            compact -> 40.dp
            else -> 48.dp
        }
        val viewConfiguration = LocalViewConfiguration.current
        val headerViewConfiguration = remember(viewConfiguration, actionWidth) {
            object : ViewConfiguration by viewConfiguration {
                // 좁은 버튼의 보이지 않는 터치 확장이 이웃 버튼과 겹치지 않게 한다.
                override val minimumTouchTargetSize = DpSize(actionWidth, 48.dp)
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.weight(1f).padding(end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    stringResource(R.string.panel_title),
                    modifier = Modifier.weight(1f, fill = false),
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (compact) 17.sp else 20.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (notificationCount > 0) {
                    Surface(shape = RoundedCornerShape(8.dp), color = ActionBlue) {
                        Text(
                            stringResource(R.string.panel_count, notificationCount),
                            color = Graphite950, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 56.dp).padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }
            CompositionLocalProvider(
                LocalMinimumInteractiveComponentSize provides actionWidth,
                LocalViewConfiguration provides headerViewConfiguration
            ) {
                if (notificationCount > 0) {
                    IconButton(onClick = onClearAll, modifier = Modifier.width(actionWidth).height(48.dp)) {
                        Icon(Icons.Default.ClearAll, stringResource(R.string.panel_clear_all), tint = TextSecondary)
                    }
                }
                IconButton(onClick = onOpenSettings, modifier = Modifier.width(actionWidth).height(48.dp)) {
                    Icon(Icons.Default.Settings, stringResource(R.string.panel_settings), tint = TextSecondary)
                }
                IconButton(onClick = onClose, modifier = Modifier.width(actionWidth).height(48.dp)) {
                    Icon(Icons.Default.Close, stringResource(R.string.panel_close), tint = TextPrimary)
                }
            }
        }
    }
}
