package com.devdooly.notificationedge.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AppLogTest {
    @Test
    fun `failure summary contains only exception type`() {
        val summary = AppLog.failureSummary(
            event = "작업 실패",
            error = IllegalStateException("민감한 알림 본문")
        )

        assertEquals("작업 실패 (IllegalStateException)", summary)
        assertFalse(summary.contains("민감한 알림 본문"))
    }
}
