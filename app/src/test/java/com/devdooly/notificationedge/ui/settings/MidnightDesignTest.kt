package com.devdooly.notificationedge.ui.settings

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.devdooly.notificationedge.data.model.AppSettings
import com.devdooly.notificationedge.ui.overlay.PanelHeader
import com.devdooly.notificationedge.ui.theme.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "en-rUS-w360dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MidnightDesignTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test fun sectionOpensClosesAndRestoresItsExpansion() {
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent {
            MaterialTheme {
                SettingsSection("Handle style", Icons.Default.Settings) { Text("Saved settings") }
            }
        }
        composeRule.onNodeWithText("Saved settings").assertDoesNotExist()
        composeRule.onNodeWithText("Handle style").performClick()
        composeRule.onNodeWithText("Saved settings").assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        composeRule.onNodeWithText("Saved settings").assertIsDisplayed()
        composeRule.onNodeWithText("Handle style").performClick()
        composeRule.onNodeWithText("Saved settings").assertDoesNotExist()
    }

    @Test fun previewOpensPanelWithoutChangingCustomSettings() {
        var opens = 0
        val settings = AppSettings(handleColor = 0xFFFF4081, handleAlpha = .3f, handleWidthDp = 16)
        composeRule.setContent {
            MaterialTheme { Box(Modifier.width(320.dp)) { HandlePreviewCard(settings) { opens++ } } }
        }
        composeRule.onNodeWithText("Edge handle preview").assertIsDisplayed()
        composeRule.onNodeWithText("Open panel").performClick()
        assertEquals(1, opens)
        assertEquals(0xFFFF4081, settings.handleColor)
        assertEquals(.3f, settings.handleAlpha)
    }

    @Test fun narrowHeaderHasDistinctAccessibleActionsAtLargeFont() {
        var clears = 0
        var settings = 0
        var closes = 0
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1.5f)) {
                MaterialTheme {
                    Box(Modifier.width(232.dp)) {
                        PanelHeader(150, { clears++ }, { settings++ }, { closes++ })
                    }
                }
            }
        }
        val clear = composeRule.onNodeWithContentDescription("Clear all")
        val settingsButton = composeRule.onNodeWithContentDescription("Settings")
        val close = composeRule.onNodeWithContentDescription("Close")
        clear.assertIsDisplayed().performClick()
        settingsButton.assertIsDisplayed().performClick()
        close.assertIsDisplayed().performClick()
        assertEquals(listOf(1, 1, 1), listOf(clears, settings, closes))
        assertFalse(clear.fetchSemanticsNode().boundsInRoot.overlaps(settingsButton.fetchSemanticsNode().boundsInRoot))
        assertFalse(settingsButton.fetchSemanticsNode().boundsInRoot.overlaps(close.fetchSemanticsNode().boundsInRoot))
    }

    @Test fun midnightTextTokensHaveReadableContrastOnCards() {
        listOf(TextPrimary, TextSecondary, TextMuted, EdgeCyan).forEach { foreground ->
            assertTrue(ColorUtils.calculateContrast(foreground.toArgb(), DarkSurface.toArgb()) >= 4.5)
        }
        assertTrue(ColorUtils.calculateContrast(Graphite950.toArgb(), ActionBlue.toArgb()) >= 4.5)
        assertEquals(0xFF060B1E.toInt(), DarkBackground.toArgb())
        assertEquals(0xFF102744.toInt(), DarkSurface.toArgb())
        assertEquals(1f, DarkCardBackground.alpha)
    }
}
