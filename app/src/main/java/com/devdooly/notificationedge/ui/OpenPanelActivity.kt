package com.devdooly.notificationedge.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import com.devdooly.notificationedge.R
import com.devdooly.notificationedge.ui.overlay.EdgePanelLauncher
import com.devdooly.notificationedge.ui.settings.SettingsActivity

/**
 * Good Lock (One Hand Operation +), 엣지 패널 바로가기, 런처 숏컷 등에서
 * 설정창 없이 알림 엣지 패널을 즉시 열거나 토글하기 위한 투명 액티비티
 */
class OpenPanelActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Settings.canDrawOverlays(this)) {
            EdgePanelLauncher.toggle(this)
        } else {
            Toast.makeText(this, getString(R.string.panel_permission_required), Toast.LENGTH_SHORT).show()
            val settingsIntent = Intent(this, SettingsActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(settingsIntent)
        }

        finish()
        com.devdooly.notificationedge.util.ActivityUtils.overridePendingTransitionNoAnim(this)
    }
}
