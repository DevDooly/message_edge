package com.devdooly.notificationedge.ui.theme

import android.content.Context
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.devdooly.notificationedge.R
import com.devdooly.notificationedge.util.CustomFontManager

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

enum class AppFont(
    val id: String,
    val displayName: String,
    val description: String
) {
    SYSTEM_DEFAULT(
        id = "default",
        displayName = "시스템 기본 서체",
        description = "One UI / 스마트폰 기본 시스템 폰트"
    ),
    NOTO_SANS_KR(
        id = "noto_sans_kr",
        displayName = "Noto Sans KR (본고딕)",
        description = "구글 표준의 또렷하고 정갈한 한글 폰트"
    ),
    IBM_PLEX_SANS_KR(
        id = "ibm_plex_sans_kr",
        displayName = "IBM Plex Sans KR",
        description = "한글과 영문 베이스라인이 정확히 일치하는 모던 서체"
    ),
    NANUM_GOTHIC(
        id = "nanum_gothic",
        displayName = "나눔고딕 (Nanum Gothic)",
        description = "부드럽고 친근한 라운드 코너 한글 고딕"
    ),
    GOWUN_DODUM(
        id = "gowun_dodum",
        displayName = "고운돋움 (Gowun Dodum)",
        description = "따뜻하고 편안한 감성의 라운드 서체"
    ),
    SERIF(
        id = "serif",
        displayName = "세리프 / 명조 (Serif)",
        description = "클래식하고 우아한 명조체"
    ),
    MONOSPACE(
        id = "monospace",
        displayName = "모노스페이스 (Monospace)",
        description = "글자 폭이 일정한 테크니컬 고정폭 폰트"
    );

    fun toFontFamily(): FontFamily {
        return when (this) {
            SYSTEM_DEFAULT -> FontFamily.Default
            NOTO_SANS_KR -> createGoogleFontFamily("Noto Sans KR")
            IBM_PLEX_SANS_KR -> createGoogleFontFamily("IBM Plex Sans KR")
            NANUM_GOTHIC -> createGoogleFontFamily("Nanum Gothic")
            GOWUN_DODUM -> createGoogleFontFamily("Gowun Dodum")
            SERIF -> FontFamily.Serif
            MONOSPACE -> FontFamily.Monospace
        }
    }

    companion object {
        fun fromId(id: String): AppFont {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: SYSTEM_DEFAULT
        }
    }
}

fun resolveFontFamily(context: Context, fontId: String): FontFamily {
    if (fontId.startsWith("custom:")) {
        val customFamily = CustomFontManager.loadFontFamily(context, fontId)
        if (customFamily != null) {
            return customFamily
        }
    }
    return AppFont.fromId(fontId).toFontFamily()
}

private fun createGoogleFontFamily(fontName: String): FontFamily {
    return FontFamily(
        Font(
            googleFont = GoogleFont(fontName),
            fontProvider = provider
        )
    )
}
