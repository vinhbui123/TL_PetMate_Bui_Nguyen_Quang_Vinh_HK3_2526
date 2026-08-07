package com.example.petmate.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object AppEventBus {
    private val _refreshEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val refreshEvents = _refreshEvents.asSharedFlow()

    fun triggerRefresh() {
        _refreshEvents.tryEmit(Unit)
    }
}
