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
import org.junit.Assert.assertFalse
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
        isGroupConversation: Boolean? = null,
        selfDisplayName: String? = null,
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
        if (isGroupConversation != null) {
            notification.extras.putBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, isGroupConversation)
        }
        if (selfDisplayName != null) {
            notification.extras.putCharSequence(Notification.EXTRA_SELF_DISPLAY_NAME, selfDisplayName)
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
    fun `instagram 1-to-1 direct message dump should be parsed as direct chat with proper self reply identification`() {
        val msg0 = Bundle().apply {
            putCharSequence("sender", "병종")
            putCharSequence("text", "답장은 잘가나 단체방 뱃지는 뭐져")
            putLong("time", 1787717190698L)
        }
        val msg1 = Bundle().apply {
            putCharSequence("sender", "김선홍")
            putCharSequence("text", "안그래도 그거 수정중 ㅋㅋ")
            putLong("time", 1787717207929L)
        }
        val msg2 = Bundle().apply {
            putCharSequence("sender", "병종")
            putCharSequence("text", "ㅋㅋㅋㅋ")
            putLong("time", 1787717211688L)
        }
        val msg3 = Bundle().apply {
            putCharSequence("sender", "김선홍")
            putCharSequence("text", "글고 내가 보낸 내용도 남이 보낸것처럼 한번 더 보이네")
            putLong("time", 1787717258276L)
        }
        val msg4 = Bundle().apply {
            putCharSequence("sender", "병종")
            putCharSequence("text", "맞아여 그렇게 알림이 뜨더라구요")
            putLong("time", 1787717387797L)
        }

        val sbn = createMockSbn(
            packageName = "com.instagram.android",
            title = "sunhong2910: 병종",
            text = "맞아여 그렇게 알림이 뜨더라구요",
            isGroupConversation = false,
            selfDisplayName = "김선홍",
            messages = arrayOf(msg0, msg1, msg2, msg3, msg4)
        )

        val result = MessengerNotificationParser.parse(sbn)
        assertFalse(result.isGroupChat)
        assertEquals("병종", result.roomTitle)
        assertEquals(5, result.messages.size)
        assertFalse(result.messages[0].isFromUser)
        assertEquals("병종", result.messages[0].sender)
        assertTrue(result.messages[1].isFromUser)
        assertEquals("나", result.messages[1].sender)
        assertTrue(result.messages[3].isFromUser)
        assertEquals("나", result.messages[3].sender)
        assertFalse(result.messages[4].isFromUser)
        assertEquals("병종", result.messages[4].sender)
    }

    @Test
    fun `samsung messaging 1-to-1 message dump with self reply should remain direct chat and identify self messages`() {
        val msg0 = Bundle().apply {
            putCharSequence("sender", "전병종")
            putCharSequence("text", "테스트 해보기")
            putLong("time", 1787717176971L)
        }
        val msg1 = Bundle().apply {
            putCharSequence("text", "아아아아아아")
            putLong("time", 1787717281696L)
        }
        val msg2 = Bundle().apply {
            putCharSequence("text", "이건 뭔...")
            putLong("time", 1787717294823L)
        }

        val sbn = createMockSbn(
            packageName = "com.samsung.android.messaging",
            title = "전병종",
            subText = "전병종",
            text = "테스트 해보기",
            isGroupConversation = false,
            selfDisplayName = "나",
            messages = arrayOf(msg0, msg1, msg2)
        )

        val result = MessengerNotificationParser.parse(sbn)
        assertFalse(result.isGroupChat)
        assertEquals("전병종", result.roomTitle)
        assertEquals(3, result.messages.size)
        assertFalse(result.messages[0].isFromUser)
        assertEquals("전병종", result.messages[0].sender)
        assertTrue(result.messages[1].isFromUser)
        assertEquals("나", result.messages[1].sender)
        assertTrue(result.messages[2].isFromUser)
        assertEquals("나", result.messages[2].sender)
    }

    @Test
    fun `kakao talk group chat with channelName should extract group room title and sender properly`() {
        val msg0 = Bundle().apply {
            putCharSequence("sender", "김동관")
            putCharSequence("text", "속도 3, 4 정도로 걸음 유리는")
            putLong("time", 1787719594782L)
        }

        val sbn = createMockSbn(
            packageName = "com.kakao.talk",
            title = "김동관",
            text = "속도 3, 4 정도로 걸음 유리는",
            isGroupConversation = true,
            selfDisplayName = "김선홍",
            messages = arrayOf(msg0)
        )

        val result = MessengerNotificationParser.parse(
            sbn = sbn,
            channelName = "11단톡"
        )

        assertTrue(result.isGroupChat)
        assertEquals("11단톡", result.roomTitle)
        assertEquals("김동관", result.currentSender)
        assertEquals("속도 3, 4 정도로 걸음 유리는", result.cleanText)
        assertEquals(1, result.messages.size)
        assertEquals("김동관", result.messages[0].sender)
        assertFalse(result.messages[0].isFromUser)
    }

    @Test
    fun `kakao talk group chat with system channel like 알림 받지 않는 메시지 should be ignored and fallback to sender`() {
        val msg0 = Bundle().apply {
            putCharSequence("sender", "김동관")
            putCharSequence("text", "속도 3, 4 정도로 걸음 유리는")
            putLong("time", 1787719594782L)
        }

        val sbn = createMockSbn(
            packageName = "com.kakao.talk",
            title = "김동관",
            text = "속도 3, 4 정도로 걸음 유리는",
            isGroupConversation = true,
            selfDisplayName = "김선홍",
            messages = arrayOf(msg0)
        )

        val result = MessengerNotificationParser.parse(
            sbn = sbn,
            channelName = "알림 받지 않는 메시지"
        )

        assertTrue(result.isGroupChat)
        assertEquals("김동관", result.roomTitle) // 시스템 채널명 "알림 받지 않는 메시지"는 무시되어 발신자 이름 유지
        assertEquals("김동관", result.currentSender)
        assertEquals("속도 3, 4 정도로 걸음 유리는", result.cleanText)
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

    @Test
    fun `toss securities non-chat notification should not be parsed as group chat and should have empty messages`() {
        val sbn = createMockSbn(
            packageName = "viva.republica.toss",
            title = "KODEX SK하이닉스단일종목레버리지 📈",
            text = "주식 가격이 5% 올랐어요(10,590원)",
            subText = "토스증권"
        )

        val result = MessengerNotificationParser.parse(sbn)
        assertFalse(result.isGroupChat)
        assertNull(result.groupRoomName)
        assertEquals("KODEX SK하이닉스단일종목레버리지 📈", result.roomTitle)
        assertEquals("주식 가격이 5% 올랐어요(10,590원)", result.cleanText)
        assertTrue(result.messages.isEmpty())
    }
}
