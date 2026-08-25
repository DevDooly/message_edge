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
}
