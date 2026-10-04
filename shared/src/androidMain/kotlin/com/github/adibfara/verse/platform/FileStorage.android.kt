package com.github.adibfara.verse.core.persistence

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private class AndroidFileStorage : FileStorage {

    override suspend fun read(absolutePath: String): String? = withContext(Dispatchers.IO) {
        val file = File(absolutePath)
        if (file.isFile) runCatching { file.readText() }.getOrNull() else null
    }

    override suspend fun write(absolutePath: String, content: String) = withContext(Dispatchers.IO) {
        val file = File(absolutePath)
        file.parentFile?.mkdirs()
        val temporary = File("$absolutePath.tmp")
        temporary.writeText(content)
        if (!temporary.renameTo(file)) {
            file.writeText(content)
            temporary.delete()
        }
        Unit
    }

    override suspend fun exists(absolutePath: String): Boolean = withContext(Dispatchers.IO) {
        File(absolutePath).exists()
    }

    override fun join(base: String, vararg parts: String): String =
        parts.fold(File(base)) { accumulator, part -> File(accumulator, part) }.path
}

actual fun createFileStorage(): FileStorage = AndroidFileStorage()
