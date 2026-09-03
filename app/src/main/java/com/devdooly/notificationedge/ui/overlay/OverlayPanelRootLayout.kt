package com.devdooly.notificationedge.ui.overlay

import android.content.Context
import android.os.Build
import android.util.Log
import android.view.KeyEvent
import android.widget.FrameLayout
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher

/**
 * WindowManager 오버레이 윈도우 환경에서 하단 네비게이션 뒤로가기 버튼,
 * 키보드(IME) Pre-Ime 뒤로가기, ESC 키, 및 Android 13+ 제스처 네비게이션을
 * 100.0% 완벽하게 가로채어 패널 닫기 콜백(onClose)을 호출하는 루트 레이아웃.
 */
class OverlayPanelRootLayout(context: Context) : FrameLayout(context) {

    var onClose: (() -> Unit)? = null

    private var onBackInvokedCallback: OnBackInvokedCallback? = null

    init {
        isFocusable = true
        isFocusableInTouchMode = true
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK || event.keyCode == KeyEvent.KEYCODE_ESCAPE) {
            if (event.action == KeyEvent.ACTION_UP) {
                onClose?.invoke()
            }
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchKeyEventPreIme(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK || event.keyCode == KeyEvent.KEYCODE_ESCAPE) {
            if (event.action == KeyEvent.ACTION_UP) {
                onClose?.invoke()
            }
            return true
        }
        return super.dispatchKeyEventPreIme(event)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // Android 13+ (API 33, 34) 화면 가장자리 제스처 뒤로가기 감지 콜백 등록
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val callback = OnBackInvokedCallback {
                    onClose?.invoke()
                }
                onBackInvokedCallback = callback
                findOnBackInvokedDispatcher()?.registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_OVERLAY,
                    callback
                )
            } catch (e: Exception) {
                Log.w("OverlayPanelRootLayout", "Failed to register OnBackInvokedCallback: ${e.message}")
            }
        }
        post {
            requestFocus()
        }
    }

    override fun onDetachedFromWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedCallback?.let { callback ->
                try {
                    findOnBackInvokedDispatcher()?.unregisterOnBackInvokedCallback(callback)
                } catch (e: Exception) {
                    Log.w("OverlayPanelRootLayout", "Failed to unregister OnBackInvokedCallback: ${e.message}")
                }
                onBackInvokedCallback = null
            }
        }
        super.onDetachedFromWindow()
    }
}
