package com.devdooly.notificationedge.ui.settings

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
import com.devdooly.notificationedge.R
import com.devdooly.notificationedge.data.model.AppSettings
import com.devdooly.notificationedge.data.repository.SettingsRepository
import com.devdooly.notificationedge.service.EdgeOverlayService
import com.devdooly.notificationedge.ui.theme.NotificationEdgeTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Notification Edge 설정 화면 전용 액티비티
 */
class SettingsActivity : ComponentActivity() {

    private lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsRepository = SettingsRepository(applicationContext)

        setTheme(R.style.Theme_NotificationEdge)
        window.setLayout(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        )
        enableEdgeToEdge()

        lifecycleScope.launch {
            val settings = settingsRepository.settingsFlow.first()
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

    override fun onResume() {
        super.onResume()
        checkAndStartService()
    }

    private fun checkAndStartService() {
        if (Settings.canDrawOverlays(this)) {
            lifecycleScope.launch {
                val settings = settingsRepository.settingsFlow.first()
                if (settings.isServiceEnabled || settings.isEdgeLightingEnabled) {
                    val serviceIntent = Intent(this@SettingsActivity, EdgeOverlayService::class.java)
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
