package com.devdooly.notificationedge.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationDumpSanitizerTest {

    @Test
    fun `민감한 키와 개인정보를 마스킹한다`() {
        val raw = """
            • access_token (String): "secret-value"
            • sender: "010-1234-5678"
            • email: "tester@example.com"
            • text: "인증번호는 123456 입니다"
        """.trimIndent()

        val result = NotificationDumpSanitizer.sanitize(raw)

        assertFalse(result.contains("secret-value"))
        assertFalse(result.contains("010-1234-5678"))
        assertFalse(result.contains("tester@example.com"))
        assertFalse(result.contains("123456"))
        assertTrue(result.contains("마스킹됨"))
    }

    @Test
    fun `일반 알림 본문은 보존한다`() {
        val raw = "• android.text (CharSequence): \"회의가 오후 3시에 시작됩니다\""

        val result = NotificationDumpSanitizer.sanitize(raw)

        assertTrue(result.contains("회의가 오후 3시에 시작됩니다"))
    }

    @Test
    fun `중첩 번들의 인증 값도 마스킹한다`() {
        val raw = "Bundle: { title=안내, access_token=\"short-secret\", }"

        val result = NotificationDumpSanitizer.sanitize(raw)

        assertTrue(result.contains("access_token=\"<민감정보 마스킹됨>\""))
        assertFalse(result.contains("short-secret"))
    }
}
