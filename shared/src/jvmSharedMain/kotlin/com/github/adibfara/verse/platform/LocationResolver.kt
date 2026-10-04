package com.github.adibfara.verse.core.platform

import java.nio.file.Files
import java.nio.file.Paths

private class NioLocationResolver : LocationResolver {

    private fun normalized(location: String) = Paths.get(location).toAbsolutePath().normalize()

    override fun nameOf(location: String): String =
        normalized(location).fileName?.toString() ?: location

    override fun parentOf(location: String): String? =
        normalized(location).parent?.toString()

    override fun isFile(location: String): Boolean =
        runCatching { Files.isRegularFile(normalized(location)) }.getOrDefault(false)

    override fun isDirectory(location: String): Boolean =
        runCatching { Files.isDirectory(normalized(location)) }.getOrDefault(false)

    override fun relativize(root: String, location: String): String? = runCatching {
        val rootPath = normalized(root)
        val target = normalized(location)
        if (target.startsWith(rootPath)) {
            rootPath.relativize(target).toString().replace('\\', '/')
        } else {
            null
        }
    }.getOrNull()
}

actual fun createLocationResolver(): LocationResolver = NioLocationResolver()
