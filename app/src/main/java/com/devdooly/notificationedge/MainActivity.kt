package com.devdooly.notificationedge

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.devdooly.notificationedge.data.repository.SettingsRepository
import com.devdooly.notificationedge.service.EdgeOverlayService
import com.devdooly.notificationedge.ui.settings.SettingsScreen
import com.devdooly.notificationedge.ui.theme.AppFont
import com.devdooly.notificationedge.ui.theme.NotificationEdgeTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        val openSettings = intent.getBooleanExtra(EXTRA_OPEN_SETTINGS, false)
        settingsRepository = SettingsRepository(applicationContext)

        // 1. 오버레이 권한이 있고, 설정 화면 요청이 아니며, 바로 열기 모드가 활성화된 경우:
        // 비동기 코루틴 없이 0ms 동기로 즉시 서비스 호출하고 액티비티 즉시 종료 (유튜브 PiP 전환 100% 방지)
        if (!openSettings && Settings.canDrawOverlays(this) && settingsRepository.isLaunchDirectToPanelSync()) {
            val serviceIntent = Intent(this, EdgeOverlayService::class.java).apply {
                action = EdgeOverlayService.ACTION_OPEN_PANEL
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            super.onCreate(savedInstanceState)
            finish()
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
            return
        }

        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            val settings = settingsRepository.settingsFlow.first()

            // 2. 설정 화면 표시
            setTheme(R.style.Theme_NotificationEdge)
            window.setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
            enableEdgeToEdge()
            setContent {
                val liveSettings by settingsRepository.settingsFlow.collectAsState(initial = settings)
                NotificationEdgeTheme(
                    fontId = liveSettings.selectedFont
                ) {
                    SettingsScreen()
                }
            }

            checkAndStartService()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val openSettings = intent.getBooleanExtra(EXTRA_OPEN_SETTINGS, false)
        if (!openSettings && Settings.canDrawOverlays(this) && settingsRepository.isLaunchDirectToPanelSync()) {
            val serviceIntent = Intent(this, EdgeOverlayService::class.java).apply {
                action = EdgeOverlayService.ACTION_OPEN_PANEL
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            finish()
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    override fun onResume() {
        super.onResume()
        checkAndStartService()
    }

    private fun checkAndStartService() {
        if (Settings.canDrawOverlays(this)) {
            lifecycleScope.launch {
                val settings = settingsRepository.settingsFlow.first()
                if (settings.isServiceEnabled) {
                    val serviceIntent = Intent(this@MainActivity, EdgeOverlayService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent)
                    } else {
                        startService(serviceIntent)
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_OPEN_SETTINGS = "extra_open_settings"
    }
}
