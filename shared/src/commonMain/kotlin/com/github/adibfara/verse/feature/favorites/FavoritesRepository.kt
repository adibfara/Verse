package com.github.adibfara.verse.feature.favorites

import com.github.adibfara.verse.core.filesystem.SpaceFileSystem
import com.github.adibfara.verse.core.model.FilePath
import com.github.adibfara.verse.feature.references.ReferencesJson
import com.github.adibfara.verse.feature.references.ReferencesStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

private val FavoritesPath = FilePath(".favorites")

@Serializable
data class FavoritesDocument(val ids: Set<Long> = emptySet())

class FavoritesRepository(
    private val fileSystem: SpaceFileSystem,
    private val references: ReferencesStore = ReferencesStore(fileSystem),
) {

    private val mutex = Mutex()
    private var favorites: FavoritesDocument? = null

    private val mutablePaths = MutableStateFlow<Set<FilePath>>(emptySet())
    val paths: StateFlow<Set<FilePath>> = mutablePaths.asStateFlow()

    suspend fun isFavorite(path: FilePath): Boolean = mutex.withLock {
        loadIfNeeded()
        val id = references.idFor(path) ?: return@withLock false
        id in requireFavorites().ids
    }

    suspend fun toggleFavorite(path: FilePath): Boolean = mutex.withLock {
        loadIfNeeded()
        val id = references.mintId(path)
        val current = requireFavorites()
        val updatedIds = if (id in current.ids) current.ids - id else current.ids + id
        favorites = current.copy(ids = updatedIds)
        persist()
        id in updatedIds
    }

    suspend fun favoritedPaths(): Set<FilePath> = mutex.withLock {
        loadIfNeeded()
        val ids = requireFavorites().ids
        val entries = references.entries()
        ids.mapNotNull { id -> entries[id.toString()] }.map(::FilePath).toSet()
    }

    suspend fun onPathsRelocated(oldPath: FilePath, newPath: FilePath) = mutex.withLock {
        loadIfNeeded()
        if (references.relocate(oldPath, newPath)) publishLocked()
    }

    suspend fun onPathDeleted(path: FilePath) = mutex.withLock {
        loadIfNeeded()
        dropIdsLocked(references.removeEntries(path))
    }

    suspend fun dropIds(ids: Set<Long>) = mutex.withLock {
        loadIfNeeded()
        dropIdsLocked(ids)
    }

    private suspend fun dropIdsLocked(ids: Set<Long>) {
        if (ids.isEmpty()) return
        val favs = requireFavorites()
        favorites = favs.copy(ids = favs.ids - ids)
        persist()
    }

    private suspend fun loadIfNeeded() {
        if (favorites != null) return
        favorites = runCatching {
            ReferencesJson.decodeFromString<FavoritesDocument>(fileSystem.readText(FavoritesPath))
        }.getOrDefault(FavoritesDocument())
        publishLocked()
    }

    private suspend fun publishLocked() {
        val ids = requireFavorites().ids
        val entries = references.entries()
        mutablePaths.value = ids
            .mapNotNull { id -> entries[id.toString()] }
            .map(::FilePath)
            .toSet()
    }

    private suspend fun persist() {
        publishLocked()
        val favoritesDoc = favorites ?: return
        runCatching {
            fileSystem.writeText(FavoritesPath, ReferencesJson.encodeToString(favoritesDoc))
        }
    }

    private fun requireFavorites(): FavoritesDocument = favorites ?: FavoritesDocument()
}
