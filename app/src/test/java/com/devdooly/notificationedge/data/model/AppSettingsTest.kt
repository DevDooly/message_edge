package com.devdooly.notificationedge.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsTest {

    @Test
    fun `verify default AppSettings configuration`() {
        val settings = AppSettings()

        assertTrue(settings.isServiceEnabled)
        assertEquals(EdgeSide.LEFT, settings.edgeSide)
        assertEquals(0.5f, settings.handlePositionRatio, 0.001f)
        assertEquals(8, settings.handleWidthDp)
        assertEquals(110, settings.handleHeightDp)
        assertEquals(0xFF82D8D0, settings.handleColor)
        assertEquals(0.75f, settings.handleAlpha, 0.001f)
        assertTrue(settings.isHandleVisible)
        assertTrue(settings.launchDirectToPanel)
        assertEquals(260, settings.panelWidthDp)
        assertTrue(settings.autoDismissOnOpen)
        assertTrue(settings.isEdgeLightingEnabled)
        assertEquals(3000L, settings.edgeLightingDurationMs)
        assertEquals(0xFF82D8D0, settings.edgeLightingColor)
        assertEquals(32, settings.edgeLightingCornerRadiusDp)
        assertEquals("default", settings.selectedFont)
        assertTrue(settings.hapticFeedbackEnabled)
        assertFalse(settings.pauseMediaOnOpen)
        assertTrue(settings.excludedPackages.isEmpty())
        assertTrue(settings.discoveredAppPackages.isEmpty())
        assertTrue(settings.blockedKeywords.isEmpty())
    }

    @Test
    fun `verify AppSettings with custom filter keywords and packages`() {
        val settings = AppSettings(
            excludedPackages = setOf("com.spam.app"),
            discoveredAppPackages = setOf("com.spam.app", "com.kakao.talk"),
            blockedKeywords = setOf("광고", "특가", "스팸")
        )

        assertTrue(settings.excludedPackages.contains("com.spam.app"))
        assertEquals(2, settings.discoveredAppPackages.size)
        assertTrue(settings.blockedKeywords.contains("광고"))
    }
}
