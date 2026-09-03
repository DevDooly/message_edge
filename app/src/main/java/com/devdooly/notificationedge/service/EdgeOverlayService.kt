package com.devdooly.notificationedge.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color as AndroidColor
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import com.devdooly.notificationedge.MainActivity
import com.devdooly.notificationedge.R
import com.devdooly.notificationedge.data.model.AppSettings
import com.devdooly.notificationedge.data.model.EdgeSide
import com.devdooly.notificationedge.data.repository.NotificationRepository
import com.devdooly.notificationedge.data.repository.SettingsRepository
import com.devdooly.notificationedge.ui.overlay.EdgeLightingEffect
import com.devdooly.notificationedge.ui.overlay.EdgePanelContent
import com.devdooly.notificationedge.ui.overlay.OverlayPanelRootLayout
import com.devdooly.notificationedge.ui.settings.SettingsActivity
import com.devdooly.notificationedge.ui.theme.NotificationEdgeTheme
import com.devdooly.notificationedge.util.MediaControlHelper
import com.devdooly.notificationedge.util.OverlayLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 화면 측면 플로팅 핸들(Handle) 및 엣지 라이팅(Edge Lighting)을 백그라운드에서 관리하는 포그라운드 서비스
 */
class EdgeOverlayService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var windowManager: WindowManager
    private lateinit var settingsRepository: SettingsRepository

    private var handleView: View? = null
    private var panelComposeView: View? = null
    private var panelLifecycleOwner: OverlayLifecycleOwner? = null
    private var lightingComposeView: ComposeView? = null
    private var lightingLifecycleOwner: OverlayLifecycleOwner? = null
    private var systemDialogReceiver: BroadcastReceiver? = null

    private var currentSettings = AppSettings()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        settingsRepository = SettingsRepository.getInstance(applicationContext)

        startForegroundNotification()
        observeSettings()
        observeNewNotifications()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_OPEN_PANEL -> {
                triggerHaptic()
                openPanel()
            }
            ACTION_CLOSE_PANEL -> {
                closePanel()
            }
            ACTION_TOGGLE_PANEL -> {
                triggerHaptic()
                togglePanel()
            }
        }
        return START_STICKY
    }

    private fun startForegroundNotification() {
        val channelId = "notification_edge_service_channel"
        val channelName = "Notification Edge Service"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "알림 엣지 서비스 백그라운드 실행"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.overlay_service_title))
            .setContentText(getString(R.string.overlay_service_desc))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun observeSettings() {
        serviceScope.launch {
            settingsRepository.settingsFlow.collectLatest { settings ->
                currentSettings = settings
                if (settings.isServiceEnabled) {
                    updateHandleView()
                } else {
                    removeHandleView()
                }

                // 엣지 핸들과 엣지 라이팅이 둘 다 꺼진 경우에만 서비스 자체를 종료
                if (!settings.isServiceEnabled && !settings.isEdgeLightingEnabled) {
                    stopSelf()
                }
            }
        }
    }

    private fun observeNewNotifications() {
        serviceScope.launch {
            NotificationRepository.newNotificationEvent.collect { notification ->
                // 엣지 핸들(마스터 스위치) 활성화 여부와 무관하게, 엣지 라이팅 설정이 켜져 있으면 라이팅 발동
                if (currentSettings.isEdgeLightingEnabled) {
                    if (!currentSettings.excludedPackages.contains(notification.packageName)) {
                        triggerHaptic()
                        showEdgeLighting()
                    }
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun triggerHaptic() {
        if (!currentSettings.hapticFeedbackEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(40)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun updateHandleView() {
        val density = resources.displayMetrics.density
        val screenHeight = resources.displayMetrics.heightPixels

        val widthPx = (currentSettings.handleWidthDp * density).toInt()
        val heightPx = (currentSettings.handleHeightDp * density).toInt()
        val yPosPx = (screenHeight * currentSettings.handlePositionRatio - heightPx / 2f).toInt()

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val gravity = if (currentSettings.edgeSide == EdgeSide.RIGHT) {
            Gravity.TOP or Gravity.END
        } else {
            Gravity.TOP or Gravity.START
        }

        val params = WindowManager.LayoutParams(
            widthPx,
            heightPx,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            this.gravity = gravity
            this.x = 0
            this.y = yPosPx
        }

        val effectiveAlpha = if (currentSettings.isHandleVisible) {
            (currentSettings.handleAlpha * 255).toInt()
        } else {
            0 // 완전히 투명하게 숨김 (터치는 가능)
        }

        if (handleView == null) {
            val view = View(this).apply {
                val drawable = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setColor(currentSettings.handleColor.toInt())
                    alpha = effectiveAlpha
                    val radius = 12f * density
                    if (currentSettings.edgeSide == EdgeSide.RIGHT) {
                        cornerRadii = floatArrayOf(radius, radius, 0f, 0f, 0f, 0f, radius, radius)
                    } else {
                        cornerRadii = floatArrayOf(0f, 0f, radius, radius, radius, radius, 0f, 0f)
                    }
                }
                background = drawable

                var startX = 0f
                var startY = 0f
                setOnTouchListener { _, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            startX = event.rawX
                            startY = event.rawY
                            true
                        }
                        MotionEvent.ACTION_UP -> {
                            val diffX = event.rawX - startX
                            val isSwipe = if (currentSettings.edgeSide == EdgeSide.RIGHT) {
                                diffX < -20 // 왼쪽으로 스와이프
                            } else {
                                diffX > 20  // 오른쪽으로 스와이프
                            }
                            val isTap = kotlin.math.abs(diffX) < 15 && kotlin.math.abs(event.rawY - startY) < 15
                            if (isSwipe || isTap) {
                                triggerHaptic()
                                togglePanel()
                            }
                            true
                        }
                        else -> false
                    }
                }
            }
            handleView = view
            try {
                windowManager.addView(view, params)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            handleView?.let { view ->
                val drawable = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setColor(currentSettings.handleColor.toInt())
                    alpha = effectiveAlpha
                    val radius = 12f * density
                    if (currentSettings.edgeSide == EdgeSide.RIGHT) {
                        cornerRadii = floatArrayOf(radius, radius, 0f, 0f, 0f, 0f, radius, radius)
                    } else {
                        cornerRadii = floatArrayOf(0f, 0f, radius, radius, radius, radius, 0f, 0f)
                    }
                }
                view.background = drawable
                try {
                    windowManager.updateViewLayout(view, params)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun removeHandleView() {
        handleView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            handleView = null
        }
    }

    private fun togglePanel() {
        if (isPanelOpen) {
            closePanel()
        } else {
            openPanel()
        }
    }

    private fun registerSystemDialogReceiver() {
        if (systemDialogReceiver != null) return
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_CLOSE_SYSTEM_DIALOGS) {
                    closePanel()
                }
            }
        }
        systemDialogReceiver = receiver
        val filter = IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                registerReceiver(receiver, filter)
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun unregisterSystemDialogReceiver() {
        systemDialogReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {
                // ignore
            }
            systemDialogReceiver = null
        }
    }

    private fun openPanel() {
        if (isPanelOpen) return
        isPanelOpen = true

        if (currentSettings.pauseMediaOnOpen) {
            MediaControlHelper.pauseYouTubeOnly(this)
        }

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            windowAnimations = 0
            format = PixelFormat.TRANSLUCENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        val lifecycleOwner = OverlayLifecycleOwner()
        panelLifecycleOwner = lifecycleOwner

        val rootLayout = OverlayPanelRootLayout(this).apply {
            setBackgroundColor(AndroidColor.TRANSPARENT)
            onClose = { closePanel() }
        }

        val composeView = ComposeView(this).apply {
            setBackgroundColor(AndroidColor.TRANSPARENT)
            lifecycleOwner.attachToView(this)

            setContent {
                NotificationEdgeTheme(
                    fontId = currentSettings.selectedFont,
                    transparentStatusBar = true
                ) {
                    EdgePanelContent(
                        edgeSide = currentSettings.edgeSide,
                        panelWidthDp = currentSettings.panelWidthDp,
                        autoDismissOnOpen = currentSettings.autoDismissOnOpen,
                        onClose = { closePanel() },
                        onOpenSettings = {
                            closePanel()
                            val settingsIntent = Intent(this@EdgeOverlayService, SettingsActivity::class.java).apply {
                                putExtra(SettingsActivity.EXTRA_OPEN_SETTINGS, true)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            }
                            startActivity(settingsIntent)
                        },
                        onRequestFocus = { focusable ->
                            setPanelFocusable(focusable)
                        }
                    )
                }
            }
        }

        rootLayout.addView(
            composeView,
            android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        panelComposeView = rootLayout
        lifecycleOwner.onCreate()
        registerSystemDialogReceiver()

        try {
            windowManager.addView(rootLayout, params)
            rootLayout.post {
                rootLayout.requestFocus()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isPanelOpen = false
            unregisterSystemDialogReceiver()
        }
    }

    private fun closePanel() {
        if (!isPanelOpen) return
        isPanelOpen = false
        unregisterSystemDialogReceiver()

        val view = panelComposeView
        val owner = panelLifecycleOwner
        panelComposeView = null
        panelLifecycleOwner = null

        if (view != null) {
            view.visibility = View.GONE
            try {
                windowManager.removeViewImmediate(view)
            } catch (e: Exception) {
                try {
                    windowManager.removeView(view)
                } catch (e2: Exception) {
                    e2.printStackTrace()
                }
            }
        }
        owner?.onDestroy()
    }

    private fun setPanelFocusable(focusable: Boolean) {
        val view = panelComposeView ?: return
        val params = view.layoutParams as? WindowManager.LayoutParams ?: return
        if (focusable) {
            params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
            params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        } else {
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
        try {
            windowManager.updateViewLayout(view, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showEdgeLighting() {
        if (lightingComposeView != null) return // 이미 표시 중이면 무시

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            windowAnimations = 0
            format = PixelFormat.TRANSLUCENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        val lifecycleOwner = OverlayLifecycleOwner()
        lightingLifecycleOwner = lifecycleOwner

        val composeView = ComposeView(this).apply {
            setBackgroundColor(AndroidColor.TRANSPARENT)
            lifecycleOwner.attachToView(this)
            setContent {
                EdgeLightingEffect(
                    color = Color(currentSettings.edgeLightingColor),
                    cornerRadiusDp = currentSettings.edgeLightingCornerRadiusDp,
                    durationMs = currentSettings.edgeLightingDurationMs,
                    onFinish = { removeEdgeLighting() }
                )
            }
        }
        lightingComposeView = composeView
        lifecycleOwner.onCreate()

        try {
            windowManager.addView(composeView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeEdgeLighting() {
        lightingComposeView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            lightingComposeView = null
        }
        lightingLifecycleOwner?.onDestroy()
        lightingLifecycleOwner = null
    }

    override fun onDestroy() {
        super.onDestroy()
        removeHandleView()
        closePanel()
        removeEdgeLighting()
        serviceScope.cancel()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        const val ACTION_OPEN_PANEL = "com.devdooly.notificationedge.ACTION_OPEN_PANEL"
        const val ACTION_CLOSE_PANEL = "com.devdooly.notificationedge.ACTION_CLOSE_PANEL"
        const val ACTION_TOGGLE_PANEL = "com.devdooly.notificationedge.ACTION_TOGGLE_PANEL"

        @Volatile
        var isPanelOpen: Boolean = false
            private set
    }
}
