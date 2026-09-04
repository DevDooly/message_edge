package com.devdooly.notificationedge.ui.overlay

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.abs

/** 패널의 애니메이션, 답장, 드래그 상태를 한곳에서 관리한다. */
@Stable
internal class EdgePanelUiState {
    var isVisible by mutableStateOf(false)
        private set

    var activeReplyKey by mutableStateOf<String?>(null)
        private set

    var replyText by mutableStateOf("")
        private set

    var dragOffsetX by mutableFloatStateOf(0f)
        private set

    private var isClosing = false

    fun reveal() {
        if (!isClosing) isVisible = true
    }

    /** 닫기 애니메이션을 새로 시작해야 할 때만 true를 반환한다. */
    fun beginClose(): Boolean {
        if (isClosing) return false
        isClosing = true
        isVisible = false
        return true
    }

    fun openReply(notificationKey: String) {
        activeReplyKey = notificationKey
    }

    fun closeReply() {
        activeReplyKey = null
        replyText = ""
    }

    fun updateReplyText(value: String) {
        replyText = value
    }

    fun beginDrag() {
        dragOffsetX = 0f
    }

    fun dragBy(amount: Float) {
        dragOffsetX += amount
    }

    fun isDragPastThreshold(threshold: Float): Boolean = abs(dragOffsetX) > threshold

    fun endDrag() {
        dragOffsetX = 0f
    }
}
