package com.devdooly.notificationedge.service

import android.app.Notification
import android.app.NotificationManager
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 34], qualifiers = "ko-rKR")
class ServiceLocalizationTest {
    @Test
    fun `running service refreshes its notification and channel without changing channel identity`() {
        val controller = Robolectric.buildService(EdgeOverlayService::class.java).create()
        val service = controller.get()
        try {
            val manager = service.getSystemService(NotificationManager::class.java)
            val original = shadowOf(service).lastForegroundNotification
            assertEquals("Slivue 실행 중", original.extras.getString(Notification.EXTRA_TITLE))
            val channelId = original.channelId
            assertEquals("Slivue 백그라운드 서비스", manager.getNotificationChannel(channelId).name.toString())

            RuntimeEnvironment.setQualifiers("en-rUS")
            service.onConfigurationChanged(service.resources.configuration)

            val updated = shadowOf(service).lastForegroundNotification
            assertEquals(channelId, updated.channelId)
            assertEquals("Slivue is running", updated.extras.getString(Notification.EXTRA_TITLE))
            assertEquals("Slivue background service", manager.getNotificationChannel(channelId).name.toString())
            assertEquals(NotificationManager.IMPORTANCE_MIN, manager.getNotificationChannel(channelId).importance)
        } finally {
            controller.destroy()
        }
    }
}
