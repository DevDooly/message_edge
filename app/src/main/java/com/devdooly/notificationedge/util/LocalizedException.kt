package com.devdooly.notificationedge.util

import android.content.Context
import androidx.annotation.StringRes

/** 작업 실패의 원인과 표시 언어를 분리해 UI의 현재 언어로 오류를 해석한다. */
internal class LocalizedException(
    @StringRes val messageRes: Int,
    vararg val formatArgs: Any
) : IllegalStateException("Slivue message resource: $messageRes")

internal fun requireLocalized(condition: Boolean, @StringRes messageRes: Int, vararg args: Any) {
    if (!condition) throw LocalizedException(messageRes, *args)
}

/** 네트워크/플랫폼 예외 원문 대신 번역된 안내를 제공한다. */
internal fun Throwable.userMessage(context: Context, @StringRes fallback: Int): String =
    if (this is LocalizedException) context.getString(messageRes, *formatArgs)
    else context.getString(fallback)
