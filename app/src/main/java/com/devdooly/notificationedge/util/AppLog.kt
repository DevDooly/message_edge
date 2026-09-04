package com.devdooly.notificationedge.util

import android.util.Log
import com.devdooly.notificationedge.BuildConfig

/**
 * 릴리스 로그에 예외 메시지나 스택 트레이스를 남기지 않는 공통 로거.
 */
object AppLog {
    fun warning(tag: String, event: String, error: Throwable? = null) {
        if (BuildConfig.DEBUG && error != null) {
            Log.w(tag, event, error)
        } else {
            Log.w(tag, failureSummary(event, error))
        }
    }

    fun debug(tag: String, event: String, error: Throwable? = null) {
        if (!BuildConfig.DEBUG) return
        if (error == null) {
            Log.d(tag, event)
        } else {
            Log.d(tag, event, error)
        }
    }

    internal fun failureSummary(event: String, error: Throwable?): String =
        if (error == null) event else "$event (${error.javaClass.simpleName})"
}
