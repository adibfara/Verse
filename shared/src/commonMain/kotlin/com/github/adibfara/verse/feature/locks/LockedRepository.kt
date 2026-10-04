package com.github.adibfara.verse.feature.locks

import com.github.adibfara.verse.core.filesystem.SpaceFileSystem
import com.github.adibfara.verse.core.model.FilePath
import com.github.adibfara.verse.feature.references.ReferencesJson
import com.github.adibfara.verse.feature.references.ReferencesStore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

private val LockedPath = FilePath(".locked")

@Serializable
data class LockedDocument(val ids: Set<Long> = emptySet())

class LockedRepository(
    private val fileSystem: SpaceFileSystem,
    private val references: ReferencesStore = ReferencesStore(fileSystem),
) {

    private val mutex = Mutex()
    private var locked: LockedDocument? = null

    suspend fun isLocked(path: FilePath): Boolean = mutex.withLock {
        loadIfNeeded()
        val id = references.idFor(path) ?: return@withLock false
        id in requireLocked().ids
    }

    suspend fun setLocked(path: FilePath, isLocked: Boolean): Boolean = mutex.withLock {
        loadIfNeeded()
        val current = requireLocked()
        val updatedIds = if (isLocked) {
            current.ids + references.mintId(path)
        } else {
            val id = references.idFor(path) ?: return@withLock false
            current.ids - id
        }
        locked = current.copy(ids = updatedIds)
        persist()
        isLocked
    }

    suspend fun lockedPaths(): Set<FilePath> = mutex.withLock {
        loadIfNeeded()
        val ids = requireLocked().ids
        val entries = references.entries()
        ids.mapNotNull { id -> entries[id.toString()] }.map(::FilePath).toSet()
    }

    suspend fun onPathsRelocated(oldPath: FilePath, newPath: FilePath) = mutex.withLock {
        loadIfNeeded()
        references.relocate(oldPath, newPath)
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
        val current = requireLocked()
        locked = current.copy(ids = current.ids - ids)
        persist()
    }

    private suspend fun loadIfNeeded() {
        if (locked != null) return
        locked = runCatching {
            ReferencesJson.decodeFromString<LockedDocument>(fileSystem.readText(LockedPath))
        }.getOrDefault(LockedDocument())
    }

    private suspend fun persist() {
        val doc = locked ?: return
        runCatching {
            fileSystem.writeText(LockedPath, ReferencesJson.encodeToString(doc))
        }
    }

    private fun requireLocked(): LockedDocument = locked ?: LockedDocument()
}
