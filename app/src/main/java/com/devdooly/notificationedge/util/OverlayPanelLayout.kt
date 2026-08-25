package com.devdooly.notificationedge.util

import android.content.Context
import android.os.Build
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout

/**
 * WindowManager 오버레이 윈도우에서 안드로이드 시스템 뒤로가기(BackKey/제스처) 이벤트를
 * Android 8.0부터 최신 Android 13/14(OnBackInvokedDispatcher)까지 100% 가로채어
 * 패널을 즉시 닫기 위한 루트 레이아웃
 */
class OverlayPanelLayout(context: Context) : FrameLayout(context) {

    var onBackPressed: (() -> Unit)? = null

    // Android 13+ (API 33, Tiramisu / Android 14) 예측 뒤로가기 콜백
    private var onBackInvokedCallback: Any? = null

    init {
        isFocusable = true
        isFocusableInTouchMode = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val callback = android.window.OnBackInvokedCallback {
                onBackPressed?.invoke()
            }
            onBackInvokedCallback = callback

            addOnAttachStateChangeListener(object : OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {
                    try {
                        findOnBackInvokedDispatcher()?.registerOnBackInvokedCallback(
                            android.window.OnBackInvokedDispatcher.PRIORITY_OVERLAY,
                            callback
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun onViewDetachedFromWindow(v: View) {
                    try {
                        findOnBackInvokedDispatcher()?.unregisterOnBackInvokedCallback(callback)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            })
        }
    }

    override fun dispatchKeyEventPreIme(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK || event.keyCode == KeyEvent.KEYCODE_ESCAPE) {
            if (event.action == KeyEvent.ACTION_UP) {
                onBackPressed?.invoke()
            }
            return true
        }
        return super.dispatchKeyEventPreIme(event)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK || event.keyCode == KeyEvent.KEYCODE_ESCAPE) {
            if (event.action == KeyEvent.ACTION_UP) {
                onBackPressed?.invoke()
            }
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
            onBackPressed?.invoke()
            return true
        }
        return super.onKeyUp(keyCode, event)
    }
}
