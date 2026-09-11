package com.devdooly.notificationedge.ui.settings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.devdooly.notificationedge.data.model.AppSettings
import com.devdooly.notificationedge.data.repository.NotificationRepository
import com.devdooly.notificationedge.data.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
        // 위임 검사는 파일 I/O와 분리한다. 실제 영속화는 SettingsRepositoryTest가 검증한다.
        // Robolectric의 다른 테스트가 만든 임시 DataStore 경로를 재사용하지 않는다.
        repository = mockk()
        every { repository.settingsFlow } returns flowOf(AppSettings())
        coEvery { repository.updateHapticEnabled(any()) } returns Unit
        coEvery { repository.updatePauseMediaOnOpen(any()) } returns Unit
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

        advanceUntilIdle()
        coVerify(exactly = 1) { repository.updateHapticEnabled(false) }
        coVerify(exactly = 1) { repository.updatePauseMediaOnOpen(false) }
        coVerify(exactly = 0) { repository.updateHapticEnabled(true) }
        coVerify(exactly = 0) { repository.updatePauseMediaOnOpen(true) }
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
