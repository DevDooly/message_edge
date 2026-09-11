package com.devdooly.notificationedge.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.devdooly.notificationedge.ui.shortcuts.PanelShortcuts

/** 앱 업데이트·시스템 언어 변경 후에도 바로가기와 표시 이름을 복원한다. */
class ShortcutRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED || intent.action == Intent.ACTION_LOCALE_CHANGED) {
            PanelShortcuts.publish(context)
        }
    }
}
