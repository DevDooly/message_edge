package com.devdooly.notificationedge

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationEdgeInstrumentedTest {

    @Test
    fun `애플리케이션 ID가 유지된다`() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.devdooly.notificationedge", appContext.packageName)
    }

    @Test
    fun `민감 컴포넌트의 외부 공개 정책이 유지된다`() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val packageInfo = context.packageManager.getPackageInfoCompat(context.packageName)

        val edgePanelActivity = packageInfo.activities.single {
            it.name == "com.devdooly.notificationedge.ui.overlay.EdgePanelActivity"
        }
        val settingsActivity = packageInfo.activities.single {
            it.name == "com.devdooly.notificationedge.ui.settings.SettingsActivity"
        }
        val overlayService = packageInfo.services.single {
            it.name == "com.devdooly.notificationedge.service.EdgeOverlayService"
        }
        val bootReceiver = packageInfo.receivers.single {
            it.name == "com.devdooly.notificationedge.service.BootReceiver"
        }

        assertFalse(edgePanelActivity.exported)
        assertFalse(settingsActivity.exported)
        assertFalse(overlayService.exported)
        assertFalse(bootReceiver.exported)
    }

    @Test
    fun `알림 리스너 서비스는 시스템 바인딩 권한으로 보호된다`() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val packageInfo = context.packageManager.getPackageInfoCompat(context.packageName)
        val listener = packageInfo.services.single {
            it.name == "com.devdooly.notificationedge.service.NotificationListener"
        }

        assertEquals(
            "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE",
            listener.permission
        )
    }

    @Suppress("DEPRECATION")
    private fun PackageManager.getPackageInfoCompat(packageName: String): PackageInfo {
        val flags = PackageManager.GET_ACTIVITIES or
            PackageManager.GET_SERVICES or
            PackageManager.GET_RECEIVERS or
            PackageManager.GET_PROVIDERS
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            getPackageInfo(packageName, flags)
        }
    }
}
