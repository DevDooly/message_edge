package com.devdooly.notificationedge

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.devdooly.notificationedge.data.repository.SettingsRepository
import com.devdooly.notificationedge.service.EdgeOverlayService
import com.devdooly.notificationedge.ui.settings.SettingsScreen
import com.devdooly.notificationedge.ui.theme.NotificationEdgeTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        settingsRepository = SettingsRepository(applicationContext)

        setContent {
            NotificationEdgeTheme {
                SettingsScreen()
            }
        }

        checkAndStartService()
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
}
