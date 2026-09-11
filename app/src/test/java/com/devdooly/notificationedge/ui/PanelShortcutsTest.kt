package com.devdooly.notificationedge.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.os.UserManager
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.test.core.app.ApplicationProvider
import com.devdooly.notificationedge.MainActivity
import com.devdooly.notificationedge.R
import com.devdooly.notificationedge.ui.shortcuts.PanelShortcuts
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 34])
class PanelShortcutsTest {
    private lateinit var context: Context
    private lateinit var manager: ShortcutManager

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        manager = context.getSystemService(ShortcutManager::class.java)
        shadowOf(context.getSystemService(UserManager::class.java)).setUserUnlocked(true)
        manager.removeAllDynamicShortcuts()
    }

    @Test fun publishesLauncherDiscoverableShortcutWithInitialLaunchFlags() {
        PanelShortcuts.publish(context)
        val shortcut = manager.dynamicShortcuts.single()
        assertTrue(shortcut.isDynamic)
        assertEquals(PanelShortcuts.OPEN_PANEL_ID, shortcut.id)
        assertEquals(ComponentName(context, MainActivity::class.java), shortcut.activity)
        assertEquals(context.getString(R.string.panel_shortcut_label), shortcut.shortLabel)
        val intent = requireNotNull(shortcut.intent)
        assertEquals(ComponentName(context, OpenPanelActivity::class.java), intent.component)
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NO_USER_ACTION != 0)
    }

    @Test fun repeatedPublicationPreservesOtherShortcutsAndDoesNotDuplicate() {
        manager.addDynamicShortcuts(listOf(ShortcutInfo.Builder(context, "unrelated_test")
            .setShortLabel("Other")
            .setIntent(Intent(context, MainActivity::class.java).setAction(Intent.ACTION_VIEW))
            .build()))
        repeat(3) { PanelShortcuts.publish(context) }
        assertEquals(setOf("unrelated_test", PanelShortcuts.OPEN_PANEL_ID),
            manager.dynamicShortcuts.map { it.id }.toSet())
        val current = ShortcutManagerCompat.getDynamicShortcuts(context).single { it.id == PanelShortcuts.OPEN_PANEL_ID }
        assertTrue(PanelShortcuts.isCurrent(current, PanelShortcuts.createInfo(context)))
    }

    @Test
    @Config(qualifiers = "ko")
    fun refreshReplacesStaleLanguageAndMissingLaunchFlags() {
        manager.addDynamicShortcuts(listOf(ShortcutInfo.Builder(context, PanelShortcuts.OPEN_PANEL_ID)
            .setShortLabel("Old English label")
            .setIntent(Intent(context, OpenPanelActivity::class.java).setAction(Intent.ACTION_VIEW))
            .build()))
        PanelShortcuts.publish(context)
        val updated = manager.dynamicShortcuts.single()
        assertEquals("알림 패널 열기", updated.shortLabel)
        assertTrue(requireNotNull(updated.intent).flags and Intent.FLAG_ACTIVITY_NO_USER_ACTION != 0)
    }

    @Test fun waitsForUserUnlockThenRetriesOnNextLaunch() {
        val user = shadowOf(context.getSystemService(UserManager::class.java))
        user.setUserUnlocked(false)
        PanelShortcuts.publish(context)
        assertTrue(manager.dynamicShortcuts.isEmpty())
        user.setUserUnlocked(true)
        PanelShortcuts.publish(context)
        assertEquals(PanelShortcuts.OPEN_PANEL_ID, manager.dynamicShortcuts.single().id)
    }
}
