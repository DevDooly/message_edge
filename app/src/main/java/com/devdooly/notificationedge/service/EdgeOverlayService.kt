package com.devdooly.notificationedge.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
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
import com.devdooly.notificationedge.util.OverlayLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class EdgeOverlayService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var windowManager: WindowManager
    private lateinit var settingsRepository: SettingsRepository

    private var handleView: View? = null
    private var panelComposeView: ComposeView? = null
    private var lightingComposeView: ComposeView? = null

    private var panelLifecycleOwner: OverlayLifecycleOwner? = null
    private var lightingLifecycleOwner: OverlayLifecycleOwner? = null

    private var currentSettings = AppSettings()
    private var isPanelOpen = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        settingsRepository = SettingsRepository(applicationContext)

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
                if (isPanelOpen) closePanel() else openPanel()
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
                    closePanel()
                }
            }
        }
    }

    private fun observeNewNotifications() {
        serviceScope.launch {
            NotificationRepository.newNotificationEvent.collect { notification ->
                if (currentSettings.isServiceEnabled && currentSettings.isEdgeLightingEnabled) {
                    if (!currentSettings.excludedPackages.contains(notification.packageName)) {
                        triggerHaptic()
                        showEdgeLighting()
                    }
                }
            }
        }
    }

    private fun triggerHaptic() {
        if (!currentSettings.hapticFeedbackEnabled) return
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
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
                                openPanel()
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

    private fun openPanel() {
        if (isPanelOpen) return
        isPanelOpen = true

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
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        val lifecycleOwner = OverlayLifecycleOwner()
        panelLifecycleOwner = lifecycleOwner

        val composeView = ComposeView(this).apply {
            lifecycleOwner.attachToComposeView(this)
            setContent {
                EdgePanelContent(
                    edgeSide = currentSettings.edgeSide,
                    panelWidthDp = currentSettings.panelWidthDp,
                    autoDismissOnOpen = currentSettings.autoDismissOnOpen,
                    onClose = { closePanel() },
                    onOpenSettings = {
                        closePanel()
                        val intent = Intent(this@EdgeOverlayService, MainActivity::class.java).apply {
                            putExtra(MainActivity.EXTRA_OPEN_SETTINGS, true)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        }
                        startActivity(intent)
                    }
                )
            }
        }
        panelComposeView = composeView
        lifecycleOwner.onCreate()

        try {
            windowManager.addView(composeView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun closePanel() {
        if (!isPanelOpen) return
        isPanelOpen = false

        panelComposeView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            panelComposeView = null
        }
        panelLifecycleOwner?.onDestroy()
        panelLifecycleOwner = null
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
        )

        val lifecycleOwner = OverlayLifecycleOwner()
        lightingLifecycleOwner = lifecycleOwner

        val composeView = ComposeView(this).apply {
            lifecycleOwner.attachToComposeView(this)
            setContent {
                EdgeLightingEffect(
                    color = Color(currentSettings.edgeLightingColor),
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
    }
}
