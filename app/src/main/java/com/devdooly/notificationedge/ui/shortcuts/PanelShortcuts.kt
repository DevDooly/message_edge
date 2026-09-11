package com.devdooly.notificationedge.ui.shortcuts

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.UserManager
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.devdooly.notificationedge.MainActivity
import com.devdooly.notificationedge.R
import com.devdooly.notificationedge.ui.OpenPanelActivity
import com.devdooly.notificationedge.util.AppLog

/** 런처 및 One Hand Operation+ 등 앱 바로가기 소비자가 조회할 수 있도록 등록한다. */
object PanelShortcuts {
    internal const val OPEN_PANEL_ID = "slivue_open_panel"

    internal fun createLaunchIntent(context: Context) = Intent(context, OpenPanelActivity::class.java).apply {
        action = Intent.ACTION_VIEW
        // 정적 XML 바로가기는 사용자 지정 플래그를 보존하지 못하므로 동적 바로가기를 쓴다.
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION or
            Intent.FLAG_ACTIVITY_NO_USER_ACTION)
    }

    internal fun createInfo(context: Context): ShortcutInfoCompat =
        ShortcutInfoCompat.Builder(context, OPEN_PANEL_ID)
            .setActivity(ComponentName(context, MainActivity::class.java))
            .setShortLabel(context.getString(R.string.panel_shortcut_label))
            .setLongLabel(context.getString(R.string.open_panel_activity_label))
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
            .setIntent(createLaunchIntent(context))
            .setRank(0)
            .build()

    fun publish(context: Context) {
        val appContext = context.applicationContext
        try {
            if (appContext.getSystemService(UserManager::class.java)?.isUserUnlocked != true) return
            val expected = createInfo(appContext)
            val current = ShortcutManagerCompat.getDynamicShortcuts(appContext)
                .firstOrNull { it.id == OPEN_PANEL_ID }
            if (current != null && isCurrent(current, expected)) return
            // 다른 기능의 바로가기를 지우지 않고 이 ID만 추가·갱신한다.
            if (!ShortcutManagerCompat.addDynamicShortcuts(appContext, listOf(expected))) {
                AppLog.warning("PanelShortcuts", "앱 바로가기 등록 지연; 다음 실행 시 재시도")
            }
        } catch (error: Exception) {
            // 잠금 상태·런처 서비스 오류가 패널 실행을 막아서는 안 된다.
            AppLog.warning("PanelShortcuts", "앱 바로가기 등록 실패", error)
        }
    }

    internal fun isCurrent(current: ShortcutInfoCompat, expected: ShortcutInfoCompat): Boolean =
        current.isEnabled && current.activity == expected.activity &&
            current.shortLabel.toString() == expected.shortLabel.toString() &&
            current.longLabel?.toString() == expected.longLabel?.toString() &&
            current.rank == expected.rank && current.intents.size == 1 &&
            current.intent.filterEquals(expected.intent) && current.intent.flags == expected.intent.flags
}
