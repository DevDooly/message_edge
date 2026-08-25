package com.devdooly.notificationedge.util

import android.content.Context
import android.view.KeyEvent
import android.widget.FrameLayout

/**
 * WindowManager 오버레이 윈도우에서 안드로이드 시스템 뒤로가기(BackKey) 이벤트를
 * 100% 신뢰성 있게 가로채기 위한 루트 레이아웃
 */
class OverlayPanelLayout(context: Context) : FrameLayout(context) {

    var onBackPressed: (() -> Boolean)? = null

    init {
        isFocusable = true
        isFocusableInTouchMode = true
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            if (event.action == KeyEvent.ACTION_UP) {
                val handled = onBackPressed?.invoke() ?: false
                return if (handled) true else super.dispatchKeyEvent(event)
            }
            return true // ACTION_DOWN 소비하여 하위/배경 윈도우로 빠져나가지 않도록 방지
        }
        return super.dispatchKeyEvent(event)
    }
}
