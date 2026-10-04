package com.github.adibfara.verse.core.filesystem

import com.github.adibfara.verse.core.model.FileNode
import com.github.adibfara.verse.core.model.FilePath
import com.github.adibfara.verse.core.model.Space
import kotlinx.coroutines.flow.Flow

sealed interface FileChange {
    val path: FilePath

    data class Created(override val path: FilePath) : FileChange
    data class Modified(override val path: FilePath) : FileChange
    data class Deleted(override val path: FilePath) : FileChange
}

class FileSystemException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

interface SpaceFileSystem {

    val rootLocation: String

    suspend fun listChildren(path: FilePath): List<FileNode>

    suspend fun readText(path: FilePath): String

    suspend fun readBytes(path: FilePath): ByteArray

    suspend fun writeText(path: FilePath, content: String)

    suspend fun createFile(parent: FilePath, name: String): FilePath

    suspend fun createDirectory(parent: FilePath, name: String): FilePath

    suspend fun rename(path: FilePath, newName: String): FilePath

    suspend fun move(path: FilePath, newParent: FilePath): FilePath

    suspend fun copy(path: FilePath, newParent: FilePath): FilePath

    suspend fun delete(path: FilePath)

    suspend fun exists(path: FilePath): Boolean

    suspend fun metadata(path: FilePath): FileNode?

    suspend fun listDirectoriesRecursively(limit: Int = 2000): List<FileNode>

    suspend fun listFilesRecursively(limit: Int = 20000): List<FileNode>

    fun absolutePathOf(path: FilePath): String

    fun watch(path: FilePath): Flow<FileChange>
}

interface SpaceFileSystemFactory {
    fun create(space: Space): SpaceFileSystem
}

expect fun createSpaceFileSystemFactory(): SpaceFileSystemFactory

fun uniqueName(existing: Set<String>, desired: String): String {
    if (desired !in existing) return desired
    val dotIndex = desired.lastIndexOf('.')
    val base = if (dotIndex > 0) desired.substring(0, dotIndex) else desired
    val suffix = if (dotIndex > 0) desired.substring(dotIndex) else ""
    var index = 1
    while (true) {
        val candidate = "$base $index$suffix"
        if (candidate !in existing) return candidate
        index++
    }
}
