package com.devdooly.notificationedge.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: SettingsRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repository = SettingsRepository(context)
    }

    @Test
    fun `isLaunchDirectToPanelSync should return true by default`() {
        val result = repository.isLaunchDirectToPanelSync()
        assertTrue(result)
    }

    @Test
    fun `updateLaunchDirectToPanel should synchronize to SharedPreferences immediately`() = runTest {
        repository.updateLaunchDirectToPanel(false)
        assertFalse(repository.isLaunchDirectToPanelSync())

        repository.updateLaunchDirectToPanel(true)
        assertTrue(repository.isLaunchDirectToPanelSync())
    }

    @Test
    fun `external control should be disabled by default and synchronize immediately`() = runTest {
        assertFalse(repository.isExternalControlEnabledSync())

        repository.updateExternalControlEnabled(true)
        assertTrue(repository.isExternalControlEnabledSync())

        repository.updateExternalControlEnabled(false)
        assertFalse(repository.isExternalControlEnabledSync())
    }

    @Test
    fun `diagnostic mode should be disabled by default and synchronize immediately`() = runTest {
        assertFalse(repository.isDiagnosticModeEnabledSync())

        repository.updateDiagnosticModeEnabled(true)
        assertTrue(repository.isDiagnosticModeEnabledSync())

        repository.updateDiagnosticModeEnabled(false)
        assertFalse(repository.isDiagnosticModeEnabledSync())
    }

    @Test
    fun `diagnostic session should expire after its deadline`() {
        val now = 1_000L

        assertTrue(SettingsRepository.isDiagnosticSessionActive(true, now + 1L, now))
        assertFalse(SettingsRepository.isDiagnosticSessionActive(true, now, now))
        assertFalse(SettingsRepository.isDiagnosticSessionActive(false, now + 1L, now))
    }

    @Test
    fun `discovered packages and excluded packages operations should update state correctly`() = runTest {
        repository.addDiscoveredPackage("com.kakao.talk")
        repository.addDiscoveredPackage("com.google.android.gm")

        repository.setPackageExcluded("com.kakao.talk", true)
    }

    @Test
    fun `blocked keywords add and remove operations should update state correctly`() = runTest {
        repository.addBlockedKeyword("광고")
        repository.addBlockedKeyword("스팸")
        repository.removeBlockedKeyword("스팸")
    }
}
