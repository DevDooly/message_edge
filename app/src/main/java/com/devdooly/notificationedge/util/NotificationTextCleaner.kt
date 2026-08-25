package com.devdooly.notificationedge.util

/**
 * 알림 메시지의 중복 발신자/전화번호 접두어 제거 및
 * 카카오톡 단체방 참여자 명단 정제 유틸리티
 */
object NotificationTextCleaner {

    /**
     * 알림 본문 텍스트에서 제목(Title), 발신자(Sender), 전화번호 등의 중복 접두어 제거
     */
    fun cleanMessageText(text: String?, title: String?, sender: String? = null): String {
        if (text.isNullOrBlank()) return ""
        var cleaned = text.trim()

        // 1. 발신자(Sender) 중복 접두어 제거
        if (!sender.isNullOrBlank()) {
            cleaned = removeSenderPrefix(cleaned, sender.trim())
        }

        // 2. 제목(Title) 중복 접두어 제거
        if (!title.isNullOrBlank()) {
            cleaned = removeSenderPrefix(cleaned, title.trim())
        }

        // 3. 전화번호 중복 접두어 제거 (예: "010-1234-5678: 안녕하세요", "01012345678 - 내용")
        val phoneRegex = Regex("""^(\+?82[-. ]?10[-. ]?\d{4}[-. ]?\d{4}|01[016789][-. ]?\d{3,4}[-. ]?\d{4}|\d{2,4}[-. ]?\d{3,4}[-. ]?\d{4})[:：\s\-]*""")
        val phoneMatch = phoneRegex.find(cleaned)
        if (phoneMatch != null) {
            val prefix = phoneMatch.value
            val remainder = cleaned.substring(prefix.length).trim()
            if (remainder.isNotEmpty()) {
                cleaned = remainder
            }
        }

        return cleaned
    }

    /**
     * 텍스트 시작 부분에 특정 이름/발신자 접두어가 있으면 제거
     */
    private fun removeSenderPrefix(content: String, name: String): String {
        if (name.isBlank() || content.isBlank()) return content
        val escapedName = Regex.escape(name)

        // 1) "이름: 내용" or "이름 : 내용" or "이름- 내용"
        // 2) "[이름] 내용" or "(이름) 내용" or "<이름> 내용"
        // 3) "이름\n내용"
        val patterns = listOf(
            Regex("""^$escapedName\s*[:：\-]\s*""", RegexOption.IGNORE_CASE),
            Regex("""^\[$escapedName\]\s*[:：\-]?\s*""", RegexOption.IGNORE_CASE),
            Regex("""^\($escapedName\)\s*[:：\-]?\s*""", RegexOption.IGNORE_CASE),
            Regex("""^<$escapedName>\s*[:：\-]?\s*""", RegexOption.IGNORE_CASE),
            Regex("""^$escapedName\s*\n+\s*""", RegexOption.IGNORE_CASE)
        )

        var result = content
        for (pattern in patterns) {
            val match = pattern.find(result)
            if (match != null && match.range.first == 0) {
                val remainder = result.substring(match.range.last + 1).trim()
                if (remainder.isNotEmpty()) {
                    result = remainder
                }
                break
            }
        }
        return result
    }

    /**
     * 카카오톡 등 그룹채팅 참여자 목록이 너무 길 때 (예: "김철수, 이영희, 박민수, 최지훈, 정수진")
     * 참여자가 4명 이상이면 "김철수, 이영희, 박민수..." 형태로 알맞게 자름
     */
    fun formatGroupTitle(rawTitle: String?, maxNames: Int = 3): String {
        if (rawTitle.isNullOrBlank()) return ""
        val title = rawTitle.trim()

        // 쉼표(,)로 구분된 이름 목록인지 확인
        if (title.contains(",")) {
            val names = title.split(",").map { it.trim() }.filter { it.isNotBlank() }
            if (names.size > maxNames) {
                val head = names.take(maxNames).joinToString(", ")
                return "$head..."
            }
        }
        return title
    }
}
