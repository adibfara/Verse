package com.github.adibfara.mds.core.filesystem

import com.github.adibfara.mds.core.model.FileKind
import com.github.adibfara.mds.core.model.FileNode
import com.github.adibfara.mds.core.model.FilePath
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemorySpaceFileSystem(
    override val rootLocation: String = "memory://space",
    seed: Map<String, String> = emptyMap(),
) : SpaceFileSystem {

    private class Entry(
        val path: FilePath,
        val isDirectory: Boolean,
        var content: String = "",
        var lastModified: Long = 0L,
    )

    private val entries = linkedMapOf<String, Entry>()
    private val mutex = Mutex()
    private val changes = MutableSharedFlow<FileChange>(extraBufferCapacity = 64)
    private var clock = 1L

    init {
        entries[""] = Entry(FilePath.Root, isDirectory = true)
        seed.forEach { (rawPath, content) ->
            val path = FilePath(rawPath)
            createParents(path)
            entries[path.value] =
                Entry(path, isDirectory = false, content = content, lastModified = clock++)
        }
    }

    private fun createParents(path: FilePath) {
        val parents = generateSequence(path.parent) { it.parent }.toList().reversed()
        parents.forEach { parent ->
            if (!entries.containsKey(parent.value)) {
                entries[parent.value] = Entry(parent, isDirectory = true, lastModified = clock++)
            }
        }
    }

    private fun entryToNode(entry: Entry): FileNode = FileNode(
        path = entry.path,
        name = if (entry.path.isRoot) "/" else entry.path.name,
        isDirectory = entry.isDirectory,
        kind = if (entry.isDirectory) FileKind.Other else FileKind.fromExtension(entry.path.extension),
        lastModified = entry.lastModified,
        size = entry.content.length.toLong(),
    )

    private fun childrenOf(path: FilePath): List<Entry> = entries.values.filter { entry ->
        !entry.path.isRoot && entry.path.parent?.value == path.value
    }

    override suspend fun listChildren(path: FilePath): List<FileNode> = mutex.withLock {
        childrenOf(path).map(::entryToNode)
    }

    override suspend fun readText(path: FilePath): String = mutex.withLock {
        entries[path.value]?.content ?: throw FileSystemException("Not found: ${path.value}")
    }

    override suspend fun readBytes(path: FilePath): ByteArray = readText(path).encodeToByteArray()

    override suspend fun writeText(path: FilePath, content: String) {
        mutex.withLock {
            val entry = entries.getOrPut(path.value) { Entry(path, isDirectory = false) }
            entry.content = content
            entry.lastModified = clock++
        }
        changes.emit(FileChange.Modified(path))
    }

    override suspend fun createFile(parent: FilePath, name: String): FilePath {
        val target = mutex.withLock {
            val taken = childrenOf(parent).map { it.path.name }.toSet()
            val finalName = uniqueName(taken, name)
            val path = parent.child(finalName)
            entries[path.value] = Entry(path, isDirectory = false, lastModified = clock++)
            path
        }
        changes.emit(FileChange.Created(target))
        return target
    }

    override suspend fun createDirectory(parent: FilePath, name: String): FilePath {
        val target = mutex.withLock {
            val taken = childrenOf(parent).map { it.path.name }.toSet()
            val finalName = uniqueName(taken, name)
            val path = parent.child(finalName)
            entries[path.value] = Entry(path, isDirectory = true, lastModified = clock++)
            path
        }
        changes.emit(FileChange.Created(target))
        return target
    }

    override suspend fun rename(path: FilePath, newName: String): FilePath {
        val target = path.withName(newName)
        relocate(path, target)
        return target
    }

    override suspend fun move(path: FilePath, newParent: FilePath): FilePath {
        val target = newParent.child(path.name)
        relocate(path, target)
        return target
    }

    private suspend fun relocate(from: FilePath, to: FilePath) {
        mutex.withLock {
            val affected = entries.values.filter { it.path.value == from.value || it.path.isDescendantOf(from) }
            affected.forEach { entry ->
                entries.remove(entry.path.value)
                val suffix = entry.path.value.removePrefix(from.value)
                val relocated = FilePath(to.value + suffix)
                entries[relocated.value] = Entry(
                    path = relocated,
                    isDirectory = entry.isDirectory,
                    content = entry.content,
                    lastModified = clock++,
                )
            }
        }
        changes.emit(FileChange.Deleted(from))
        changes.emit(FileChange.Created(to))
    }

    override suspend fun copy(path: FilePath, newParent: FilePath): FilePath {
        val target = mutex.withLock {
            val taken = childrenOf(newParent).map { it.path.name }.toSet()
            val finalName = uniqueName(taken, path.name)
            val destination = newParent.child(finalName)
            val affected = entries.values.filter { it.path.value == path.value || it.path.isDescendantOf(path) }
            affected.forEach { entry ->
                val suffix = entry.path.value.removePrefix(path.value)
                val copied = FilePath(destination.value + suffix)
                entries[copied.value] = Entry(
                    path = copied,
                    isDirectory = entry.isDirectory,
                    content = entry.content,
                    lastModified = clock++,
                )
            }
            destination
        }
        changes.emit(FileChange.Created(target))
        return target
    }

    override suspend fun delete(path: FilePath) {
        mutex.withLock {
            entries.values
                .filter { it.path.value == path.value || it.path.isDescendantOf(path) }
                .forEach { entries.remove(it.path.value) }
        }
        changes.emit(FileChange.Deleted(path))
    }

    override suspend fun exists(path: FilePath): Boolean = mutex.withLock {
        entries.containsKey(path.value)
    }

    override suspend fun metadata(path: FilePath): FileNode? = mutex.withLock {
        entries[path.value]?.let(::entryToNode)
    }

    override suspend fun listDirectoriesRecursively(limit: Int): List<FileNode> = mutex.withLock {
        entries.values.filter { it.isDirectory }.take(limit).map(::entryToNode)
    }

    override suspend fun listFilesRecursively(limit: Int): List<FileNode> = mutex.withLock {
        entries.values.filter { !it.isDirectory }.take(limit).map(::entryToNode)
    }

    override fun absolutePathOf(path: FilePath): String = "$rootLocation/${path.value}"

    override fun watch(path: FilePath): Flow<FileChange> = changes
        .asSharedFlow()
        .filter { it.path.parent?.value == path.value || it.path.value == path.value }
}
