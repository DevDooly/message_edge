package com.devdooly.notificationedge.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.devdooly.notificationedge.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver {
    // Android Manifest용 기본 생성자
    constructor() : super()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // 오버레이 권한이 허용되어 있는지 확인
            if (Settings.canDrawOverlays(context)) {
                val repository = SettingsRepository(context)
                CoroutineScope(Dispatchers.IO).launch {
                    val settings = repository.settingsFlow.first()
                    if (settings.isServiceEnabled) {
                        val serviceIntent = Intent(context, EdgeOverlayService::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }
                    }
                }
            }
        }
    }
}
