package com.github.adibfara.verse.feature.explorer

import com.github.adibfara.verse.core.model.FileNode
import com.github.adibfara.verse.core.model.FilePath
import com.github.adibfara.verse.core.model.SortOrder
import com.github.adibfara.verse.core.persistence.ExplorerLayoutMode

enum class ExplorerListMode(val label: String) {
    Recent("Recent"),
    All("All"),
    Favorites("Favorite"),
}

data class TreeRow(
    val node: FileNode,
    val depth: Int,
    val isExpanded: Boolean,
)

sealed interface ExplorerDialog {

    data class NewNote(
        val parent: FilePath,
        val name: String,
        val errorMessage: String? = null,
    ) : ExplorerDialog

    data class NewFolder(
        val parent: FilePath,
        val name: String,
        val errorMessage: String? = null,
    ) : ExplorerDialog

    data class Rename(
        val target: FileNode,
        val name: String,
        val errorMessage: String? = null,
    ) : ExplorerDialog

    data class Move(
        val target: FileNode,
        val query: String,
        val destinations: List<FileNode>,
        val selected: FilePath,
    ) : ExplorerDialog

    data class ConfirmDelete(val target: FileNode) : ExplorerDialog
}

data class ContextMenuTarget(
    val node: FileNode,
    val x: Int,
    val y: Int,
)

data class EmptyAreaMenuTarget(
    val x: Int,
    val y: Int,
)

data class ExplorerUiState(
    val spaceName: String = "",
    val rows: List<TreeRow> = emptyList(),
    val selectedPath: FilePath? = null,
    val sortOrder: SortOrder = SortOrder.NameAscending,
    val isLoading: Boolean = false,
    val dialog: ExplorerDialog? = null,
    val contextMenu: ContextMenuTarget? = null,
    val emptyAreaMenu: EmptyAreaMenuTarget? = null,
    val errorMessage: String? = null,
    val followEnabled: Boolean = false,
    val layoutMode: ExplorerLayoutMode = ExplorerLayoutMode.Tree,
    val dualPaneFolders: List<FileNode> = emptyList(),
    val dualPaneSelectedFolder: FilePath? = null,
    val dualPaneFiles: List<FileNode> = emptyList(),
    val favoritesFilterEnabled: Boolean = false,
    val favoritedPaths: Set<FilePath> = emptySet(),
    val listMode: ExplorerListMode = ExplorerListMode.All,
    val recentFiles: List<FileNode> = emptyList(),
    val favoriteNodes: List<FileNode> = emptyList(),
    val isListLoading: Boolean = false,
)
