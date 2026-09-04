package com.devdooly.notificationedge.util

import android.os.Build
import android.widget.RemoteViews
import java.util.concurrent.atomic.AtomicLong

/**
 * 제조사별 알림 Extras만으로 대화명이 부족할 때 사용하는 최후 수단 추출기.
 *
 * 숨겨진 `RemoteViews.mActions` 접근 실패는 기능 실패로 전파하지 않고 계수만 남긴다.
 */
object RemoteViewsTextExtractor {
    private const val TAG = "RemoteViewsExtractor"
    private val attemptCount = AtomicLong()
    private val failureCount = AtomicLong()

    data class Metrics(val attempts: Long, val failures: Long)

    fun extract(remoteViews: RemoteViews?): List<String> {
        if (remoteViews == null) return emptyList()
        attemptCount.incrementAndGet()
        val texts = mutableListOf<String>()
        try {
            val actionsField = remoteViews.javaClass.getDeclaredField("mActions")
            actionsField.isAccessible = true
            val actions = actionsField.get(remoteViews) as? List<*> ?: return emptyList()
            for (action in actions) {
                if (action == null) continue
                for (field in action.javaClass.declaredFields) {
                    field.isAccessible = true
                    val value = field.get(action)
                    if (value is CharSequence && value.isNotBlank()) {
                        val text = value.toString().trim()
                        if (text.length >= 2 && text !in texts) texts.add(text)
                    }
                }
            }
        } catch (error: Exception) {
            val failures = failureCount.incrementAndGet()
            AppLog.debug(
                TAG,
                "RemoteViews 보조 추출 실패(api=${Build.VERSION.SDK_INT}, 제조사=${Build.MANUFACTURER}, 누적=$failures)",
                error
            )
        }
        return texts
    }

    fun metrics(): Metrics = Metrics(
        attempts = attemptCount.get(),
        failures = failureCount.get()
    )
}
