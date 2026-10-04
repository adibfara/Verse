package com.github.adibfara.verse.core.persistence

import com.github.adibfara.verse.core.model.EditorMode
import com.github.adibfara.verse.core.model.FilePath
import kotlinx.serialization.Serializable

@Serializable
data class WorkspaceTab(
    val path: FilePath,
    val mode: EditorMode = EditorMode.Preview,
    val scrollOffset: Int = 0,
)

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
) {
    val activeTab: WorkspaceTab?
        get() = tabs.getOrNull(activeTabIndex)

    companion object {
        val Empty = WorkspaceState()
    }
}
