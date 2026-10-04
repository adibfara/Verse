package com.github.adibfara.verse.feature.favorites

import com.github.adibfara.verse.core.filesystem.SpaceFileSystem
import com.github.adibfara.verse.core.model.FilePath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val FavoritesPath = FilePath(".favorites")
private val ReferencesPath = FilePath(".references")

private val FavoritesJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = true
}

@Serializable
data class FavoritesDocument(val ids: Set<Long> = emptySet())

@Serializable
data class ReferencesDocument(
    val nextId: Long = 1,
    val entries: Map<String, String> = emptyMap(),
)

class FavoritesRepository(private val fileSystem: SpaceFileSystem) {

    private val mutex = Mutex()
    private var favorites: FavoritesDocument? = null
    private var references: ReferencesDocument? = null

    private val mutablePaths = MutableStateFlow<Set<FilePath>>(emptySet())
    val paths: StateFlow<Set<FilePath>> = mutablePaths.asStateFlow()

    suspend fun isFavorite(path: FilePath): Boolean = mutex.withLock {
        loadIfNeeded()
        val id = idForLocked(path) ?: return@withLock false
        id in requireFavorites().ids
    }

    suspend fun toggleFavorite(path: FilePath): Boolean = mutex.withLock {
        loadIfNeeded()
        val id = idForLocked(path) ?: mintIdLocked(path)
        val current = requireFavorites()
        val updatedIds = if (id in current.ids) current.ids - id else current.ids + id
        favorites = current.copy(ids = updatedIds)
        persist()
        id in updatedIds
    }

    suspend fun favoritedPaths(): Set<FilePath> = mutex.withLock {
        loadIfNeeded()
        val favs = requireFavorites()
        val refs = requireReferences()
        favs.ids.mapNotNull { id -> refs.entries[id.toString()] }.map(::FilePath).toSet()
    }

    suspend fun onPathsRelocated(oldPath: FilePath, newPath: FilePath) = mutex.withLock {
        loadIfNeeded()
        val refs = requireReferences()
        var changed = false
        val updatedEntries = refs.entries.mapValues { (_, rawPath) ->
            val stored = FilePath(rawPath)
            when {
                stored == oldPath -> {
                    changed = true
                    newPath.value
                }

                stored.isDescendantOf(oldPath) -> {
                    changed = true
                    newPath.value + stored.value.removePrefix(oldPath.value)
                }

                else -> rawPath
            }
        }
        if (changed) {
            references = refs.copy(entries = updatedEntries)
            persist()
        }
    }

    suspend fun onPathDeleted(path: FilePath) = mutex.withLock {
        loadIfNeeded()
        val refs = requireReferences()
        val favs = requireFavorites()
        val removedKeys = refs.entries.filter { (_, rawPath) ->
            val stored = FilePath(rawPath)
            stored == path || stored.isDescendantOf(path)
        }.keys
        if (removedKeys.isEmpty()) return@withLock
        references = refs.copy(entries = refs.entries - removedKeys)
        favorites = favs.copy(ids = favs.ids.filterNot { it.toString() in removedKeys }.toSet())
        persist()
    }

    private fun idForLocked(path: FilePath): Long? =
        requireReferences().entries.entries.firstOrNull { it.value == path.value }?.key?.toLongOrNull()

    private fun mintIdLocked(path: FilePath): Long {
        val refs = requireReferences()
        val id = refs.nextId
        references = refs.copy(
            nextId = id + 1,
            entries = refs.entries + (id.toString() to path.value),
        )
        return id
    }

    private suspend fun loadIfNeeded() {
        if (favorites != null && references != null) return
        favorites = runCatching {
            FavoritesJson.decodeFromString<FavoritesDocument>(fileSystem.readText(FavoritesPath))
        }.getOrDefault(FavoritesDocument())
        references = runCatching {
            FavoritesJson.decodeFromString<ReferencesDocument>(fileSystem.readText(ReferencesPath))
        }.getOrDefault(ReferencesDocument())
        publishLocked()
    }

    private fun publishLocked() {
        val favs = requireFavorites()
        val refs = requireReferences()
        mutablePaths.value = favs.ids
            .mapNotNull { id -> refs.entries[id.toString()] }
            .map(::FilePath)
            .toSet()
    }

    private suspend fun persist() {
        publishLocked()
        val favoritesDoc = favorites ?: return
        val referencesDoc = references ?: return
        runCatching {
            fileSystem.writeText(FavoritesPath, FavoritesJson.encodeToString(favoritesDoc))
        }
        runCatching {
            fileSystem.writeText(ReferencesPath, FavoritesJson.encodeToString(referencesDoc))
        }
    }

    private fun requireFavorites(): FavoritesDocument = favorites ?: FavoritesDocument()

    private fun requireReferences(): ReferencesDocument = references ?: ReferencesDocument()
}
