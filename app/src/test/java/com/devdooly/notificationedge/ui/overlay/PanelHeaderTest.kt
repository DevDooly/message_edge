package com.devdooly.notificationedge.ui.overlay

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
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
class PanelHeaderTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test fun headerStaysOnOneLineAcrossWidthsCountsAndFontScales() {
        val width = mutableStateOf(232)
        val fontScale = mutableStateOf(1f)
        val count = mutableStateOf(1)
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale.value)) {
                MaterialTheme {
                    Box(Modifier.width(width.value.dp).testTag("header")) {
                        PanelHeader(count.value, {}, {}, {})
                    }
                }
            }
        }
        // 실제 패널 폭 220/260/360dp에서 좌우 여백 28dp를 제외한 헤더 폭.
        for (headerWidth in listOf(192, 232, 332)) {
            for (scale in listOf(1f, 1.5f, 2f)) {
                for (notifications in listOf(0, 1, 150, Int.MAX_VALUE)) {
                    composeRule.runOnIdle {
                        width.value = headerWidth
                        fontScale.value = scale
                        count.value = notifications
                    }
                    val title = composeRule.onNodeWithText("Slivue")
                    val items = buildList {
                        add(title)
                        if (notifications > 0) {
                            val badge = composeRule.onNodeWithText(notifications.toString())
                            add(badge)
                            assertSingleLine(badge)
                            add(composeRule.onNodeWithContentDescription("Clear all"))
                        } else {
                            composeRule.onNodeWithContentDescription("Clear all").assertDoesNotExist()
                        }
                        add(composeRule.onNodeWithContentDescription("Settings"))
                        add(composeRule.onNodeWithContentDescription("Close"))
                    }
                    assertSingleLine(title)
                    val container = composeRule.onNodeWithTag("header").fetchSemanticsNode().boundsInRoot
                    val bounds = items.map { it.assertIsDisplayed().fetchSemanticsNode().boundsInRoot }
                    val context = "width=$headerWidth, font=$scale, count=$notifications"
                    bounds.forEach {
                        assertEquals(context, container.center.y, it.center.y, 1f)
                        assertTrue(context, it.left >= container.left && it.right <= container.right)
                        assertTrue(context, it.width > 0f)
                    }
                    bounds.zipWithNext().forEach { (left, right) ->
                        assertTrue(context, left.right <= right.left)
                    }
                    // 기본 글꼴·한 건 알림에서는 최소 폭에서도 앱 이름을 온전히 표시한다.
                    if (scale == 1f && notifications == 1) {
                        assertFalse(context, textLayout(title).isLineEllipsized(0))
                    }
                }
            }
        }
    }

    @Test fun compactButtonsDispatchTouchesAtBothEdgesToTheirOwnAction() {
        val calls = IntArray(3)
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                MaterialTheme {
                    Box(Modifier.width(192.dp)) {
                        PanelHeader(1, { calls[0]++ }, { calls[1]++ }, { calls[2]++ })
                    }
                }
            }
        }
        listOf("Clear all", "Settings", "Close").forEachIndexed { index, description ->
            composeRule.onNodeWithContentDescription(description)
                .assertWidthIsEqualTo(36.dp)
                .assertHeightIsEqualTo(48.dp)
                .performTouchInput {
                    click(Offset(1f, center.y))
                    click(Offset(width - 1f, center.y))
                }
            composeRule.runOnIdle { assertEquals(2, calls[index]) }
        }
        assertArrayEquals(intArrayOf(2, 2, 2), calls)
    }

    private fun assertSingleLine(node: SemanticsNodeInteraction) {
        assertEquals(1, textLayout(node).lineCount)
    }

    private fun textLayout(node: SemanticsNodeInteraction): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        return results.single()
    }
}
