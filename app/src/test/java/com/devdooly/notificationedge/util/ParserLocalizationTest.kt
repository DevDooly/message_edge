package com.devdooly.notificationedge.util

import android.app.Application
import android.app.Notification
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.os.LocaleList
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import com.devdooly.notificationedge.data.model.EdgeNotification
import com.devdooly.notificationedge.data.model.NotificationLabel
import com.devdooly.notificationedge.data.repository.NotificationRepository
import com.devdooly.notificationedge.ui.overlay.resolve
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [26, 34])
class ParserLocalizationTest {
    @After
    fun tearDown() {
        NotificationRepository.clearAll()
    }

    @Test
    fun unnamedGroupKeepsStableKeyAndResolvesCurrentLanguage() {
        val result = parseNotification(title = "", text = "Hello", group = true)
        assertEquals("그룹 채팅방", result.roomTitle)
        assertEquals(NotificationLabel.GroupChat, result.roomTitleLabel)
        assertEquals("Group chat", result.roomTitleLabel!!.resolve(resourcesFor(Locale.ENGLISH)))
        assertEquals("그룹 채팅방", result.roomTitleLabel!!.resolve(resourcesFor(Locale.KOREAN)))
        assertEquals("Hello", result.cleanText)
    }

    @Test
    fun missingSenderIsLocalizedOnlyWhenParserGeneratedIt() {
        val result = parseNotification(title = "가족방", text = "저녁에 만나요", group = true, conversation = "가족방")
        assertEquals("가족방", result.roomTitle)
        assertNull(result.roomTitleLabel)
        assertEquals("상대방", result.currentSender)
        assertEquals(NotificationLabel.UnknownSender, result.currentSenderLabel)
        assertEquals(NotificationLabel.UnknownSender, result.messages.single().senderLabel)
        assertEquals("Someone", result.currentSenderLabel!!.resolve(resourcesFor(Locale.ENGLISH)))
        assertEquals("상대방", result.currentSenderLabel!!.resolve(resourcesFor(Locale.KOREAN)))
    }

    @Test
    fun realTitleEqualToFallbackTextIsNeverMarkedForTranslation() {
        val result = parseNotification(title = "그룹 채팅방", text = "My room name is 그룹 채팅방", group = true)
        assertEquals("그룹 채팅방", result.roomTitle)
        assertNull(result.roomTitleLabel)
        assertNull(result.groupRoomNameLabel)
        assertEquals("My room name is 그룹 채팅방", result.cleanText)
    }

    @Test
    fun realSenderEqualToFallbackTextIsNeverMarkedForTranslation() {
        val result = parseNotification(
            title = "가족방", text = "안녕하세요", group = true, conversation = "가족방",
            messages = arrayOf(message("상대방", "안녕하세요", 1_000L))
        )
        assertEquals("상대방", result.currentSender)
        assertNull(result.currentSenderLabel)
        assertEquals("상대방", result.messages.single().sender)
        assertNull(result.messages.single().senderLabel)
    }

    @Test
    fun participantSummaryLocalizesCountWithoutChangingNamesOrCanonicalKey() {
        val result = parseNotification(
            title = "", text = "Hello", group = true,
            messages = arrayOf(
                message("민수", "Hello", 1_000L), message("Alex", "Hello", 2_000L),
                message("Jane", "Hello", 3_000L), message("Mia", "Hello", 4_000L)
            )
        )
        assertEquals("민수, Alex, Jane 외 1명", result.roomTitle)
        val label = result.roomTitleLabel!!
        assertEquals("민수, Alex, Jane and 1 other", label.resolve(resourcesFor(Locale.ENGLISH)))
        assertEquals("민수, Alex, Jane 외 1명", label.resolve(resourcesFor(Locale.KOREAN)))
        assertEquals("민수, Alex, Jane and 2 others", NotificationLabel.SenderSummary(listOf("민수", "Alex", "Jane"), 2)
            .resolve(resourcesFor(Locale.ENGLISH)))
        assertTrue(result.messages.all { it.senderLabel == null })
    }

    @Test
    fun genericMessengerUsesTheSameParticipantSummaryMetadata() {
        val result = parseNotification(
            title = "", text = "Hello", group = true, packageName = "org.telegram.messenger",
            messages = arrayOf(
                message("A", "Hello", 1_000L), message("B", "Hello", 2_000L),
                message("C", "Hello", 3_000L), message("D", "Hello", 4_000L)
            )
        )
        assertEquals("A, B, C 외 1명", result.roomTitle)
        assertEquals("A, B, C and 1 other", result.roomTitleLabel!!.resolve(resourcesFor(Locale.ENGLISH)))
    }

    @Test
    fun selfIdentifierAndOriginalMessageStayUnchanged() {
        val result = parseNotification(
            title = "Alex", text = "Reply", group = false,
            messages = arrayOf(message("나", "상대방 and 그룹 채팅방 are literal text", 1_000L))
        )
        assertEquals("나", result.messages.single().sender)
        assertTrue(result.messages.single().isFromUser)
        assertNull(result.messages.single().senderLabel)
        assertEquals("상대방 and 그룹 채팅방 are literal text", result.messages.single().text)
    }

    @Test
    fun switchingDisplayLanguageDoesNotSplitTheSameConversationOrDuplicateHistory() {
        NotificationRepository.clearAll()
        val first = parseNotification(title = "", text = "First", group = true)
        val next = parseNotification(title = "", text = "Second", group = true)
        NotificationRepository.addOrUpdateNotification(toEdgeNotification("first", first))
        val cached = NotificationRepository.notifications.value.single()
        assertEquals("그룹 채팅방", cached.titleLabel!!.resolve(resourcesFor(Locale.KOREAN)))
        assertEquals("Group chat", cached.titleLabel!!.resolve(resourcesFor(Locale.ENGLISH)))
        NotificationRepository.addOrUpdateNotification(toEdgeNotification("second", next))
        val merged = NotificationRepository.notifications.value.single()
        assertEquals("그룹 채팅방", merged.title)
        assertEquals(listOf("First", "Second"), merged.messages.map { it.text })
        assertEquals(NotificationLabel.GroupChat, merged.titleLabel)
    }

    private fun toEdgeNotification(key: String, parsed: ParsedNotificationData) = EdgeNotification(
        key = key, id = 1, packageName = "com.kakao.talk", appName = "KakaoTalk",
        title = parsed.roomTitle, text = parsed.cleanText, subText = parsed.groupRoomName,
        messages = parsed.messages, isGroupChat = parsed.isGroupChat,
        titleLabel = parsed.roomTitleLabel, subTextLabel = parsed.groupRoomNameLabel
    )

    private fun resourcesFor(locale: Locale): Resources {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val configuration = Configuration(application.resources.configuration).apply { setLocales(LocaleList(locale)) }
        return application.createConfigurationContext(configuration).resources
    }

    private fun message(sender: String, text: String, time: Long) = Bundle().apply {
        putCharSequence("sender", sender)
        putCharSequence("text", text)
        putLong("time", time)
    }

    private fun parseNotification(
        title: String, text: String, group: Boolean, conversation: String? = null,
        messages: Array<Bundle>? = null, packageName: String = "com.kakao.talk"
    ): ParsedNotificationData {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val notification = NotificationCompat.Builder(context, "test")
            .setContentTitle(title).setContentText(text).build()
        notification.extras.putBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, group)
        if (conversation != null) notification.extras.putCharSequence(Notification.EXTRA_CONVERSATION_TITLE, conversation)
        if (messages != null) notification.extras.putParcelableArray(Notification.EXTRA_MESSAGES, messages)
        val sbn = mockk<StatusBarNotification>(relaxed = true)
        every { sbn.packageName } returns packageName
        every { sbn.notification } returns notification
        every { sbn.postTime } returns 1_000L
        return MessengerNotificationParser.parse(sbn)
    }
}
