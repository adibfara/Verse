package com.github.adibfara.mds.core.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class Debouncer(
    private val scope: CoroutineScope,
    private val delayMillis: Long,
) {
    private var job: Job? = null

    fun submit(action: suspend () -> Unit) {
        job?.cancel()
        job = scope.launch {
            delay(delayMillis)
            action()
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
    }
}

fun <T> MutableStateFlow<T>.mutate(transform: (T) -> T) = update(transform)

fun <T> StateFlow<T>.current(): T = value

fun <T> Flow<T>.launchCollect(scope: CoroutineScope, collector: suspend (T) -> Unit): Job =
    scope.launch { collect(collector) }
