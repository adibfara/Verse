package com.github.adibfara.verse.app

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class ExternalOpenRequests {

    private val mutableRequests = MutableSharedFlow<String>(
        replay = 1,
        extraBufferCapacity = 8,
    )

    val requests: SharedFlow<String> = mutableRequests.asSharedFlow()

    fun request(absolutePath: String) {
        mutableRequests.tryEmit(absolutePath)
    }
}
