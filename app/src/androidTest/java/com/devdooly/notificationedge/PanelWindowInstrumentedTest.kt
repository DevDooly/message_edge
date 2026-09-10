package com.devdooly.notificationedge

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.graphics.Rect
import android.view.accessibility.AccessibilityWindowInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.devdooly.notificationedge.data.model.EdgeNotification
import com.devdooly.notificationedge.data.model.NotificationActionItem
import com.devdooly.notificationedge.data.repository.NotificationRepository
import com.devdooly.notificationedge.ui.overlay.EdgePanelActivity
import org.junit.After
import org.junit.Before
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.FileInputStream

@RunWith(AndroidJUnit4::class)
class PanelWindowInstrumentedTest {
    @get:Rule val composeRule = createEmptyComposeRule()
    private var scenario: ActivityScenario<EdgePanelActivity>? = null
    private val testKey = "slivue-window-ime-regression"
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private var originalAccessibilityFlags = 0

    @Before fun observeSystemWindows() {
        val automation = instrumentation.uiAutomation
        val info = automation.serviceInfo
        originalAccessibilityFlags = info.flags
        info.flags = info.flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        automation.serviceInfo = info
    }

    @After fun tearDown() {
        scenario?.close()
        instrumentation.runOnMainSync { NotificationRepository.removeNotification(testKey) }
        val automation = instrumentation.uiAutomation
        automation.serviceInfo = automation.serviceInfo.apply { flags = originalAccessibilityFlags }
    }

    @Test fun panelFillsTheVisibleSafeAreaAndKeepsItsHeaderOnOneLine() {
        launchPanel()
        composeRule.waitForIdle()
        scenario!!.onActivity { activity ->
            val decor = activity.window.decorView
            val location = IntArray(2)
            decor.getLocationOnScreen(location)
            val visibleArea = Rect()
            decor.getWindowVisibleDisplayFrame(visibleArea)
            assertEquals(visibleArea.left, location[0])
            assertEquals(visibleArea.width(), decor.width)
            assertTrue(location[1] >= visibleArea.top)
            assertTrue(location[1] + decor.height <= visibleArea.bottom)
        }
        val context = instrumentation.targetContext
        val title = composeRule.onNodeWithText(context.getString(R.string.panel_title), useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val close = composeRule.onNodeWithContentDescription(context.getString(R.string.panel_close))
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertEquals(title.center.y, close.center.y, 1f)
    }

    @Test fun syntheticReplyCanOpenImeEnterTextAndHideImeWithoutClosingPanel() {
        instrumentation.runOnMainSync {
            // 전송 대상이 없는 합성 데이터만 사용한다. 실제 메시지 전송은 수행하지 않는다.
            NotificationRepository.addOrUpdateNotification(EdgeNotification(
                key = testKey, id = 99123, packageName = instrumentation.targetContext.packageName,
                appName = "Slivue regression", title = "Keyboard regression", text = "Synthetic only",
                actions = listOf(NotificationActionItem("Reply", null, isReply = true, remoteInputKey = "test"))
            ))
        }
        launchPanel()
        val context = instrumentation.targetContext
        composeRule.onNodeWithText(context.getString(R.string.panel_reply)).performScrollTo().performClick()
        composeRule.waitUntil(10_000) { imeVisible() }
        composeRule.onNode(hasSetTextAction()).assertIsDisplayed().performTextInput("Slivue keyboard test")
        composeRule.onNodeWithText("Slivue keyboard test").assertIsDisplayed()
        instrumentation.uiAutomation.executeShellCommand("input keyevent 4").use { descriptor ->
            FileInputStream(descriptor.fileDescriptor).use { it.readBytes() }
        }
        composeRule.waitUntil(10_000) { !imeVisible() }
        scenario!!.onActivity { assertFalse(it.isFinishing) }
    }

    private fun launchPanel() {
        scenario = ActivityScenario.launch(Intent(instrumentation.targetContext, EdgePanelActivity::class.java))
    }

    private fun imeVisible(): Boolean {
        // 구형 OS의 크기가 조절된 투명 창에서는 호환 Insets 추정값 대신 실제 키보드 창을 확인한다.
        return instrumentation.uiAutomation.windows.any { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }
    }
}
