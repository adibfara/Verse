package com.github.adibfara.verse.core.persistence

import com.github.adibfara.verse.core.common.Debouncer
import com.github.adibfara.verse.core.common.DispatcherProvider
import com.github.adibfara.verse.core.model.RecentSpace
import com.github.adibfara.verse.core.model.Space
import com.github.adibfara.verse.core.platform.AppPaths
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

internal val VerseJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = true
    isLenient = true
}

private const val SettingsFileName = "settings.json"
private const val MaxRecentSpaces = 12

internal fun String.withoutByteOrderMark(): String = removePrefix("﻿")

class SettingsStore(
    private val storage: FileStorage,
    private val appPaths: AppPaths,
    private val dispatchers: DispatcherProvider,
    private val scope: CoroutineScope,
) {

    private val state = MutableStateFlow(AppSettings())
    private val debouncer = Debouncer(scope, delayMillis = 500)

    val settings: StateFlow<AppSettings> = state.asStateFlow()

    private val filePath: String get() = storage.join(appPaths.appDataDirectory, SettingsFileName)

    suspend fun load(): AppSettings = withContext(dispatchers.io) {
        val raw = storage.read(filePath)
        val loaded = raw
            ?.withoutByteOrderMark()
            ?.let { runCatching { VerseJson.decodeFromString<AppSettings>(it) }.getOrNull() }
            ?: AppSettings()
        state.value = loaded
        loaded
    }

    fun update(transform: (AppSettings) -> AppSettings) {
        state.value = transform(state.value)
        scheduleSave()
    }

    fun rememberSpace(space: Space, openedAt: Long) {
        if (space.isEphemeral) return
        update { current ->
            val entry = RecentSpace(
                id = space.id,
                name = space.name,
                location = space.location,
                lastOpenedAt = openedAt,
            )
            val remaining = current.recentSpaces.filterNot { it.id == space.id }
            current.copy(
                recentSpaces = (listOf(entry) + remaining).take(MaxRecentSpaces),
                lastSpaceId = space.id,
            )
        }
    }

    fun forgetSpace(space: RecentSpace) {
        update { current ->
            current.copy(
                recentSpaces = current.recentSpaces.filterNot { it.id == space.id },
                lastSpaceId = current.lastSpaceId?.takeIf { it != space.id },
            )
        }
    }

    private fun scheduleSave() {
        val snapshot = state.value
        debouncer.submit {
            withContext(dispatchers.io) {
                storage.write(filePath, VerseJson.encodeToString(snapshot))
            }
        }
    }

    suspend fun flush() = withContext(dispatchers.io) {
        debouncer.cancel()
        storage.write(filePath, VerseJson.encodeToString(state.value))
    }
}
