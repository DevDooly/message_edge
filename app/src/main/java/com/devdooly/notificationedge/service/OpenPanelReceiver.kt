package com.devdooly.notificationedge.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.devdooly.notificationedge.MainActivity

/**
 * Good Lock(One Hand Operation +), Tasker, 숏컷 등에서
 * 액티비티 전환 없이(유튜브/미디어 재생 PiP 전환 방지)
 * 엣지 패널 오버레이만 즉시 열기 위한 브로드캐스트 리시버
 */
class OpenPanelReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Settings.canDrawOverlays(context)) {
            val serviceAction = when (intent.action) {
                ACTION_CLOSE_PANEL, "com.devdooly.notificationedge.ACTION_CLOSE_PANEL" -> EdgeOverlayService.ACTION_CLOSE_PANEL
                ACTION_TOGGLE_PANEL, "com.devdooly.notificationedge.ACTION_TOGGLE_PANEL" -> EdgeOverlayService.ACTION_TOGGLE_PANEL
                else -> EdgeOverlayService.ACTION_OPEN_PANEL
            }
            val serviceIntent = Intent(context, EdgeOverlayService::class.java).apply {
                action = serviceAction
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } else {
            val settingsIntent = Intent(context, com.devdooly.notificationedge.ui.settings.SettingsActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(settingsIntent)
        }
    }

    companion object {
        const val ACTION_OPEN_PANEL = "com.devdooly.notificationedge.OPEN_PANEL"
        const val ACTION_CLOSE_PANEL = "com.devdooly.notificationedge.CLOSE_PANEL"
        const val ACTION_TOGGLE_PANEL = "com.devdooly.notificationedge.TOGGLE_PANEL"
    }
}
