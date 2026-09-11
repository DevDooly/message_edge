package com.devdooly.notificationedge.ui.overlay

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PanelLaunchGateTest {
    @Test fun preparesBeforeLaunchingAndCoalescesRepeatedOpenRequests() = runTest {
        val events = mutableListOf<String>()
        val gate = PanelLaunchGate(this)
        gate.open({ events += "pause"; delay(100); events += "paused" }, { events += "open" })
        gate.open({ events += "duplicate" }, { events += "duplicate" })
        assertTrue(gate.isPending)
        testScheduler.advanceUntilIdle()
        assertEquals(listOf("pause", "paused", "open"), events)
        assertFalse(gate.isPending)
    }

    @Test fun closeCancelsDelayedOpenAndAllowsNextRequest() = runTest {
        var opens = 0
        val gate = PanelLaunchGate(this)
        gate.open({ delay(600) }, { opens++ })
        testScheduler.runCurrent()
        gate.cancel()
        testScheduler.advanceUntilIdle()
        assertEquals(0, opens)
        assertFalse(gate.isPending)
        gate.open({}, { opens++ })
        testScheduler.advanceUntilIdle()
        assertEquals(1, opens)
    }
}
