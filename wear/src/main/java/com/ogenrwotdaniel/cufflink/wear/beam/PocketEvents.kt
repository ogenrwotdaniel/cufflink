package com.ogenrwotdaniel.cufflink.wear.beam

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Ticks whenever a beam lands, so an open Pocket screen refreshes on its own.
 * The listener service runs in the app's process, so a plain singleton reaches the UI.
 */
object PocketEvents {
    private val _arrivals = MutableStateFlow(0)
    val arrivals: StateFlow<Int> = _arrivals

    fun arrived() {
        _arrivals.value++
    }
}
