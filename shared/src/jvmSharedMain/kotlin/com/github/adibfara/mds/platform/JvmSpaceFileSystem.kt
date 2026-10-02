package com.github.adibfara.mds.core.filesystem

import com.github.adibfara.mds.core.model.FileKind
import com.github.adibfara.mds.core.model.FileNode
import com.github.adibfara.mds.core.model.FilePath
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardCopyOption
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.TimeUnit
import kotlin.io.path.absolutePathString
import kotlin.io.path.name
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal const val MdsMetadataDirectory = ".mds"

open class JvmSpaceFileSystem(
    protected val root: Path,
) : SpaceFileSystem {

    override val rootLocation: String = root.absolutePathString()

    protected fun resolve(path: FilePath): Path {
        val resolved = if (path.isRoot) root else root.resolve(path.value).normalize()
        if (!resolved.startsWith(root)) {
            throw FileSystemException("Path escapes the space: ${path.value}")
        }
        return resolved
    }

    protected fun relativize(absolute: Path): FilePath {
        val relative = root.relativize(absolute).toString().replace('\\', '/')
        return FilePath(relative)
    }

    protected fun isHidden(name: String): Boolean = name.startsWith(".")

    private fun toNode(absolute: Path, attributes: BasicFileAttributes): FileNode {
        val path = relativize(absolute)
        val isDirectory = attributes.isDirectory
        return FileNode(
            path = path,
            name = absolute.name,
            isDirectory = isDirectory,
            kind = if (isDirectory) FileKind.Other else FileKind.fromExtension(path.extension),
            lastModified = attributes.lastModifiedTime().toMillis(),
            size = if (isDirectory) 0L else attributes.size(),
        )
    }

    private fun readNode(absolute: Path): FileNode? = runCatching {
        toNode(absolute, Files.readAttributes(absolute, BasicFileAttributes::class.java))
    }.getOrNull()

    override suspend fun listChildren(path: FilePath): List<FileNode> = withContext(Dispatchers.IO) {
        val directory = resolve(path)
        if (!Files.isDirectory(directory)) return@withContext emptyList()
        Files.newDirectoryStream(directory).use { stream ->
            stream.mapNotNull { child ->
                if (isHidden(child.name)) null else readNode(child)
            }
        }
    }

    override suspend fun readText(path: FilePath): String = withContext(Dispatchers.IO) {
        val file = resolve(path)
        if (!Files.isRegularFile(file)) throw FileSystemException("Not a file: ${path.value}")
        String(Files.readAllBytes(file), StandardCharsets.UTF_8)
    }

    override suspend fun readBytes(path: FilePath): ByteArray = withContext(Dispatchers.IO) {
        Files.readAllBytes(resolve(path))
    }

    override suspend fun writeText(path: FilePath, content: String) = withContext(Dispatchers.IO) {
        val file = resolve(path)
        Files.createDirectories(file.parent)
        val temporary = file.parent.resolve("${file.name}.mds-tmp")
        Files.write(temporary, content.toByteArray(StandardCharsets.UTF_8))
        runCatching {
            Files.move(
                temporary,
                file,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        }.recoverCatching {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING)
        }.onFailure {
            Files.deleteIfExists(temporary)
            throw FileSystemException("Could not write ${path.value}", it)
        }
        Unit
    }

    private fun takenNames(directory: Path): Set<String> =
        runCatching {
            Files.newDirectoryStream(directory).use { stream -> stream.map { it.name }.toSet() }
        }.getOrDefault(emptySet())

    override suspend fun createFile(parent: FilePath, name: String): FilePath =
        withContext(Dispatchers.IO) {
            val directory = resolve(parent)
            Files.createDirectories(directory)
            val finalName = uniqueName(takenNames(directory), name)
            val created = directory.resolve(finalName)
            Files.createFile(created)
            relativize(created)
        }

    override suspend fun createDirectory(parent: FilePath, name: String): FilePath =
        withContext(Dispatchers.IO) {
            val directory = resolve(parent)
            Files.createDirectories(directory)
            val finalName = uniqueName(takenNames(directory), name)
            val created = directory.resolve(finalName)
            Files.createDirectory(created)
            relativize(created)
        }

    override suspend fun rename(path: FilePath, newName: String): FilePath =
        withContext(Dispatchers.IO) {
            val source = resolve(path)
            val target = source.parent.resolve(newName)
            if (Files.exists(target)) throw FileSystemException("Already exists: $newName")
            Files.move(source, target)
            relativize(target)
        }

    override suspend fun move(path: FilePath, newParent: FilePath): FilePath =
        withContext(Dispatchers.IO) {
            val source = resolve(path)
            val destination = resolve(newParent)
            if (destination.startsWith(source)) {
                throw FileSystemException("Cannot move a folder into itself")
            }
            Files.createDirectories(destination)
            val finalName = uniqueName(takenNames(destination), source.name)
            val target = destination.resolve(finalName)
            Files.move(source, target)
            relativize(target)
        }

    override suspend fun copy(path: FilePath, newParent: FilePath): FilePath =
        withContext(Dispatchers.IO) {
            val source = resolve(path)
            val destination = resolve(newParent)
            Files.createDirectories(destination)
            val finalName = uniqueName(takenNames(destination), source.name)
            val target = destination.resolve(finalName)
            if (Files.isDirectory(source)) {
                Files.walkFileTree(
                    source,
                    object : SimpleFileVisitor<Path>() {
                        override fun preVisitDirectory(
                            directory: Path,
                            attributes: BasicFileAttributes,
                        ): FileVisitResult {
                            Files.createDirectories(target.resolve(source.relativize(directory).toString()))
                            return FileVisitResult.CONTINUE
                        }

                        override fun visitFile(
                            file: Path,
                            attributes: BasicFileAttributes,
                        ): FileVisitResult {
                            Files.copy(file, target.resolve(source.relativize(file).toString()))
                            return FileVisitResult.CONTINUE
                        }
                    },
                )
            } else {
                Files.copy(source, target)
            }
            relativize(target)
        }

    override suspend fun delete(path: FilePath) = withContext(Dispatchers.IO) {
        val target = resolve(path)
        if (!Files.exists(target)) return@withContext
        if (Files.isDirectory(target)) {
            Files.walkFileTree(
                target,
                object : SimpleFileVisitor<Path>() {
                    override fun visitFile(
                        file: Path,
                        attributes: BasicFileAttributes,
                    ): FileVisitResult {
                        Files.delete(file)
                        return FileVisitResult.CONTINUE
                    }

                    override fun postVisitDirectory(
                        directory: Path,
                        failure: IOException?,
                    ): FileVisitResult {
                        Files.delete(directory)
                        return FileVisitResult.CONTINUE
                    }
                },
            )
        } else {
            Files.delete(target)
        }
    }

    override suspend fun exists(path: FilePath): Boolean = withContext(Dispatchers.IO) {
        Files.exists(resolve(path))
    }

    override suspend fun metadata(path: FilePath): FileNode? = withContext(Dispatchers.IO) {
        readNode(resolve(path))
    }

    private suspend fun walk(limit: Int, directories: Boolean): List<FileNode> =
        withContext(Dispatchers.IO) {
            val collected = ArrayList<FileNode>()
            Files.walkFileTree(
                root,
                object : SimpleFileVisitor<Path>() {
                    override fun preVisitDirectory(
                        directory: Path,
                        attributes: BasicFileAttributes,
                    ): FileVisitResult {
                        if (directory != root && isHidden(directory.name)) {
                            return FileVisitResult.SKIP_SUBTREE
                        }
                        if (directories && directory != root) {
                            collected.add(toNode(directory, attributes))
                        }
                        return if (collected.size >= limit) {
                            FileVisitResult.TERMINATE
                        } else {
                            FileVisitResult.CONTINUE
                        }
                    }

                    override fun visitFile(
                        file: Path,
                        attributes: BasicFileAttributes,
                    ): FileVisitResult {
                        if (!directories && !isHidden(file.name)) {
                            collected.add(toNode(file, attributes))
                        }
                        return if (collected.size >= limit) {
                            FileVisitResult.TERMINATE
                        } else {
                            FileVisitResult.CONTINUE
                        }
                    }

                    override fun visitFileFailed(file: Path, failure: IOException): FileVisitResult =
                        FileVisitResult.CONTINUE
                },
            )
            collected
        }

    override suspend fun listDirectoriesRecursively(limit: Int): List<FileNode> =
        walk(limit, directories = true)

    override suspend fun listFilesRecursively(limit: Int): List<FileNode> =
        walk(limit, directories = false)

    override fun absolutePathOf(path: FilePath): String = resolve(path).absolutePathString()

    override fun watch(path: FilePath): Flow<FileChange> = callbackFlow {
        val directory = resolve(path)
        if (!Files.isDirectory(directory)) {
            close()
            return@callbackFlow
        }
        val service = directory.fileSystem.newWatchService()
        directory.register(
            service,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE,
            StandardWatchEventKinds.ENTRY_MODIFY,
        )
        val job = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                val key = service.poll(300, TimeUnit.MILLISECONDS) ?: continue
                key.pollEvents().forEach { event ->
                    @Suppress("UNCHECKED_CAST")
                    val typed = event as WatchEvent<Path>
                    val changed = directory.resolve(typed.context())
                    if (!isHidden(changed.name)) {
                        val relative = relativize(changed)
                        val change = when (typed.kind()) {
                            StandardWatchEventKinds.ENTRY_CREATE -> FileChange.Created(relative)
                            StandardWatchEventKinds.ENTRY_DELETE -> FileChange.Deleted(relative)
                            else -> FileChange.Modified(relative)
                        }
                        trySend(change)
                    }
                }
                if (!key.reset()) break
            }
        }
        awaitClose {
            job.cancel()
            runCatching { service.close() }
        }
    }.flowOn(Dispatchers.IO)
}
