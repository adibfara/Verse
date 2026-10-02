package com.github.adibfara.mds.core.persistence

import com.github.adibfara.mds.core.model.EditorMode
import com.github.adibfara.mds.core.model.FilePath
import kotlinx.serialization.Serializable

@Serializable
data class WorkspaceTab(
    val path: FilePath,
    val mode: EditorMode = EditorMode.Live,
    val scrollOffset: Int = 0,
)

@Serializable
data class WorkspaceState(
    val tabs: List<WorkspaceTab> = emptyList(),
    val activeTabIndex: Int = -1,
    val expandedFolders: List<FilePath> = emptyList(),
    val sidebarWidth: Float = 260f,
    val sidebarVisible: Boolean = true,
) {
    val activeTab: WorkspaceTab?
        get() = tabs.getOrNull(activeTabIndex)

    companion object {
        val Empty = WorkspaceState()
    }
}
