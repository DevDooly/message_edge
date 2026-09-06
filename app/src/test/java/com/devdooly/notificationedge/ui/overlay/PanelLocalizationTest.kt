package com.devdooly.notificationedge.ui.overlay

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.icu.text.DateTimePatternGenerator
import android.os.LocaleList
import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.test.core.app.ApplicationProvider
import com.devdooly.notificationedge.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import java.util.Calendar
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(
    application = Application::class,
    sdk = [26, 34],
    shadows = [PanelLocalizationTest.Api26DatePatternShadow::class]
)
class PanelLocalizationTest {

    @Test
    fun panelActionsUseTheRequestedLanguage() {
        assertEquals("Reply", contextFor(Locale.ENGLISH).getString(R.string.panel_reply))
        assertEquals("답장", contextFor(Locale.KOREAN).getString(R.string.panel_reply))
        assertEquals("Me: ", contextFor(Locale.ENGLISH).getString(R.string.panel_me_prefix))
        assertEquals("나: ", contextFor(Locale.KOREAN).getString(R.string.panel_me_prefix))
    }

    @Test
    fun unsupportedLanguageFallsBackToEnglish() {
        assertEquals("Open settings", contextFor(Locale.FRENCH).getString(R.string.panel_open_settings))
    }

    @Test
    fun olderMessageCountsUseEnglishPluralRulesAndKeepKoreanText() {
        val english = contextFor(Locale.ENGLISH).resources
        val korean = contextFor(Locale.KOREAN).resources
        assertEquals("▼ 1 older message", english.getQuantityString(R.plurals.panel_older_messages, 1, 1))
        assertEquals("▼ 2 older messages", english.getQuantityString(R.plurals.panel_older_messages, 2, 2))
        assertEquals("▼ 이전 대화 2개 더보기", korean.getQuantityString(R.plurals.panel_older_messages, 2, 2))
    }

    @Test
    fun timeUsesEnglishAndKoreanDayPeriods() {
        val now = timestamp(6, 16, 0)
        val messageTime = timestamp(6, 15, 24)
        val english = formatMessageTime(messageTime, Locale.US, false, now)
        val korean = formatMessageTime(messageTime, Locale.KOREA, false, now)
        assertTrue(english, english.contains("3:24") && english.contains("PM"))
        assertFalse(english.contains("오후"))
        assertTrue(korean, korean.contains("3:24") && korean.contains("오후"))
    }

    @Test
    fun timeHonors24HourPreferenceInBothLanguages() {
        val now = timestamp(6, 16, 0)
        val messageTime = timestamp(6, 15, 24)
        assertEquals("15:24", formatMessageTime(messageTime, Locale.US, true, now))
        assertEquals("15:24", formatMessageTime(messageTime, Locale.KOREA, true, now))
    }

    @Test
    fun earlierDayIncludesDateAndInvalidTimeIsEmpty() {
        val now = timestamp(6, 16, 0)
        val previousDay = formatMessageTime(timestamp(5, 15, 24), Locale.US, true, now)
        assertTrue(previousDay, previousDay.contains("9/5") && previousDay.contains("15:24"))
        assertEquals("", formatMessageTime(0, Locale.US, true, now))
        assertEquals("", formatMessageTime(-1, Locale.KOREA, false, now))
    }

    @Test
    fun relativeTimeUsesResourceLocaleNotTheProcessDefault() {
        val now = timestamp(6, 16, 0)
        val earlier = now - 2 * DateUtils.MINUTE_IN_MILLIS
        assertEquals("2 minutes ago", formatPanelRelativeTime(contextFor(Locale.ENGLISH), earlier, now))
        assertEquals("2분 전", formatPanelRelativeTime(contextFor(Locale.KOREAN), earlier, now))
        assertEquals("Just now", formatPanelRelativeTime(contextFor(Locale.ENGLISH), now, now))
        assertEquals("방금", formatPanelRelativeTime(contextFor(Locale.KOREAN), now, now))
    }

    @Test
    fun replyTargetContentIsNotTranslated() {
        val target = "가족 채팅 😊"
        assertEquals("Reply to: $target", contextFor(Locale.ENGLISH).getString(R.string.panel_reply_to, target))
        assertEquals("답장: $target", contextFor(Locale.KOREAN).getString(R.string.panel_reply_to, target))
    }

    private fun contextFor(locale: Locale): Context {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val configuration = Configuration(application.resources.configuration).apply {
            setLocales(LocaleList(locale))
        }
        return application.createConfigurationContext(configuration)
    }

    private fun timestamp(day: Int, hour: Int, minute: Int): Long = Calendar.getInstance().apply {
        clear()
        set(2026, Calendar.SEPTEMBER, day, hour, minute)
    }.timeInMillis

    /**
     * Robolectric 4.13의 API 26 ShadowICU는 Hm/hm/MdHm을 처리하지 않고 입력값을 그대로 반환한다.
     * 실제 Android 8의 DateFormat은 ICU로 패턴을 생성하므로, 이 네이티브 경계만 Android ICU의
     * 실제 패턴 생성기로 연결한다. 앱의 포맷 함수 및 기대값은 대체하지 않으며 API 34에는 적용하지 않는다.
     * 근거: https://github.com/robolectric/robolectric/blob/robolectric-4.13/shadows/framework/src/main/java/org/robolectric/shadows/ShadowICU.java
     */
    @Implements(value = DateFormat::class, maxSdk = 26)
    class Api26DatePatternShadow {
        companion object {
            @JvmStatic
            @Implementation
            fun getBestDateTimePattern(locale: Locale, skeleton: String): String =
                DateTimePatternGenerator.getInstance(locale).getBestPattern(skeleton)
        }
    }
}
