package com.devdooly.notificationedge.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsTest {

    @Test
    fun `verify default AppSettings configuration`() {
        val settings = AppSettings()

        assertTrue(settings.isServiceEnabled)
        assertEquals(EdgeSide.RIGHT, settings.edgeSide)
        assertEquals(0.5f, settings.handlePositionRatio, 0.001f)
        assertEquals(8, settings.handleWidthDp)
        assertEquals(110, settings.handleHeightDp)
        assertEquals(0xFF82D8D0, settings.handleColor)
        assertEquals(0.75f, settings.handleAlpha, 0.001f)
        assertTrue(settings.isHandleVisible)
        assertTrue(settings.launchDirectToPanel)
        assertEquals(280, settings.panelWidthDp)
        assertTrue(settings.autoDismissOnOpen)
        assertTrue(settings.isEdgeLightingEnabled)
        assertEquals(3000L, settings.edgeLightingDurationMs)
        assertEquals(0xFF82D8D0, settings.edgeLightingColor)
        assertEquals(32, settings.edgeLightingCornerRadiusDp)
        assertEquals("default", settings.selectedFont)
        assertTrue(settings.hapticFeedbackEnabled)
        assertTrue(settings.excludedPackages.isEmpty())
    }
}
