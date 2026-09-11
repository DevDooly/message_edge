package com.devdooly.notificationedge.ui.overlay

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** 메인 스레드에서 호출한다. 준비 중 중복 실행과 취소 후 뒤늦은 패널 열기를 막는다. */
internal class PanelLaunchGate(private val scope: CoroutineScope) {
    private var pending: Job? = null
    val isPending: Boolean get() = pending?.isActive == true

    fun open(prepare: suspend () -> Unit, launch: () -> Unit) {
        if (isPending) return
        pending = scope.launch(start = CoroutineStart.LAZY) {
            prepare()
            launch()
        }.also { it.start() }
    }

    fun cancel() {
        pending?.cancel()
        pending = null
    }
}
