package com.devdooly.notificationedge

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.devdooly.notificationedge.ui.settings.SettingsActivity

/**
 * 앱 런처 아이콘 클릭 시 설정 및 온보딩 대시보드 화면(SettingsActivity)으로 이동
 */
class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val settingsIntent = Intent(this, SettingsActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(settingsIntent)
        finish()
    }
}
