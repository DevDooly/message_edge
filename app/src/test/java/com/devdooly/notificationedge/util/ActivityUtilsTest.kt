package com.devdooly.notificationedge.util

import android.app.Activity
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class ActivityUtilsTest {

    @Test
    fun `overridePendingTransitionNoAnim should execute without throwing exception`() {
        val mockActivity = mockk<Activity>(relaxed = true)
        
        // 에러 없이 호출 완료되는지 검증
        ActivityUtils.overridePendingTransitionNoAnim(mockActivity)
        
        verify(atLeast = 0) { mockActivity.finish() }
    }
}
