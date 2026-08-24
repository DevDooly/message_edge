package com.devdooly.notificationedge.ui.overlay

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.devdooly.notificationedge.ui.theme.EdgeCyan
import kotlinx.coroutines.delay

@Composable
fun EdgeLightingEffect(
    color: Color = EdgeCyan,
    durationMs: Long = 3000L,
    onFinish: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "edge_lighting_anim")
    
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    var alpha by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(Unit) {
        // 지속시간 후 페이드아웃
        delay(durationMs - 500)
        val fadeSteps = 10
        val stepDelay = 50L
        for (i in fadeSteps downTo 0) {
            alpha = i / fadeSteps.toFloat()
            delay(stepDelay)
        }
        onFinish()
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = 8.dp.toPx()
        val cornerRadius = 32.dp.toPx()
        val colors = listOf(
            color.copy(alpha = alpha * 0.9f),
            color.copy(alpha = alpha * 0.2f),
            color.copy(alpha = alpha * 0.9f)
        )

        val brush = Brush.sweepGradient(
            colors = colors,
            center = Offset(size.width / 2f, size.height / 2f)
        )

        drawRoundRect(
            brush = brush,
            topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
            size = Size(size.width - strokeWidth, size.height - strokeWidth),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}
