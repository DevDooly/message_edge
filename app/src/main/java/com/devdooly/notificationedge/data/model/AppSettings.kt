package com.devdooly.notificationedge.data.model

enum class EdgeSide {
    LEFT, RIGHT
}

data class AppSettings(
    val isServiceEnabled: Boolean = true,
    val edgeSide: EdgeSide = EdgeSide.LEFT,
    val handlePositionRatio: Float = 0.5f,
    val handleWidthDp: Int = 8,
    val handleHeightDp: Int = 110,
    val handleColor: Long = 0xFF82D8D0,
    val handleAlpha: Float = 0.75f,
    val isHandleVisible: Boolean = true,
    val launchDirectToPanel: Boolean = true,
    val panelWidthDp: Int = 260,
    val autoDismissOnOpen: Boolean = true,
    val isEdgeLightingEnabled: Boolean = true,
    val edgeLightingDurationMs: Long = 3000L,
    val edgeLightingColor: Long = 0xFF82D8D0,
    val edgeLightingCornerRadiusDp: Int = 32,
    val selectedFont: String = "default",
    val hapticFeedbackEnabled: Boolean = true,
    val pauseMediaOnOpen: Boolean = false,
    val diagnosticModeEnabled: Boolean = false,
    val externalControlEnabled: Boolean = false,
    val excludedPackages: Set<String> = emptySet(),
    val discoveredAppPackages: Set<String> = emptySet(),
    val blockedKeywords: Set<String> = emptySet()
)
