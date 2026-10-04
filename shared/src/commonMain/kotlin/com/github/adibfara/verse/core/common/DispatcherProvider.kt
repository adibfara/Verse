package com.github.adibfara.verse.core.common

import kotlinx.coroutines.CoroutineDispatcher

interface DispatcherProvider {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
}

expect fun platformDispatchers(): DispatcherProvider
