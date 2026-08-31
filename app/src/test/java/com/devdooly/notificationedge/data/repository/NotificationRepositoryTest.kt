package com.devdooly.notificationedge.data.repository

import com.devdooly.notificationedge.data.model.EdgeNotification
import com.devdooly.notificationedge.data.model.MessageItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class NotificationRepositoryTest {

    @Before
    fun setUp() {
        NotificationRepository.clearAll()
    }

    @Test
    fun `kakao group chat notifications from different senders should merge into same room card`() {
        // 1. 홍길동이 '우리 가족방'에 보낸 메시지
        val notif1 = EdgeNotification(
            key = "kakao_msg_1",
            id = 101,
            packageName = "com.kakao.talk",
            appName = "카카오톡",
            title = "우리 가족방",
            text = "오늘 저녁 뭐 먹어요?",
            messages = listOf(
                MessageItem(
                    sender = "홍길동",
                    text = "오늘 저녁 뭐 먹어요?",
                    timestamp = 1000L
                )
            ),
            timestamp = 1000L
        )
        NotificationRepository.addOrUpdateNotification(notif1)

        val listAfterFirst = NotificationRepository.notifications.value
        assertEquals(1, listAfterFirst.size)
        assertEquals("우리 가족방", listAfterFirst[0].title)
        assertEquals(1, listAfterFirst[0].messages.size)
        assertEquals("홍길동", listAfterFirst[0].messages[0].sender)

        // 2. 이순신이 같은 '우리 가족방'에 보낸 메시지 (새로운 알림 키로 도착)
        val notif2 = EdgeNotification(
            key = "kakao_msg_2",
            id = 102,
            packageName = "com.kakao.talk",
            appName = "카카오톡",
            title = "우리 가족방",
            text = "삼겹살 어때요?",
            messages = listOf(
                MessageItem(
                    sender = "이순신",
                    text = "삼겹살 어때요?",
                    timestamp = 2000L
                )
            ),
            timestamp = 2000L
        )
        NotificationRepository.addOrUpdateNotification(notif2)

        // 단체방 알림이 분리되지 않고 하나의 카드('우리 가족방')로 병합되었는지 검증!
        val listAfterSecond = NotificationRepository.notifications.value
        assertEquals(1, listAfterSecond.size)
        assertEquals("우리 가족방", listAfterSecond[0].title)
        assertEquals(2, listAfterSecond[0].messages.size)
        assertEquals("홍길동", listAfterSecond[0].messages[0].sender)
        assertEquals("오늘 저녁 뭐 먹어요?", listAfterSecond[0].messages[0].text)
        assertEquals("이순신", listAfterSecond[0].messages[1].sender)
        assertEquals("삼겹살 어때요?", listAfterSecond[0].messages[1].text)
    }

    @Test
    fun `direct 1-to-1 chats from different people should remain separate cards`() {
        val dm1 = EdgeNotification(
            key = "dm_1",
            id = 201,
            packageName = "com.kakao.talk",
            appName = "카카오톡",
            title = "김철수",
            text = "안녕",
            timestamp = 1000L
        )
        val dm2 = EdgeNotification(
            key = "dm_2",
            id = 202,
            packageName = "com.kakao.talk",
            appName = "카카오톡",
            title = "이영희",
            text = "반가워",
            timestamp = 2000L
        )

        NotificationRepository.addOrUpdateNotification(dm1)
        NotificationRepository.addOrUpdateNotification(dm2)

        val list = NotificationRepository.notifications.value
        assertEquals(2, list.size)
    }

    @Test
    fun `instagram reply notification with self reply should merge into existing direct chat card without duplication`() {
        val dm1 = EdgeNotification(
            key = "ig_dm_1",
            id = 301,
            packageName = "com.instagram.android",
            appName = "Instagram",
            title = "Feathers Mcgraw",
            text = "헉 변태다",
            messages = listOf(
                MessageItem(sender = "Feathers Mcgraw", text = "헉 변태다", timestamp = 1000L, isFromUser = false)
            ),
            timestamp = 1000L
        )
        NotificationRepository.addOrUpdateNotification(dm1)

        val dmSelfReply = EdgeNotification(
            key = "ig_dm_1",
            id = 301,
            packageName = "com.instagram.android",
            appName = "Instagram",
            title = "Feathers Mcgraw",
            text = "아니 왜 시부래 답장하니까 두번 떠 버그발견",
            messages = listOf(
                MessageItem(sender = "Feathers Mcgraw", text = "헉 변태다", timestamp = 1000L, isFromUser = false),
                MessageItem(sender = "나", text = "아니 왜 시부래 답장하니까 두번 떠 버그발견", timestamp = 2000L, isFromUser = true)
            ),
            timestamp = 2000L
        )
        NotificationRepository.addOrUpdateNotification(dmSelfReply)

        val list = NotificationRepository.notifications.value
        assertEquals(1, list.size)
        assertEquals("Feathers Mcgraw", list[0].title)
        assertEquals(2, list[0].messages.size)
    }

    @Test
    fun `toss securities different stock notifications with same subText should remain separate cards`() {
        val stock1 = EdgeNotification(
            key = "toss_stock_1",
            id = 401,
            packageName = "viva.republica.toss",
            appName = "토스증권",
            title = "SK이터닉스 📉",
            text = "주식 가격이 5% 떨어졌어요(53,000원)",
            subText = "토스증권",
            isGroupChat = false,
            timestamp = 1000L
        )
        val stock2 = EdgeNotification(
            key = "toss_stock_2",
            id = 402,
            packageName = "viva.republica.toss",
            appName = "토스증권",
            title = "KODEX SK하이닉스단일종목레버리지 📈",
            text = "주식 가격이 5% 올랐어요(10,590원)",
            subText = "토스증권",
            isGroupChat = false,
            timestamp = 2000L
        )

        NotificationRepository.addOrUpdateNotification(stock1)
        NotificationRepository.addOrUpdateNotification(stock2)

        val list = NotificationRepository.notifications.value
        // 서로 다른 종목이므로 subText("토스증권")가 같아도 별개의 2개 카드로 유지되어야 함!
        assertEquals(2, list.size)
        assertEquals("KODEX SK하이닉스단일종목레버리지 📈", list[0].title)
        assertEquals("SK이터닉스 📉", list[1].title)
    }
}
