package com.github.adibfara.verse.core.model

import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class FilePath(val value: String) {

    val isRoot: Boolean get() = value.isEmpty()

    val name: String get() = value.substringAfterLast('/')

    val nameWithoutExtension: String get() = name.substringBeforeLast('.')

    val extension: String get() = name.substringAfterLast('.', "")

    val parent: FilePath? get() = if (isRoot) null else FilePath(value.substringBeforeLast('/', ""))

    val segments: List<String> get() = if (isRoot) emptyList() else value.split('/')

    fun child(childName: String): FilePath =
        if (isRoot) FilePath(childName) else FilePath("$value/$childName")

    fun isDescendantOf(other: FilePath): Boolean =
        other.isRoot || value.startsWith(other.value + "/")

    fun withName(newName: String): FilePath = parent?.child(newName) ?: FilePath(newName)

    override fun toString(): String = value

    companion object {
        val Root = FilePath("")
    }
}
