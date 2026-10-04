package com.github.adibfara.verse.core.persistence

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private class JvmFileStorage : FileStorage {

    override suspend fun read(absolutePath: String): String? = withContext(Dispatchers.IO) {
        val path = Paths.get(absolutePath)
        if (Files.isRegularFile(path)) runCatching { Files.readString(path) }.getOrNull() else null
    }

    override suspend fun write(absolutePath: String, content: String) = withContext(Dispatchers.IO) {
        val path = Paths.get(absolutePath)
        path.parent?.let { Files.createDirectories(it) }
        val temporary = Paths.get("$absolutePath.tmp")
        Files.writeString(temporary, content)
        runCatching {
            Files.move(
                temporary,
                path,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        }.recoverCatching {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING)
        }
        Unit
    }

    override suspend fun exists(absolutePath: String): Boolean = withContext(Dispatchers.IO) {
        Files.exists(Paths.get(absolutePath))
    }

    override fun join(base: String, vararg parts: String): String =
        parts.fold(Paths.get(base)) { accumulator, part -> accumulator.resolve(part) }.toString()
}

actual fun createFileStorage(): FileStorage = JvmFileStorage()
