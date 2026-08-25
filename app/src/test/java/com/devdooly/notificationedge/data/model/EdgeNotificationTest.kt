package com.devdooly.notificationedge.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EdgeNotificationTest {

    @Test
    fun `create EdgeNotification with default values`() {
        val notification = EdgeNotification(
            key = "key_1",
            id = 100,
            packageName = "com.kakao.talk",
            appName = "카카오톡",
            title = "홍길동",
            text = "안녕하세요"
        )

        assertEquals("key_1", notification.key)
        assertEquals(100, notification.id)
        assertEquals("com.kakao.talk", notification.packageName)
        assertEquals("카카오톡", notification.appName)
        assertEquals("홍길동", notification.title)
        assertEquals("안녕하세요", notification.text)
        assertTrue(notification.isClearable)
        assertFalse(notification.isDismissed)
        assertFalse(notification.isGroupHeader)
        assertTrue(notification.messages.isEmpty())
        assertTrue(notification.actions.isEmpty())
    }

    @Test
    fun `create MessageItem with user flag`() {
        val userMsg = MessageItem(
            sender = "나",
            text = "답장입니다.",
            timestamp = 1000L,
            isFromUser = true
        )
        val otherMsg = MessageItem(
            sender = "상대방",
            text = "안녕하세요",
            timestamp = 900L,
            isFromUser = false
        )

        assertTrue(userMsg.isFromUser)
        assertFalse(otherMsg.isFromUser)
        assertEquals("나", userMsg.sender)
        assertEquals("상대방", otherMsg.sender)
    }

    @Test
    fun `NotificationActionItem reply flag`() {
        val replyAction = NotificationActionItem(
            title = "답장",
            actionIntent = null,
            isReply = true,
            remoteInputKey = "key_text_reply"
        )

        assertTrue(replyAction.isReply)
        assertEquals("답장", replyAction.title)
        assertEquals("key_text_reply", replyAction.remoteInputKey)
    }
}
