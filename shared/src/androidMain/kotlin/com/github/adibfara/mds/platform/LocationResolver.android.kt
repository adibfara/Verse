package com.github.adibfara.mds.core.platform

private class AndroidLocationResolver : LocationResolver {

    override fun nameOf(location: String): String =
        location.trimEnd('/').substringAfterLast('/').ifEmpty { location }

    override fun parentOf(location: String): String? =
        location.trimEnd('/').substringBeforeLast('/', "").takeIf { it.isNotEmpty() }

    override fun isFile(location: String): Boolean = false

    override fun isDirectory(location: String): Boolean = true

    override fun relativize(root: String, location: String): String? =
        if (location.startsWith(root)) location.removePrefix(root).trimStart('/') else null
}

actual fun createLocationResolver(): LocationResolver = AndroidLocationResolver()
