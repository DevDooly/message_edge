package com.devdooly.notificationedge.ui.overlay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EdgePanelUiStateTest {

    @Test
    fun `답장 모드를 닫으면 대상과 입력문을 함께 초기화한다`() {
        val state = EdgePanelUiState()
        state.openReply("notification-key")
        state.updateReplyText("답장 내용")

        state.closeReply()

        assertNull(state.activeReplyKey)
        assertEquals("", state.replyText)
    }

    @Test
    fun `닫기 전환은 중복 실행되지 않는다`() {
        val state = EdgePanelUiState()
        state.reveal()

        assertTrue(state.beginClose())
        assertFalse(state.isVisible)
        assertFalse(state.beginClose())
    }

    @Test
    fun `드래그 누적값으로 닫기 임계값을 판단하고 종료 시 초기화한다`() {
        val state = EdgePanelUiState()
        state.beginDrag()
        state.dragBy(18f)
        state.dragBy(23f)

        assertTrue(state.isDragPastThreshold(40f))

        state.endDrag()
        assertEquals(0f, state.dragOffsetX)
    }
}
