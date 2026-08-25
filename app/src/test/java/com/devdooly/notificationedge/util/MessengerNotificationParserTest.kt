package com.devdooly.notificationedge.util

import android.app.Notification
import android.content.Context
import android.os.Bundle
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MessengerNotificationParserTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    private fun createMockSbn(
        packageName: String = "com.kakao.talk",
        title: String? = null,
        text: String? = null,
        subText: String? = null,
        conversationTitle: String? = null,
        summaryText: String? = null,
        isGroupConversation: Boolean = false,
        messages: Array<Bundle>? = null
    ): StatusBarNotification {
        val sbn = mockk<StatusBarNotification>(relaxed = true)
        
        val builder = NotificationCompat.Builder(context, "test_channel")
            .setContentTitle(title)
            .setContentText(text)
            .setSubText(subText)
            
        val notification = builder.build()
        if (conversationTitle != null) {
            notification.extras.putCharSequence(Notification.EXTRA_CONVERSATION_TITLE, conversationTitle)
        }
        if (summaryText != null) {
            notification.extras.putCharSequence(Notification.EXTRA_SUMMARY_TEXT, summaryText)
        }
        if (isGroupConversation) {
            notification.extras.putBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, true)
        }
        if (messages != null) {
            @Suppress("DEPRECATION")
            notification.extras.putParcelableArray(Notification.EXTRA_MESSAGES, messages)
        }

        every { sbn.packageName } returns packageName
        every { sbn.notification } returns notification
        every { sbn.postTime } returns 1000L

        return sbn
    }

    @Test
    fun `kakao talk group chat with subText should extract room title and sender properly`() {
        val sbn = createMockSbn(
            packageName = "com.kakao.talk",
            title = "홍길동",
            text = "오늘 회의 몇시인가요?",
            subText = "개발팀 단톡방"
        )

        val result = MessengerNotificationParser.parse(sbn)
        assertEquals("개발팀 단톡방", result.roomTitle)
        assertEquals("개발팀 단톡방", result.groupRoomName)
        assertTrue(result.isGroupChat)
        assertEquals("홍길동", result.currentSender)
        assertEquals("오늘 회의 몇시인가요?", result.cleanText)
    }

    @Test
    fun `kakao talk group chat with title containing paren should extract room title`() {
        val sbn = createMockSbn(
            packageName = "com.kakao.talk",
            title = "홍길동 (가족모임)",
            text = "저녁 7시에 만나요"
        )

        val result = MessengerNotificationParser.parse(sbn)
        assertEquals("가족모임", result.roomTitle)
        assertEquals("가족모임", result.groupRoomName)
        assertTrue(result.isGroupChat)
        assertEquals("홍길동", result.currentSender)
    }

    @Test
    fun `kakao talk 1-to-1 direct message should remain direct chat`() {
        val sbn = createMockSbn(
            packageName = "com.kakao.talk",
            title = "이영희",
            text = "밥 먹었어?"
        )

        val result = MessengerNotificationParser.parse(sbn)
        assertEquals("이영희", result.roomTitle)
        assertNull(result.groupRoomName)
        assertEquals("이영희", result.currentSender)
        assertEquals("밥 먹었어?", result.cleanText)
    }

    @Test
    fun `kakao talk actual dump group chat should synthesize participants room title`() {
        val msg1 = Bundle().apply {
            putCharSequence("sender", "미리비트 윤창빈 책임")
            putCharSequence("text", "전 한 29분 정도입니다")
            putLong("time", 1001L)
        }
        val msg2 = Bundle().apply {
            putCharSequence("sender", "미리비트 정진우 책임")
            putCharSequence("text", "판교 출발")
            putLong("time", 1002L)
        }
        val msg3 = Bundle().apply {
            putCharSequence("sender", "김영남")
            putCharSequence("text", "@미리비트 정진우 책임 미리 주문점여")
            putLong("time", 1003L)
        }

        val sbn = createMockSbn(
            packageName = "com.kakao.talk",
            title = "김영남",
            text = "@미리비트 정진우 책임 미리 주문점여",
            isGroupConversation = true,
            messages = arrayOf(msg1, msg2, msg3)
        )

        val result = MessengerNotificationParser.parse(sbn)
        assertTrue(result.isGroupChat)
        assertEquals("미리비트 윤창빈 책임, 미리비트 정진우 책임, 김영남", result.roomTitle)
        assertEquals("김영남", result.currentSender)
        assertEquals("@미리비트 정진우 책임 미리 주문점여", result.cleanText)
        assertEquals(3, result.messages.size)
    }

    @Test
    fun `kakao talk group chat with 1 sender should keep sender name cleanly without awkward suffix`() {
        val msg = Bundle().apply {
            putCharSequence("sender", "김수환")
            putCharSequence("text", "브레이크가 고장나서 가장자리로 달리려던걸까")
            putLong("time", 1001L)
        }

        val sbn = createMockSbn(
            packageName = "com.kakao.talk",
            title = "김수환",
            text = "브레이크가 고장나서 가장자리로 달리려던걸까",
            isGroupConversation = true,
            messages = arrayOf(msg)
        )

        val result = MessengerNotificationParser.parse(sbn)
        assertTrue(result.isGroupChat)
        assertEquals("김수환", result.roomTitle)
        assertEquals("김수환", result.currentSender)
        assertEquals("브레이크가 고장나서 가장자리로 달리려던걸까", result.cleanText)
        assertEquals(1, result.messages.size)
    }
}
