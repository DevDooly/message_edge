package com.devdooly.notificationedge.data.updater

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateManagerTest {

    @Test
    fun `isNewerVersion should return true when version is different`() {
        assertTrue(AppUpdateManager.isNewerVersion("1.0.0", "1.0.1"))
        assertTrue(AppUpdateManager.isNewerVersion("1.0.0", "v1.0.1"))
        assertTrue(AppUpdateManager.isNewerVersion("v1.0.0", "1.0.1"))
        assertTrue(AppUpdateManager.isNewerVersion("1.4.15", "1.0.1")) // 버전 리셋 상황 지원
    }

    @Test
    fun `isNewerVersion should return false when same version or latest is empty`() {
        assertFalse(AppUpdateManager.isNewerVersion("1.0.1", "1.0.1"))
        assertFalse(AppUpdateManager.isNewerVersion("v1.0.1", "1.0.1"))
        assertFalse(AppUpdateManager.isNewerVersion("1.0.1", "v1.0.1"))
        assertFalse(AppUpdateManager.isNewerVersion("1.0.1", ""))
    }
}
