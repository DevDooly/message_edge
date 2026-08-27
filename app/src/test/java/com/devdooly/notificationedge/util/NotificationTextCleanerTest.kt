package com.devdooly.notificationedge.util

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationTextCleanerTest {

    @Test
    fun `cleanMessageText should remove colon sender prefix`() {
        val input = "홍길동: 안녕하세요 반갑습니다."
        val result = NotificationTextCleaner.cleanMessageText(input, title = "홍길동")
        assertEquals("안녕하세요 반갑습니다.", result)
    }

    @Test
    fun `cleanMessageText should remove generic colon prefix even if title differs`() {
        val input = "김철수 : 오늘 회의 몇 시인가요?"
        val result = NotificationTextCleaner.cleanMessageText(input, title = "프로젝트 단톡방")
        assertEquals("오늘 회의 몇 시인가요?", result)
    }

    @Test
    fun `cleanMessageText should remove phone number prefix and preserve bracket content`() {
        val input = "010-1234-5678: [인증번호 482910] 입력해주세요."
        val result = NotificationTextCleaner.cleanMessageText(input, title = "010-1234-5678")
        assertEquals("[인증번호 482910] 입력해주세요.", result)
    }

    @Test
    fun `cleanMessageText should remove telecom bracket prefix like Web발신`() {
        val input = "[Web발신] 주문하신 상품이 배송 시작되었습니다."
        val result = NotificationTextCleaner.cleanMessageText(input, title = "택배사")
        assertEquals("주문하신 상품이 배송 시작되었습니다.", result)
    }

    @Test
    fun `cleanMessageText should preserve stock and content brackets like Toss securities`() {
        val input = "[에이피알] 100주 구매"
        val result = NotificationTextCleaner.cleanMessageText(input, title = "비닐봉짘님의 거래", sender = null)
        assertEquals("[에이피알] 100주 구매", result)
    }

    @Test
    fun `cleanMessageText should handle newline matching sender`() {
        val input = "이영희\n점심 같이 드실래요?"
        val result = NotificationTextCleaner.cleanMessageText(input, title = "이영희", sender = "이영희")
        assertEquals("점심 같이 드실래요?", result)
    }

    @Test
    fun `cleanMessageText should handle null or blank input`() {
        assertEquals("", NotificationTextCleaner.cleanMessageText(null, null))
        assertEquals("", NotificationTextCleaner.cleanMessageText("   ", "제목"))
    }

    @Test
    fun `cleanMessageText should keep normal text without prefix intact`() {
        val input = "안녕하세요! 좋은 아침입니다."
        val result = NotificationTextCleaner.cleanMessageText(input, title = "채팅방")
        assertEquals("안녕하세요! 좋은 아침입니다.", result)
    }

    @Test
    fun `formatGroupTitle should truncate when names exceed maxNames`() {
        val title = "김철수, 이영희, 박민수, 최지훈, 정수진"
        val result = NotificationTextCleaner.formatGroupTitle(title, maxNames = 3)
        assertEquals("김철수, 이영희, 박민수...", result)
    }

    @Test
    fun `formatGroupTitle should keep names intact when count is within maxNames`() {
        val title = "김철수, 이영희"
        val result = NotificationTextCleaner.formatGroupTitle(title, maxNames = 3)
        assertEquals("김철수, 이영희", result)
    }

    @Test
    fun `formatGroupTitle should handle non-comma title safely`() {
        val title = "안드로이드 개발자 모임"
        val result = NotificationTextCleaner.formatGroupTitle(title)
        assertEquals("안드로이드 개발자 모임", result)
    }
}
