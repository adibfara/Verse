package com.github.adibfara.verse.core.platform

interface LocationResolver {
    fun nameOf(location: String): String
    fun parentOf(location: String): String?
    fun isFile(location: String): Boolean
    fun isDirectory(location: String): Boolean
    fun relativize(root: String, location: String): String?
}

expect fun createLocationResolver(): LocationResolver
