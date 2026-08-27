package com.devdooly.notificationedge.ui.overlay

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.devdooly.notificationedge.data.model.AppSettings
import com.devdooly.notificationedge.data.repository.SettingsRepository
import com.devdooly.notificationedge.ui.settings.SettingsActivity
import com.devdooly.notificationedge.ui.theme.NotificationEdgeTheme
import com.devdooly.notificationedge.util.MediaControlHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 안드로이드 시스템 네비게이션 뒤로가기(하단 네비바 버튼, 양쪽 화면 제스처)를
 * 100.0% 완벽하게 지원하고 상단 상태바(배터리 바)/하단 네비바의 배경을 완전 투명하게 유지하는 호스트 액티비티
 */
class EdgePanelActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val isSystemDark = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES

        // 완전 투명 Zero-Scrim Edge-to-Edge 활성화 및 시스템 테마(다크/라이트)에 맞춘 아이콘 색상 동기화
        enableEdgeToEdge(
            statusBarStyle = if (isSystemDark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = if (isSystemDark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }

        // 상태바 및 네비게이션바 글씨/아이콘 색상을 시스템 원래 테마와 100% 동일하게 유지 (라이트 모드: 검정 글씨, 다크 모드: 흰색 글씨)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = !isSystemDark
        insetsController.isAppearanceLightNavigationBars = !isSystemDark

        // OS 레벨의 뒤로가기 콜백 등록 (100% 보장)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                closeAndFinish()
            }
        })

        val settingsRepo = SettingsRepository(applicationContext)

        // 패널 오픈 시 활성 미디어(유튜브/음악 등) 일시 정지 트리거
        lifecycleScope.launch {
            val settings = settingsRepo.settingsFlow.first()
            if (settings.pauseMediaOnOpen) {
                MediaControlHelper.pauseActiveMedia(this@EdgePanelActivity)
            }
        }

        setContent {
            val settings by settingsRepo.settingsFlow.collectAsState(initial = AppSettings())

            NotificationEdgeTheme(
                fontId = settings.selectedFont,
                transparentStatusBar = true
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
        com.devdooly.notificationedge.util.ActivityUtils.overridePendingTransitionNoAnim(this)
    }

    override fun onPause() {
        super.onPause()
        if (isFinishing) {
            com.devdooly.notificationedge.util.ActivityUtils.overridePendingTransitionNoAnim(this)
        }
    }
}
