package com.github.adibfara.verse.core.persistence

interface FileStorage {
    suspend fun read(absolutePath: String): String?
    suspend fun write(absolutePath: String, content: String)
    suspend fun exists(absolutePath: String): Boolean
    fun join(base: String, vararg parts: String): String
}

expect fun createFileStorage(): FileStorage
