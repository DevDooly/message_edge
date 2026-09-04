package com.devdooly.notificationedge.util

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import com.devdooly.notificationedge.service.NotificationListener

/**
 * 엣지 알림 패널이 열릴 때 유튜브(YouTube) 재생을 즉시 일시 정지(Pause)시켜
 * PiP 팝업 진입을 방지하는 헬퍼.
 * (유튜브 뮤직 및 타 음악 플레이어는 절대 건드리지 않음)
 */
object MediaControlHelper {

    private const val TAG = "MediaControlHelper"

    /**
     * 오직 '유튜브(YouTube)' 앱 세션만 특정하여 동기식으로 일시 정지(Pause) 명령 전송.
     * - 유튜브 뮤직(com.google.android.apps.youtube.music) 및 일반 음악 앱은 건드리지 않음.
     * - AudioManager/미디어키 브로드캐스트를 사용하지 않아 다른 앱에 영향 없음.
     */
    fun pauseYouTubeOnly(context: Context) {
        val appContext = context.applicationContext

        try {
            val mediaSessionManager = appContext.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            val componentName = ComponentName(appContext, NotificationListener::class.java)
            val sessions = mediaSessionManager?.getActiveSessions(componentName)

            sessions?.forEach { controller ->
                val pkg = controller.packageName ?: ""
                // 오직 YouTube 앱만 타겟 (유튜브 뮤직 com.google.android.apps.youtube.music 제외)
                val isYouTube = (pkg == "com.google.android.youtube" ||
                        pkg.contains("revanced.android.youtube") ||
                        pkg.contains("vanced.android.youtube")) &&
                        !pkg.contains("music")

                if (isYouTube) {
                    val state = controller.playbackState?.state
                    if (state == PlaybackState.STATE_PLAYING ||
                        state == PlaybackState.STATE_BUFFERING ||
                        state == PlaybackState.STATE_CONNECTING) {
                        controller.transportControls.pause()
                    }
                }
            }
        } catch (e: SecurityException) {
            AppLog.warning(TAG, "미디어 세션 접근 권한 없음", e)
        } catch (e: Exception) {
            AppLog.warning(TAG, "미디어 일시정지 실패", e)
        }
    }
}
