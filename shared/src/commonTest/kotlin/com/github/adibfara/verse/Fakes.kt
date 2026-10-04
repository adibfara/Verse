package com.github.adibfara.verse

import com.github.adibfara.verse.core.common.DispatcherProvider
import com.github.adibfara.verse.core.persistence.FileStorage
import com.github.adibfara.verse.core.platform.AppPaths
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher

class TestDispatchers(
    private val dispatcher: CoroutineDispatcher = UnconfinedTestDispatcher(),
) : DispatcherProvider {
    override val main: CoroutineDispatcher get() = dispatcher
    override val io: CoroutineDispatcher get() = dispatcher
    override val default: CoroutineDispatcher get() = dispatcher
}

class InMemoryFileStorage : FileStorage {

    private val files = mutableMapOf<String, String>()

    override suspend fun read(absolutePath: String): String? = files[absolutePath]

    override suspend fun write(absolutePath: String, content: String) {
        files[absolutePath] = content
    }

    override suspend fun exists(absolutePath: String): Boolean = files.containsKey(absolutePath)

    override fun join(base: String, vararg parts: String): String =
        (listOf(base) + parts).joinToString("/")
}

class TestAppPaths(
    override val appDataDirectory: String = "/app",
    override val homeDirectory: String = "/home",
) : AppPaths
