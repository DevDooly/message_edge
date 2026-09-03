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
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

        // 완전 투명 Zero-Scrim Edge-to-Edge 활성화 및 실행 시점 원래 색상 유지
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { isSystemDark },
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { isSystemDark }
        )
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }

        // OS 레벨의 뒤로가기 콜백 등록 (100% 보장)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                closeAndFinish()
            }
        })

        val settingsRepo = SettingsRepository.getInstance(applicationContext)

        // 패널 오픈 시 유튜브 재생 일시 정지 트리거 (유튜브 뮤직 제외)
        lifecycleScope.launch {
            val settings = settingsRepo.settingsFlow.first()
            if (settings.pauseMediaOnOpen) {
                MediaControlHelper.pauseYouTubeOnly(this@EdgePanelActivity)
            }
        }

        setContent {
            val settings by settingsRepo.settingsFlow.collectAsStateWithLifecycle(initialValue = AppSettings())

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

    override fun onResume() {
        super.onResume()
        activeInstance = this
        isInstanceActive = true
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // 이미 열려 있는 상태에서 다시 호출되면 즉시 토글되어 닫힘
        closeAndFinish()
    }

    private fun closeAndFinish() {
        finish()
        com.devdooly.notificationedge.util.ActivityUtils.overridePendingTransitionNoAnim(this)
    }

    override fun onPause() {
        super.onPause()
        if (activeInstance == this) {
            isInstanceActive = false
        }
        if (isFinishing) {
            com.devdooly.notificationedge.util.ActivityUtils.overridePendingTransitionNoAnim(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (activeInstance == this) {
            activeInstance = null
            isInstanceActive = false
        }
    }

    companion object {
        @Volatile
        var isInstanceActive: Boolean = false
            private set

        private var activeInstance: EdgePanelActivity? = null

        /**
         * 현재 열려 있는 EdgePanelActivity 인스턴스가 있다면 즉시 닫고 true 반환
         */
        fun closeActiveInstance(): Boolean {
            val instance = activeInstance
            if (instance != null && !instance.isFinishing) {
                instance.closeAndFinish()
                return true
            }
            return false
        }
    }
}
