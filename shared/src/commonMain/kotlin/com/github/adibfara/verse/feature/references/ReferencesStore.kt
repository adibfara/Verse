package com.github.adibfara.verse.feature.references

import com.github.adibfara.verse.core.filesystem.SpaceFileSystem
import com.github.adibfara.verse.core.model.FilePath
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val ReferencesPath = FilePath(".references")

internal val ReferencesJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = true
}

@Serializable
data class ReferencesDocument(
    val nextId: Long = 1,
    val entries: Map<String, String> = emptyMap(),
)

class ReferencesStore(private val fileSystem: SpaceFileSystem) {

    private val mutex = Mutex()
    private var references: ReferencesDocument? = null

    suspend fun idFor(path: FilePath): Long? = mutex.withLock {
        loadIfNeeded()
        idForLocked(path)
    }

    suspend fun mintId(path: FilePath): Long = mutex.withLock {
        loadIfNeeded()
        idForLocked(path)?.let { return@withLock it }
        val id = mintIdLocked(path)
        persist()
        id
    }

    suspend fun entries(): Map<String, String> = mutex.withLock {
        loadIfNeeded()
        requireReferences().entries
    }

    suspend fun relocate(oldPath: FilePath, newPath: FilePath): Boolean = mutex.withLock {
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
        changed
    }

    suspend fun removeEntries(path: FilePath): Set<Long> = mutex.withLock {
        loadIfNeeded()
        val refs = requireReferences()
        val removedKeys = refs.entries.filter { (_, rawPath) ->
            val stored = FilePath(rawPath)
            stored == path || stored.isDescendantOf(path)
        }.keys
        if (removedKeys.isEmpty()) return@withLock emptySet()
        references = refs.copy(entries = refs.entries - removedKeys)
        persist()
        removedKeys.mapNotNull { it.toLongOrNull() }.toSet()
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
        if (references != null) return
        references = runCatching {
            ReferencesJson.decodeFromString<ReferencesDocument>(fileSystem.readText(ReferencesPath))
        }.getOrDefault(ReferencesDocument())
    }

    private suspend fun persist() {
        val doc = references ?: return
        runCatching {
            fileSystem.writeText(ReferencesPath, ReferencesJson.encodeToString(doc))
        }
    }

    private fun requireReferences(): ReferencesDocument = references ?: ReferencesDocument()
}
