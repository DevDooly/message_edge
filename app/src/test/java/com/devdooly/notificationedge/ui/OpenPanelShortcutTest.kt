package com.devdooly.notificationedge.ui

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ActivityInfo
import androidx.test.core.app.ApplicationProvider
import com.devdooly.notificationedge.R
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 34])
@Suppress("DEPRECATION")
class OpenPanelShortcutTest {
    @Test fun registrationReturnsShortcutWithoutOpeningPanelOrSettings() {
        val request = Intent(Intent.ACTION_CREATE_SHORTCUT)
        val controller = Robolectric.buildActivity(OpenPanelActivity::class.java, request).create()
        val activity = controller.get()
        val shadow = shadowOf(activity)

        assertTrue(activity.isFinishing)
        assertEquals(Activity.RESULT_OK, shadow.resultCode)
        assertNull(shadow.nextStartedActivity)
        val result = requireNotNull(shadow.resultIntent)
        assertEquals(activity.getString(R.string.open_panel_activity_label),
            result.getStringExtra(Intent.EXTRA_SHORTCUT_NAME))
        val launch = requireNotNull(result.getParcelableExtra<Intent>(Intent.EXTRA_SHORTCUT_INTENT))
        assertEquals(ComponentName(activity, OpenPanelActivity::class.java), launch.component)
        assertEquals(Intent.ACTION_VIEW, launch.action)
        val required = Intent.FLAG_ACTIVITY_NO_USER_ACTION or Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_NO_ANIMATION
        assertEquals(required, launch.flags and required)
        assertTrue(result.hasExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE) ||
            result.hasExtra(Intent.EXTRA_SHORTCUT_ICON))
        controller.destroy()
    }

    @Test fun registrationActivitySupportsReturningResultToCallingApp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val info = context.packageManager.getActivityInfo(
            ComponentName(context, OpenPanelActivity::class.java), 0)
        // singleInstance/새 작업으로 등록 액티비티를 분리하면 RESULT_CANCELED가 먼저 전달된다.
        assertEquals(ActivityInfo.LAUNCH_MULTIPLE, info.launchMode)
        assertTrue(info.exported)
    }
}
