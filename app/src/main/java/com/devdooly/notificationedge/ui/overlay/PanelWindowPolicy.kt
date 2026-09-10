package com.devdooly.notificationedge.ui.overlay

import android.graphics.Color
import android.os.Build
import android.view.Window
import android.view.WindowInsets
import android.view.WindowManager
import androidx.core.view.WindowCompat

/** 시스템 바는 아래 화면이 계속 그리도록 하고 패널 창은 안전 영역 안에 배치한다. */
@Suppress("DEPRECATION") // API 26~29에서도 이전 시스템 바 플래그를 명시적으로 해제한다.
internal fun configureTransparentPanelWindow(window: Window) {
    // Decor 생성 시 플랫폼이 지정하는 대화상자 크기를 먼저 확정한 뒤 전체 안전 영역을 사용한다.
    window.decorView.setPadding(0, 0, 0, 0)
    window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
    window.clearFlags(
        WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or
            WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or
            WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
    )
    WindowCompat.setDecorFitsSystemWindows(window, true)
    window.statusBarColor = Color.TRANSPARENT
    window.navigationBarColor = Color.TRANSPARENT
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        window.isStatusBarContrastEnforced = false
        window.isNavigationBarContrastEnforced = false
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        window.attributes = window.attributes.apply {
            setFitInsetsTypes(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            setFitInsetsSides(WindowInsets.Side.all())
            setFitInsetsIgnoringVisibility(false)
        }
    }
}
