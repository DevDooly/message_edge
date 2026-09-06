package com.devdooly.notificationedge

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.test.core.app.ApplicationProvider
import com.devdooly.notificationedge.ui.theme.AppFont
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** XML 내용뿐 아니라 Android가 실제로 선택하는 지역별 리소스도 검증한다. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 34])
class LocalizationTest {

    @Test
    fun `미국 영어 환경에서 영어 문구를 선택한다`() {
        assertEnglishResources(localizedContext("en-US"))
    }

    @Test
    fun `영국 영어 환경에서도 기본 영어 문구를 선택한다`() {
        assertEnglishResources(localizedContext("en-GB"))
    }

    @Test
    fun `한국어 환경에서 기존 한국어 문구를 선택한다`() {
        val context = localizedContext("ko-KR")

        assertEquals("Slivue", context.getString(R.string.app_name))
        assertEquals("화면은 그대로, 알림은 바로.", context.getString(R.string.app_tagline))
        assertEquals("엣지 핸들 및 패널 레이아웃", context.getString(R.string.appearance_handle_layout))
        assertEquals("필수 권한 설정", context.getString(R.string.settings_permissions_title))
        assertEquals("시스템 기본 서체", context.getString(AppFont.SYSTEM_DEFAULT.displayNameRes))
        assertEquals("핸들 투명도 (75%)", context.getString(R.string.appearance_handle_opacity, 75))
    }

    @Test
    fun `미지원 언어는 번역이 빠지지 않은 기본 영어로 대체한다`() {
        assertEnglishResources(localizedContext("fr-FR"))
    }

    @Test
    fun `영어가 한국어보다 앞선 언어 목록에서는 영어를 우선한다`() {
        assertEnglishResources(localizedContext("en-US,ko-KR"))
    }

    @Test
    fun `한국어가 영어보다 앞선 언어 목록에서는 한국어를 우선한다`() {
        val context = localizedContext("ko-KR,en-US")

        assertEquals("화면은 그대로, 알림은 바로.", context.getString(R.string.app_tagline))
        assertEquals("엣지 핸들 및 패널 레이아웃", context.getString(R.string.appearance_handle_layout))
    }

    @Test
    fun `영어는 단수와 복수를 구분하고 한국어는 수량을 동일하게 표시한다`() {
        val english = localizedContext("en-US")
        val korean = localizedContext("ko-KR")

        assertEquals("▼ 1 older message", english.resources.getQuantityString(R.plurals.panel_older_messages, 1, 1))
        assertEquals("▼ 2 older messages", english.resources.getQuantityString(R.plurals.panel_older_messages, 2, 2))
        assertEquals("▼ 이전 대화 1개 더보기", korean.resources.getQuantityString(R.plurals.panel_older_messages, 1, 1))
        assertEquals("▼ 이전 대화 2개 더보기", korean.resources.getQuantityString(R.plurals.panel_older_messages, 2, 2))
    }

    @Test
    fun `사용자 폰트 이름 인자는 번역하거나 변경하지 않는다`() {
        val originalName = "내 글꼴 My Font 123"

        assertEquals("Current: $originalName", localizedContext("en-US").getString(R.string.font_current, originalName))
        assertEquals("현재: $originalName", localizedContext("ko-KR").getString(R.string.font_current, originalName))
    }

    private fun assertEnglishResources(context: Context) {
        assertEquals("Slivue", context.getString(R.string.app_name))
        assertEquals("Stay on screen. Stay notified.", context.getString(R.string.app_tagline))
        assertEquals("Edge handle and panel layout", context.getString(R.string.appearance_handle_layout))
        assertEquals("Required permissions", context.getString(R.string.settings_permissions_title))
        assertEquals("System default", context.getString(AppFont.SYSTEM_DEFAULT.displayNameRes))
        assertEquals("Handle opacity (75%)", context.getString(R.string.appearance_handle_opacity, 75))
    }

    private fun localizedContext(languageTags: String): Context {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val configuration = Configuration(base.resources.configuration).apply {
            setLocales(LocaleList.forLanguageTags(languageTags))
        }
        return base.createConfigurationContext(configuration)
    }
}
