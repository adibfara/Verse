package com.github.adibfara.verse.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class SortOrder {
    NameAscending,
    NameDescending,
    ModifiedNewest,
    ModifiedOldest;

    val label: String
        get() = when (this) {
            NameAscending -> "Name (A to Z)"
            NameDescending -> "Name (Z to A)"
            ModifiedNewest -> "Modified (newest first)"
            ModifiedOldest -> "Modified (oldest first)"
        }
}

fun List<FileNode>.sortedBy(order: SortOrder): List<FileNode> {
    val comparator = when (order) {
        SortOrder.NameAscending -> compareBy<FileNode> { it.name.lowercase() }
        SortOrder.NameDescending -> compareByDescending { it.name.lowercase() }
        SortOrder.ModifiedNewest -> compareByDescending { it.lastModified }
        SortOrder.ModifiedOldest -> compareBy { it.lastModified }
    }
    return sortedWith(compareByDescending<FileNode> { it.isDirectory }.then(comparator))
}
