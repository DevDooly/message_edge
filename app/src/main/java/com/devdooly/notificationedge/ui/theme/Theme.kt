package com.devdooly.notificationedge.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = AqueousAqua,
    secondary = QuietPeriwinkle,
    tertiary = NeonEmerald,
    background = Graphite950,
    surface = Graphite800,
    surfaceVariant = Graphite700,
    onPrimary = Graphite950,
    onSecondary = Graphite950,
    onBackground = CloudDancer,
    onSurface = CloudDancer,
    onSurfaceVariant = CloudShadow
)

@Composable
fun NotificationEdgeTheme(
    fontId: String = "default",
    transparentStatusBar: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode && view.context is Activity) {
        SideEffect {
            val window = (view.context as Activity).window
            if (transparentStatusBar) {
                window.statusBarColor = android.graphics.Color.TRANSPARENT
            } else {
                window.statusBarColor = colorScheme.background.toArgb()
            }
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = createTypography(resolveFontFamily(context, fontId)),
        content = content
    )
}

@Composable
fun NotificationEdgeTheme(
    appFont: AppFont,
    transparentStatusBar: Boolean = false,
    content: @Composable () -> Unit
) {
    NotificationEdgeTheme(
        fontId = appFont.id,
        transparentStatusBar = transparentStatusBar,
        content = content
    )
}
