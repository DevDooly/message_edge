package com.devdooly.notificationedge.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * Android 버전별 포그라운드 서비스 시작과 제한 예외 처리를 한 곳에서 관리한다.
 */
object OverlayServiceStarter {

    private const val TAG = "OverlayServiceStarter"

    fun start(context: Context, action: String? = null): Result<Unit> = runCatching {
        val serviceIntent = Intent(context, EdgeOverlayService::class.java).apply {
            this.action = action
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
        Unit
    }.onFailure { error ->
        Log.w(TAG, "오버레이 서비스를 시작하지 못했습니다: ${error.javaClass.simpleName}")
    }
}
