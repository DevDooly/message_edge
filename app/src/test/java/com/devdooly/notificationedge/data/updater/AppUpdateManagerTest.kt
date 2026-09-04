package com.devdooly.notificationedge.data.updater

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateManagerTest {

    @Test
    fun `isNewerVersion should return true only when latest version is newer`() {
        assertTrue(AppUpdateManager.isNewerVersion("1.0.0", "1.0.1"))
        assertTrue(AppUpdateManager.isNewerVersion("1.0.0", "v1.0.1"))
        assertTrue(AppUpdateManager.isNewerVersion("v1.0.0", "1.0.1"))
        assertTrue(AppUpdateManager.isNewerVersion("1.3.13", "1.4.0"))
    }

    @Test
    fun `isNewerVersion should return false when same version or latest is empty`() {
        assertFalse(AppUpdateManager.isNewerVersion("1.0.1", "1.0.1"))
        assertFalse(AppUpdateManager.isNewerVersion("v1.0.1", "1.0.1"))
        assertFalse(AppUpdateManager.isNewerVersion("1.0.1", "v1.0.1"))
        assertFalse(AppUpdateManager.isNewerVersion("1.0.1", ""))
        assertFalse(AppUpdateManager.isNewerVersion("1.4.15", "1.0.1"))
        assertFalse(AppUpdateManager.isNewerVersion("1.0.1", "not-a-version"))
    }

    @Test
    fun `download URL should allow only trusted HTTPS hosts`() {
        assertTrue(AppUpdateManager.isAllowedDownloadUrl("https://github.com/DevDooly/message_edge/file.apk"))
        assertTrue(AppUpdateManager.isAllowedDownloadUrl("https://release-assets.githubusercontent.com/file.apk"))
        assertFalse(AppUpdateManager.isAllowedDownloadUrl("http://github.com/file.apk"))
        assertFalse(AppUpdateManager.isAllowedDownloadUrl("https://github.com.evil.example/file.apk"))
        assertFalse(AppUpdateManager.isAllowedDownloadUrl("https://evil.example/file.apk"))
    }
}
