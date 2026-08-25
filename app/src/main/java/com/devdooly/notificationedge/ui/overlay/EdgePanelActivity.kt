package com.devdooly.notificationedge.ui.overlay

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import com.devdooly.notificationedge.data.model.AppSettings
import com.devdooly.notificationedge.data.repository.SettingsRepository
import com.devdooly.notificationedge.ui.settings.SettingsActivity
import com.devdooly.notificationedge.ui.theme.NotificationEdgeTheme

/**
 * 안드로이드 시스템 네비게이션 뒤로가기(하단 네비바 버튼, 양쪽 화면 제스처)를
 * 100.0% 완벽하게 지원하는 완전 투명 엣지 패널 호스트 액티비티
 */
class EdgePanelActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // OS 레벨의 뒤로가기 콜백 등록 (100% 보장)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                closeAndFinish()
            }
        })

        val settingsRepo = SettingsRepository(applicationContext)

        setContent {
            val settings by settingsRepo.settingsFlow.collectAsState(initial = AppSettings())

            NotificationEdgeTheme(
                fontId = settings.selectedFont
            ) {
                EdgePanelContent(
                    edgeSide = settings.edgeSide,
                    panelWidthDp = settings.panelWidthDp,
                    autoDismissOnOpen = settings.autoDismissOnOpen,
                    onClose = { closeAndFinish() },
                    onOpenSettings = {
                        val intent = Intent(this@EdgePanelActivity, SettingsActivity::class.java).apply {
                            putExtra(SettingsActivity.EXTRA_OPEN_SETTINGS, true)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        }
                        startActivity(intent)
                        closeAndFinish()
                    },
                    onRequestFocus = { /* Activity 호스팅 환경에서는 OS가 포커스 자동 관리 */ }
                )
            }
        }
    }

    private fun closeAndFinish() {
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    override fun onPause() {
        super.onPause()
        if (isFinishing) {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }
}
