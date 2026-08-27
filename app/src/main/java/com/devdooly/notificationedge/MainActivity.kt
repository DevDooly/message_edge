package com.devdooly.notificationedge

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import com.devdooly.notificationedge.data.repository.SettingsRepository
import com.devdooly.notificationedge.service.EdgeOverlayService
import com.devdooly.notificationedge.ui.settings.SettingsActivity

/**
 * 런처 아이콘 / Good Lock / 제스처 실행 시 윈도우 생성 없이 0.0001초 만에 엣지 패널을 띄우는 무화면 트램펄린 액티비티
 * (유튜브/비디오 재생 중 실행해도 윈도우 전환이 발생하지 않아 PiP 전환이 100% 발생하지 않음)
 */
class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val settingsRepository = SettingsRepository(applicationContext)
        val openSettings = intent.getBooleanExtra(EXTRA_OPEN_SETTINGS, false)

        if (!openSettings && Settings.canDrawOverlays(this) && settingsRepository.isLaunchDirectToPanelSync()) {
            com.devdooly.notificationedge.util.MediaControlHelper.pauseYouTubeOnly(this)
            val panelIntent = Intent(this, com.devdooly.notificationedge.ui.overlay.EdgePanelActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(panelIntent)
        } else {
            val settingsIntent = Intent(this, SettingsActivity::class.java).apply {
                if (openSettings) putExtra(SettingsActivity.EXTRA_OPEN_SETTINGS, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(settingsIntent)
        }

        finish()
        com.devdooly.notificationedge.util.ActivityUtils.overridePendingTransitionNoAnim(this)
    }

    companion object {
        const val EXTRA_OPEN_SETTINGS = "extra_open_settings"
    }
}
