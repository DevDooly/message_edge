package com.devdooly.notificationedge.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

@Suppress("DEPRECATION")
private val DefaultPlatformTextStyle = PlatformTextStyle(
    includeFontPadding = false
)

private val DefaultLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

fun createTypography(fontFamily: FontFamily = FontFamily.Default): Typography {
    return Typography(
        titleLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            lineHeight = 26.sp,
            letterSpacing = (-0.2).sp,
            platformStyle = DefaultPlatformTextStyle,
            lineHeightStyle = DefaultLineHeightStyle
        ),
        titleMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            letterSpacing = 0.sp,
            platformStyle = DefaultPlatformTextStyle,
            lineHeightStyle = DefaultLineHeightStyle
        ),
        titleSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp,
            platformStyle = DefaultPlatformTextStyle,
            lineHeightStyle = DefaultLineHeightStyle
        ),
        bodyLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.15.sp,
            platformStyle = DefaultPlatformTextStyle,
            lineHeightStyle = DefaultLineHeightStyle
        ),
        bodyMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.15.sp,
            platformStyle = DefaultPlatformTextStyle,
            lineHeightStyle = DefaultLineHeightStyle
        ),
        bodySmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.3.sp,
            platformStyle = DefaultPlatformTextStyle,
            lineHeightStyle = DefaultLineHeightStyle
        ),
        labelLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.2.sp,
            platformStyle = DefaultPlatformTextStyle,
            lineHeightStyle = DefaultLineHeightStyle
        ),
        labelMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.4.sp,
            platformStyle = DefaultPlatformTextStyle,
            lineHeightStyle = DefaultLineHeightStyle
        ),
        labelSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.5.sp,
            platformStyle = DefaultPlatformTextStyle,
            lineHeightStyle = DefaultLineHeightStyle
        )
    )
}

val Typography = createTypography(FontFamily.Default)
