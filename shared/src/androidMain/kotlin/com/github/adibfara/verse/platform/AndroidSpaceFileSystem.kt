package com.github.adibfara.verse.core.filesystem

import com.github.adibfara.verse.core.model.FilePath
import com.github.adibfara.verse.core.model.Space
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope

private const val PollIntervalMillis = 1500L

class AndroidSpaceFileSystem(root: Path) : JvmSpaceFileSystem(root) {

    override fun watch(path: FilePath): Flow<FileChange> = callbackFlow {
        val directory = resolve(path)
        if (!Files.isDirectory(directory)) {
            close()
            return@callbackFlow
        }
        var snapshot = snapshotOf(directory)
        val job = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                delay(PollIntervalMillis)
                val current = snapshotOf(directory)
                if (current != snapshot) {
                    val previousNames = snapshot.keys
                    val currentNames = current.keys
                    (currentNames - previousNames).forEach { name ->
                        trySend(FileChange.Created(relativize(directory.resolve(name))))
                    }
                    (previousNames - currentNames).forEach { name ->
                        trySend(FileChange.Deleted(relativize(directory.resolve(name))))
                    }
                    currentNames.intersect(previousNames).forEach { name ->
                        if (current[name] != snapshot[name]) {
                            trySend(FileChange.Modified(relativize(directory.resolve(name))))
                        }
                    }
                    snapshot = current
                }
            }
        }
        awaitClose { job.cancel() }
    }.flowOn(Dispatchers.IO)

    private fun snapshotOf(directory: Path): Map<String, Long> = runCatching {
        Files.newDirectoryStream(directory).use { stream ->
            stream
                .filter { !isHidden(it.fileName.toString()) }
                .associate { child ->
                    child.fileName.toString() to runCatching {
                        Files.getLastModifiedTime(child).toMillis()
                    }.getOrDefault(0L)
                }
        }
    }.getOrDefault(emptyMap())
}

private class AndroidSpaceFileSystemFactory : SpaceFileSystemFactory {

    private val instances = mutableMapOf<String, SpaceFileSystem>()

    override fun create(space: Space): SpaceFileSystem = instances.getOrPut(space.location) {
        AndroidSpaceFileSystem(Paths.get(space.location).toAbsolutePath().normalize())
    }
}

actual fun createSpaceFileSystemFactory(): SpaceFileSystemFactory = AndroidSpaceFileSystemFactory()
