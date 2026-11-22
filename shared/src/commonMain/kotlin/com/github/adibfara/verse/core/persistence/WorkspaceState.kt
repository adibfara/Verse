package com.github.adibfara.verse.core.persistence

import com.github.adibfara.verse.core.model.EditorMode
import com.github.adibfara.verse.core.model.FileKind
import com.github.adibfara.verse.core.model.FileNode
import com.github.adibfara.verse.core.model.FilePath
import kotlinx.serialization.Serializable

@Serializable
data class WorkspaceTab(
    val path: FilePath,
    val mode: EditorMode = EditorMode.Preview,
    val scrollOffset: Int = 0,
)

@Serializable
data class RecentFileEntry(
    val path: FilePath,
    val lastModified: Long,
    val size: Long,
) {
    fun toFileNode(): FileNode = FileNode(
        path = path,
        name = path.name,
        isDirectory = false,
        kind = FileKind.fromExtension(path.extension),
        lastModified = lastModified,
        size = size,
    )

    companion object {
        fun of(node: FileNode): RecentFileEntry =
            RecentFileEntry(path = node.path, lastModified = node.lastModified, size = node.size)
    }
}

@Serializable
enum class ExplorerLayoutMode {
    Tree,
    DualPane,
}

@Serializable
data class WorkspaceState(
    val tabs: List<WorkspaceTab> = emptyList(),
    val activeTabIndex: Int = -1,
    val expandedFolders: List<FilePath> = emptyList(),
    val sidebarWidth: Float = 260f,
    val sidebarVisible: Boolean = true,
    val followSelectionEnabled: Boolean = false,
    val explorerLayoutMode: ExplorerLayoutMode = ExplorerLayoutMode.Tree,
    val dualPaneSelectedFolder: FilePath? = null,
    val favoritesFilterEnabled: Boolean = false,
    val recentFiles: List<RecentFileEntry> = emptyList(),
) {
    val activeTab: WorkspaceTab?
        get() = tabs.getOrNull(activeTabIndex)

    companion object {
        val Empty = WorkspaceState()
    }
}
