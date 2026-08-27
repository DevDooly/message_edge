package com.devdooly.notificationedge.util

import android.content.ComponentName
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.util.Log
import android.view.KeyEvent
import com.devdooly.notificationedge.service.NotificationListener

/**
 * 엣지 알림 패널이 열릴 때 재생 중인 유튜브, 미디어, 음악 등을 즉시 일시 정지(Pause)시키는 헬퍼
 */
object MediaControlHelper {

    private const val TAG = "MediaControlHelper"

    /**
     * 현재 재생 중인 모든 활성 미디어 세션 및 오디오에 일시 정지(Pause) 명령 전송
     */
    fun pauseActiveMedia(context: Context) {
        val appContext = context.applicationContext

        // 1. NotificationListenerService 권한 기반 MediaSessionManager를 통해 활성 미디어 세션 Pause
        try {
            val mediaSessionManager = appContext.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            val componentName = ComponentName(appContext, NotificationListener::class.java)
            val sessions = mediaSessionManager?.getActiveSessions(componentName)
            sessions?.forEach { controller ->
                val state = controller.playbackState?.state
                if (state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING || state == PlaybackState.STATE_CONNECTING) {
                    controller.transportControls.pause()
                }
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "NotificationListener permission not granted for MediaSessionManager: ${e.message}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to pause active media sessions: ${e.message}")
        }

        // 2. AudioManager 오디오 포커스 일시 요청을 통한 유튜브/미디어 자동 일시 정지 발동
        try {
            val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .build()
                        )
                        .setOnAudioFocusChangeListener { /* 일시 포커스 */ }
                        .build()
                    audioManager.requestAudioFocus(focusRequest)
                } else {
                    @Suppress("DEPRECATION")
                    audioManager.requestAudioFocus(
                        { /* 일시 포커스 */ },
                        AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                    )
                }

                // 3. Fallback: 미디어 일시 정지 키 이벤트 전송
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE))
                audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to request audio focus or dispatch pause key: ${e.message}")
        }
    }
}
