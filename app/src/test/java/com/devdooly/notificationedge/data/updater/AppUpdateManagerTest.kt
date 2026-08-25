package com.devdooly.notificationedge.data.updater

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateManagerTest {

    @Test
    fun `isNewerVersion should return true when patch version is higher`() {
        assertTrue(AppUpdateManager.isNewerVersion("1.3.8", "1.3.9"))
        assertTrue(AppUpdateManager.isNewerVersion("1.3.8", "v1.3.9"))
        assertTrue(AppUpdateManager.isNewerVersion("v1.3.8", "1.3.9"))
    }

    @Test
    fun `isNewerVersion should return true when minor or major version is higher`() {
        assertTrue(AppUpdateManager.isNewerVersion("1.3.8", "1.4.0"))
        assertTrue(AppUpdateManager.isNewerVersion("1.3.8", "2.0.0"))
        assertTrue(AppUpdateManager.isNewerVersion("1.0", "1.0.1"))
    }

    @Test
    fun `isNewerVersion should return false when same version`() {
        assertFalse(AppUpdateManager.isNewerVersion("1.3.8", "1.3.8"))
        assertFalse(AppUpdateManager.isNewerVersion("v1.3.8", "1.3.8"))
        assertFalse(AppUpdateManager.isNewerVersion("1.3.8", "v1.3.8"))
    }

    @Test
    fun `isNewerVersion should return false when current is newer than remote`() {
        assertFalse(AppUpdateManager.isNewerVersion("1.4.0", "1.3.9"))
        assertFalse(AppUpdateManager.isNewerVersion("2.0.0", "1.9.9"))
        assertFalse(AppUpdateManager.isNewerVersion("1.3.8", "1.3.7"))
    }
}
