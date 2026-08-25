package com.devdooly.notificationedge.util

import android.content.Context
import android.view.KeyEvent
import android.widget.FrameLayout

/**
 * WindowManager 오버레이 윈도우에서 안드로이드 시스템 뒤로가기(BackKey/제스처) 이벤트를
 * 100% 가로채어 패널을 즉시 닫기 위한 루트 레이아웃
 */
class OverlayPanelLayout(context: Context) : FrameLayout(context) {

    var onBackPressed: (() -> Unit)? = null

    init {
        isFocusable = true
        isFocusableInTouchMode = true
    }

    override fun dispatchKeyEventPreIme(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            if (event.action == KeyEvent.ACTION_UP) {
                onBackPressed?.invoke()
            }
            return true
        }
        return super.dispatchKeyEventPreIme(event)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            if (event.action == KeyEvent.ACTION_UP) {
                onBackPressed?.invoke()
            }
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}
