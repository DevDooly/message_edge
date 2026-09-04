package com.devdooly.notificationedge.ui.overlay

import android.content.Context
import android.content.Intent

/** 패널 Activity의 열기·닫기·토글 정책을 모든 진입점에서 공유한다. */
object EdgePanelLauncher {

    fun open(context: Context) {
        if (EdgePanelActivity.isInstanceActive) return
        val intent = Intent(context, EdgePanelActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
        }
        context.startActivity(intent)
    }

    fun close() {
        EdgePanelActivity.closeActiveInstance()
    }

    fun toggle(context: Context) {
        if (EdgePanelActivity.isInstanceActive) close() else open(context)
    }
}
