package com.github.adibfara.mds.feature.explorer

import com.github.adibfara.mds.core.model.FileNode
import com.github.adibfara.mds.core.model.FilePath
import com.github.adibfara.mds.core.model.SortOrder

sealed interface ExplorerEvent {

    data class ToggleFolder(val path: FilePath) : ExplorerEvent

    data class Activate(val node: FileNode) : ExplorerEvent

    data class Select(val path: FilePath) : ExplorerEvent

    data class OpenContextMenu(val node: FileNode, val x: Int, val y: Int) : ExplorerEvent

    data object DismissContextMenu : ExplorerEvent

    data class ChangeSortOrder(val order: SortOrder) : ExplorerEvent

    data object CollapseAll : ExplorerEvent

    data class RequestNewNote(val parent: FilePath?) : ExplorerEvent

    data class RequestNewFolder(val parent: FilePath?) : ExplorerEvent

    data class RequestRename(val node: FileNode) : ExplorerEvent

    data class RequestMove(val node: FileNode) : ExplorerEvent

    data class RequestDelete(val node: FileNode) : ExplorerEvent

    data class Duplicate(val node: FileNode) : ExplorerEvent

    data class OpenInDefaultApplication(val node: FileNode) : ExplorerEvent

    data class RevealInFileManager(val node: FileNode) : ExplorerEvent

    data class DialogNameChanged(val name: String) : ExplorerEvent

    data class MoveQueryChanged(val query: String) : ExplorerEvent

    data class MoveDestinationSelected(val path: FilePath) : ExplorerEvent

    data object ConfirmDialog : ExplorerEvent

    data object DismissDialog : ExplorerEvent

    data object DismissError : ExplorerEvent

    data class Refresh(val path: FilePath) : ExplorerEvent

    data object ToggleFollowSelection : ExplorerEvent

    data object ToggleLayoutMode : ExplorerEvent

    data class SelectDualPaneFolder(val path: FilePath) : ExplorerEvent

    data object ToggleFavoritesFilter : ExplorerEvent
}

sealed interface ExplorerAction {
    data class OpenFile(val node: FileNode, val focusContent: Boolean = false) : ExplorerAction
    data class OpenFileInNewTab(val node: FileNode) : ExplorerAction
    data class FileRenamed(val from: FilePath, val to: FilePath) : ExplorerAction
    data class FileDeleted(val path: FilePath) : ExplorerAction
}
