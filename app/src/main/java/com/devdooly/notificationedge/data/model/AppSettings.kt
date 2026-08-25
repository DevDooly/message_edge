package com.devdooly.notificationedge.data.model

enum class EdgeSide {
    LEFT, RIGHT
}

data class AppSettings(
    val isServiceEnabled: Boolean = true,
    val edgeSide: EdgeSide = EdgeSide.RIGHT,
    val handlePositionRatio: Float = 0.5f,
    val handleWidthDp: Int = 8,
    val handleHeightDp: Int = 110,
    val handleColor: Long = 0xFF00E5FF,
    val handleAlpha: Float = 0.75f,
    val isHandleVisible: Boolean = true,
    val launchDirectToPanel: Boolean = true,
    val panelWidthDp: Int = 280,
    val autoDismissOnOpen: Boolean = true,
    val isEdgeLightingEnabled: Boolean = true,
    val edgeLightingDurationMs: Long = 3000L,
    val edgeLightingColor: Long = 0xFF00E5FF,
    val hapticFeedbackEnabled: Boolean = true,
    val excludedPackages: Set<String> = emptySet()
)
