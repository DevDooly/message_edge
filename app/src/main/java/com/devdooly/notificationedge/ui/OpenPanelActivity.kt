package com.devdooly.notificationedge.ui

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import com.devdooly.notificationedge.MainActivity
import com.devdooly.notificationedge.service.EdgeOverlayService

/**
 * Good Lock (One Hand Operation +), 엣지 패널 바로가기, 런처 숏컷 등에서
 * 설정창 없이 알림 엣지 패널 오버레이만 즉시 열기 위한 투명 액티비티
 */
class OpenPanelActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Settings.canDrawOverlays(this)) {
            if (com.devdooly.notificationedge.ui.overlay.EdgePanelActivity.isInstanceActive) {
                com.devdooly.notificationedge.ui.overlay.EdgePanelActivity.closeActiveInstance()
            } else {
                com.devdooly.notificationedge.util.MediaControlHelper.pauseYouTubeOnly(this)
                val panelIntent = Intent(this, com.devdooly.notificationedge.ui.overlay.EdgePanelActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                startActivity(panelIntent)
            }
        } else {
            Toast.makeText(this, "Notification Edge 권한 설정이 필요합니다.", Toast.LENGTH_SHORT).show()
            val settingsIntent = Intent(this, com.devdooly.notificationedge.ui.settings.SettingsActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(settingsIntent)
        }

        finish()
        com.devdooly.notificationedge.util.ActivityUtils.overridePendingTransitionNoAnim(this)
    }
}
