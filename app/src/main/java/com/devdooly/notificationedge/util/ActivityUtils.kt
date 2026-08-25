package com.devdooly.notificationedge.util

import android.app.Activity
import android.os.Build

/**
 * Android OS 버전별(API 26~34+) 액티비티 트랜지션 및 화면 전환 유틸리티
 */
object ActivityUtils {

    /**
     * 화면 깜빡임/전환 애니메이션 없이 0ms 즉시 실행/종료
     */
    fun overridePendingTransitionNoAnim(activity: Activity) {
        if (Build.VERSION.SDK_INT >= 34) {
            activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0)
            activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            activity.overridePendingTransition(0, 0)
        }
    }
}
