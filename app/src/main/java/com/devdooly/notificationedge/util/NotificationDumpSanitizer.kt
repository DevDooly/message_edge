package com.devdooly.notificationedge.util

/**
 * 알림 진단 덤프에서 개인정보와 인증 정보를 제거하고 크기를 제한한다.
 */
object NotificationDumpSanitizer {

    private const val MAX_LINES = 300
    private const val MAX_LINE_LENGTH = 512

    private val sensitiveKeyNames = listOf(
        "token", "auth", "authorization", "password", "passwd", "secret",
        "session", "cookie", "otp", "pin", "credential", "access_key", "api_key"
    )
    private val emailPattern = Regex("""[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}""", RegexOption.IGNORE_CASE)
    private val phonePattern = Regex("""(?<!\d)(?:\+?82[- ]?)?0?1[016789][-. ]?\d{3,4}[-. ]?\d{4}(?!\d)""")
    private val bearerPattern = Regex("""(?i)bearer\s+[A-Za-z0-9._~+/=-]{8,}""")
    private val longSecretPattern = Regex("""(?<![A-Za-z0-9])[A-Za-z0-9_+/=-]{48,}(?![A-Za-z0-9])""")
    private val otpPattern = Regex("""(?<!\d)\d{6}(?!\d)""")
    private val embeddedSensitiveValuePattern = Regex(
        """(?i)((?:token|auth|authorization|password|passwd|secret|session|cookie|otp|pin|credential|access_key|api_key)[A-Za-z0-9_.-]*\s*=\s*)(?:"[^"]*"|'[^']*'|[^,}\s]+)"""
    )

    fun sanitize(rawDump: String): String {
        if (rawDump.isBlank()) return ""

        val sanitizedLines = rawDump.lineSequence()
            .take(MAX_LINES)
            .map(::sanitizeLine)
            .toList()

        return buildString {
            append(sanitizedLines.joinToString("\n"))
            if (rawDump.lineSequence().drop(MAX_LINES).any()) {
                append("\n… (진단 덤프가 길이 제한으로 생략됨)")
            }
        }
    }

    private fun sanitizeLine(rawLine: String): String {
        val line = rawLine.take(MAX_LINE_LENGTH)
        val keyPart = line.substringBefore(':', missingDelimiterValue = "")
        if (keyPart.isNotBlank() && sensitiveKeyNames.any { keyPart.contains(it, ignoreCase = true) }) {
            return "$keyPart: <민감정보 마스킹됨>"
        }

        return line
            .replace(embeddedSensitiveValuePattern) { match ->
                "${match.groupValues[1]}\"<민감정보 마스킹됨>\""
            }
            .replace(bearerPattern, "Bearer <마스킹됨>")
            .replace(emailPattern, "<이메일 마스킹됨>")
            .replace(phonePattern, "<전화번호 마스킹됨>")
            .replace(otpPattern, "<인증번호 마스킹됨>")
            .replace(longSecretPattern, "<긴 값 마스킹됨>")
    }
}
