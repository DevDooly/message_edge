package com.devdooly.notificationedge.ui.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.devdooly.notificationedge.data.repository.NotificationRepository
import com.devdooly.notificationedge.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val mainDispatcher = UnconfinedTestDispatcher()
    private lateinit var application: Application
    private lateinit var repository: SettingsRepository
    private lateinit var viewModelStore: ViewModelStore
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
        application = ApplicationProvider.getApplicationContext()
        repository = SettingsRepository(application)
        viewModelStore = ViewModelStore()
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(application, repository) as T
            }
        }
        viewModel = ViewModelProvider(viewModelStore, factory)[SettingsViewModel::class.java]
        NotificationRepository.clearAll()
    }

    @After
    fun tearDown() {
        viewModelStore.clear()
        NotificationRepository.clearAll()
        Dispatchers.resetMain()
    }

    @Test
    fun `설정 변경 이벤트를 저장소에 위임한다`() = runTest(mainDispatcher) {
        viewModel.updateHapticEnabled(false)
        viewModel.updatePauseMediaOnOpen(false)

        val saved = repository.settingsFlow.first {
            !it.hapticFeedbackEnabled && !it.pauseMediaOnOpen
        }
        assertTrue(!saved.hapticFeedbackEnabled)
        assertTrue(!saved.pauseMediaOnOpen)
    }

    @Test
    fun `테스트 알림은 앱 패키지 정보와 함께 저장된다`() {
        viewModel.emitTestNotification()

        val notification = NotificationRepository.notifications.value.single()
        assertEquals(application.packageName, notification.packageName)
        assertEquals(999, notification.id)
        assertTrue(notification.text.contains("Slivue"))
    }
}
