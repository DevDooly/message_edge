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

        // 1. 전화번호 접두어 제거 (예: "010-1234-5678: ", "01012345678 - ", "+82 10-1234-5678: ")
        cleaned = cleaned.replaceFirst(
            Regex("""^(\+?82[-. ]?10[-. ]?\d{4}[-. ]?\d{4}|01[016789][-. ]?\d{3,4}[-. ]?\d{4}|\d{2,4}[-. ]?\d{3,4}[-. ]?\d{4})[:：\s\-]*"""),
            ""
        ).trim()

        // 2. 제목/발신자 이름 매칭 기반 접두어 제거
        cleaned = removeDuplicateNamePrefix(cleaned, title, sender)

        // 3. 범용 발신자 콜론 접두어 무조건 제거 (예: "홍길동: 안녕하세요", "김철수 : 오늘 몇시?", "[팀장]: 회의시작")
        val genericColon = Regex("""^(\[[^\]\n]{1,20}\]|\([^\)\n]{1,20}\)|[가-힣a-zA-Z0-9_\.\s]{1,20})[:：\-]\s*(.+)$""", RegexOption.DOT_MATCHES_ALL).find(cleaned)
        if (genericColon != null) {
            val contentAfterColon = genericColon.groupValues[2].trim()
            if (contentAfterColon.isNotEmpty()) {
                cleaned = contentAfterColon
            }
        }

        // 4. 대괄호 접두어 제거 (예: "[홍길동] 안녕하세요", "[Web발신] 택배가 도착했습니다")
        val genericBracket = Regex("""^\[[가-힣a-zA-Z0-9_\.\s]{1,20}\]\s*(.+)$""", RegexOption.DOT_MATCHES_ALL).find(cleaned)
        if (genericBracket != null) {
            val contentAfterBracket = genericBracket.groupValues[1].trim()
            if (contentAfterBracket.isNotEmpty()) {
                cleaned = contentAfterBracket
            }
        }

        return cleaned.trim()
    }

    /**
     * 본문 시작 부분의 발신자/제목 이름 접두어 판별 및 잘라내기
     */
    private fun removeDuplicateNamePrefix(content: String, title: String?, sender: String?): String {
        var result = content.trim()
        if (result.isBlank()) return ""

        // A) "이름: 내용" 또는 "[이름]: 내용" 또는 "(이름): 내용" 패턴
        val colonMatch = Regex("""^([^:\n]{1,35})[:：\-]\s*(.*)$""", RegexOption.DOT_MATCHES_ALL).find(result)
        if (colonMatch != null) {
            val rawPrefix = colonMatch.groupValues[1].trim()
            val cleanPrefix = rawPrefix
                .removeSurrounding("[", "]")
                .removeSurrounding("(", ")")
                .removeSurrounding("<", ">")
                .trim()
            if (isMatchingName(cleanPrefix, title, sender) || cleanPrefix.isEmpty()) {
                val remainder = colonMatch.groupValues[2].trim()
                if (remainder.isNotEmpty()) {
                    result = remainder
                }
            }
        }

        // B) "[이름] 내용" 또는 "(이름) 내용" 패턴 (콜론 없음)
        val bracketMatch = Regex("""^[\[\(<]([^\]\)>]{1,35})[\]\)>]\s*(.*)$""", RegexOption.DOT_MATCHES_ALL).find(result)
        if (bracketMatch != null) {
            val rawPrefix = bracketMatch.groupValues[1].trim()
            if (isMatchingName(rawPrefix, title, sender)) {
                val remainder = bracketMatch.groupValues[2].trim()
                if (remainder.isNotEmpty()) {
                    result = remainder
                }
            }
        }

        // C) "이름\n내용" 패턴
        val newlineMatch = Regex("""^([^\n]{1,35})\n+\s*(.*)$""", RegexOption.DOT_MATCHES_ALL).find(result)
        if (newlineMatch != null) {
            val rawPrefix = newlineMatch.groupValues[1].trim()
                .removeSurrounding("[", "]")
                .removeSurrounding("(", ")")
            if (isMatchingName(rawPrefix, title, sender)) {
                val remainder = newlineMatch.groupValues[2].trim()
                if (remainder.isNotEmpty()) {
                    result = remainder
                }
            }
        }

        return result
    }

    /**
     * 접두어 이름이 알림 Title 또는 Sender와 일치/유사한지 검사
     */
    private fun isMatchingName(prefix: String, title: String?, sender: String?): Boolean {
        if (prefix.isBlank()) return false
        val p = prefix.trim().lowercase()

        val t = title?.trim()?.lowercase()
        val s = sender?.trim()?.lowercase()

        if (t != null && t.isNotBlank()) {
            if (p == t || t.contains(p) || p.contains(t)) return true
        }

        if (s != null && s.isNotBlank()) {
            if (p == s || s.contains(p) || p.contains(s)) return true
        }

        return false
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
