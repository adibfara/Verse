package com.github.adibfara.mds.feature.explorer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.adibfara.mds.core.common.Debouncer
import com.github.adibfara.mds.core.common.DispatcherProvider
import com.github.adibfara.mds.core.filesystem.SpaceFileSystem
import com.github.adibfara.mds.core.model.FileKind
import com.github.adibfara.mds.core.model.FileNode
import com.github.adibfara.mds.core.model.FilePath
import com.github.adibfara.mds.core.model.SortOrder
import com.github.adibfara.mds.core.model.sortedBy
import com.github.adibfara.mds.core.persistence.ExplorerLayoutMode
import com.github.adibfara.mds.core.persistence.SettingsStore
import com.github.adibfara.mds.core.persistence.WorkspaceSessionRepository
import com.github.adibfara.mds.core.platform.SystemOpener
import com.github.adibfara.mds.feature.explorer.usecase.CopyNode
import com.github.adibfara.mds.feature.explorer.usecase.CreateFolder
import com.github.adibfara.mds.feature.explorer.usecase.CreateNote
import com.github.adibfara.mds.feature.explorer.usecase.DeleteNode
import com.github.adibfara.mds.feature.explorer.usecase.MoveNode
import com.github.adibfara.mds.feature.explorer.usecase.NameValidation
import com.github.adibfara.mds.feature.explorer.usecase.RenameNode
import com.github.adibfara.mds.feature.explorer.usecase.validateName
import com.github.adibfara.mds.feature.space.SpaceRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

class ExplorerViewModel(
    private val spaceRepository: SpaceRepository,
    private val sessionRepository: WorkspaceSessionRepository,
    private val settingsStore: SettingsStore,
    private val systemOpener: SystemOpener,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    private val mutableState = MutableStateFlow(ExplorerUiState())
    val state: StateFlow<ExplorerUiState> = mutableState.asStateFlow()

    private val mutableActions = MutableSharedFlow<ExplorerAction>(extraBufferCapacity = 16)
    val actions: SharedFlow<ExplorerAction> = mutableActions.asSharedFlow()

    private val children = mutableMapOf<FilePath, List<FileNode>>()
    private val expanded = mutableSetOf<FilePath>()
    private val watchers = mutableMapOf<FilePath, Job>()
    private val watchDebouncer = Debouncer(viewModelScope, 250L)

    private val fileSystem: SpaceFileSystem?
        get() = spaceRepository.current.value?.fileSystem

    private val favorites
        get() = spaceRepository.current.value?.favorites

    init {
        viewModelScope.launch {
            spaceRepository.current.collect { opened ->
                resetTree()
                if (opened == null) {
                    mutableState.value = ExplorerUiState()
                } else {
                    val session = sessionRepository.session.value
                    mutableState.value = ExplorerUiState(
                        spaceName = opened.space.name,
                        sortOrder = settingsStore.settings.value.sortOrder,
                        isLoading = true,
                        followEnabled = session.followSelectionEnabled,
                        layoutMode = session.explorerLayoutMode,
                        dualPaneSelectedFolder = session.dualPaneSelectedFolder,
                        favoritesFilterEnabled = session.favoritesFilterEnabled,
                    )
                    restoreExpanded()
                    loadDirectory(FilePath.Root)
                    expanded.toList().forEach { loadDirectory(it) }
                    mutableState.value = mutableState.value.copy(isLoading = false)
                    rebuildRows()
                    if (mutableState.value.layoutMode == ExplorerLayoutMode.DualPane) {
                        loadDualPaneFolders()
                        session.dualPaneSelectedFolder?.let { selectDualPaneFolder(it) }
                    }
                    refreshFavoritedPaths()
                }
            }
        }
        viewModelScope.launch {
            spaceRepository.current
                .flatMapLatest { opened -> opened?.favorites?.paths ?: flowOf(emptySet()) }
                .collect { paths ->
                    mutableState.value = mutableState.value.copy(favoritedPaths = paths)
                }
        }
    }

    fun onEvent(event: ExplorerEvent) {
        when (event) {
            is ExplorerEvent.ToggleFolder -> toggleFolder(event.path)
            is ExplorerEvent.Activate -> activate(event.node)
            is ExplorerEvent.Select -> mutableState.value =
                mutableState.value.copy(selectedPath = event.path)

            is ExplorerEvent.OpenContextMenu -> mutableState.value = mutableState.value.copy(
                contextMenu = ContextMenuTarget(event.node, event.x, event.y),
                selectedPath = event.node.path,
            )

            ExplorerEvent.DismissContextMenu -> mutableState.value =
                mutableState.value.copy(contextMenu = null)

            is ExplorerEvent.ChangeSortOrder -> changeSortOrder(event.order)
            ExplorerEvent.CollapseAll -> collapseAll()
            is ExplorerEvent.RequestNewNote -> openDialog(
                ExplorerDialog.NewNote(parent = resolveParent(event.parent), name = "Untitled"),
            )

            is ExplorerEvent.RequestNewFolder -> openDialog(
                ExplorerDialog.NewFolder(parent = resolveParent(event.parent), name = "New folder"),
            )

            is ExplorerEvent.RequestRename -> openDialog(
                ExplorerDialog.Rename(
                    target = event.node,
                    name = if (event.node.isDirectory) {
                        event.node.name
                    } else {
                        event.node.path.nameWithoutExtension
                    },
                ),
            )

            is ExplorerEvent.RequestMove -> requestMove(event.node)
            is ExplorerEvent.RequestDelete -> openDialog(ExplorerDialog.ConfirmDelete(event.node))
            is ExplorerEvent.Duplicate -> duplicate(event.node)
            is ExplorerEvent.OpenInDefaultApplication -> openExternally(event.node)
            is ExplorerEvent.RevealInFileManager -> reveal(event.node)
            is ExplorerEvent.DialogNameChanged -> updateDialogName(event.name)
            is ExplorerEvent.MoveQueryChanged -> updateMoveQuery(event.query)
            is ExplorerEvent.MoveDestinationSelected -> updateMoveDestination(event.path)
            ExplorerEvent.ConfirmDialog -> confirmDialog()
            ExplorerEvent.DismissDialog -> mutableState.value = mutableState.value.copy(dialog = null)
            ExplorerEvent.DismissError -> mutableState.value =
                mutableState.value.copy(errorMessage = null)

            is ExplorerEvent.Refresh -> viewModelScope.launch {
                loadDirectory(event.path)
                rebuildRows()
            }

            ExplorerEvent.ToggleFollowSelection -> toggleFollowSelection()
            ExplorerEvent.ToggleLayoutMode -> toggleLayoutMode()
            is ExplorerEvent.SelectDualPaneFolder -> selectDualPaneFolder(event.path)
            ExplorerEvent.ToggleFavoritesFilter -> toggleFavoritesFilter()
            is ExplorerEvent.ToggleFavorite -> toggleFavorite(event.node)
        }
    }

    private fun toggleFavorite(node: FileNode) {
        val favorites = favorites ?: return
        viewModelScope.launch { favorites.toggleFavorite(node.path) }
    }

    private fun toggleFollowSelection() {
        val enabled = !mutableState.value.followEnabled
        mutableState.value = mutableState.value.copy(followEnabled = enabled)
        sessionRepository.update { it.copy(followSelectionEnabled = enabled) }
    }

    private fun toggleFavoritesFilter() {
        val enabled = !mutableState.value.favoritesFilterEnabled
        mutableState.value = mutableState.value.copy(favoritesFilterEnabled = enabled)
        sessionRepository.update { it.copy(favoritesFilterEnabled = enabled) }
    }

    private fun refreshFavoritedPaths() {
        viewModelScope.launch {
            val paths = favorites?.favoritedPaths() ?: emptySet()
            mutableState.value = mutableState.value.copy(favoritedPaths = paths)
        }
    }

    private fun toggleLayoutMode() {
        val next = if (mutableState.value.layoutMode == ExplorerLayoutMode.Tree) {
            ExplorerLayoutMode.DualPane
        } else {
            ExplorerLayoutMode.Tree
        }
        mutableState.value = mutableState.value.copy(layoutMode = next)
        sessionRepository.update { it.copy(explorerLayoutMode = next) }
        if (next == ExplorerLayoutMode.DualPane) {
            loadDualPaneFolders()
            val selected = mutableState.value.dualPaneSelectedFolder ?: FilePath.Root
            selectDualPaneFolder(selected)
        }
    }

    private fun loadDualPaneFolders() {
        viewModelScope.launch {
            val target = fileSystem ?: return@launch
            val folders = runCatching { target.listDirectoriesRecursively() }.getOrElse { emptyList() }
            mutableState.value = mutableState.value.copy(dualPaneFolders = folders)
        }
    }

    private fun selectDualPaneFolder(path: FilePath) {
        viewModelScope.launch {
            val target = fileSystem ?: return@launch
            val files = runCatching { target.listChildren(path) }.getOrElse { emptyList() }
            mutableState.value = mutableState.value.copy(
                dualPaneSelectedFolder = path,
                dualPaneFiles = files,
            )
            sessionRepository.update { it.copy(dualPaneSelectedFolder = path) }
        }
    }

    fun revealPath(path: FilePath) {
        viewModelScope.launch {
            val ancestors = generateSequence(path.parent) { it.parent }.toList().reversed()
            ancestors.forEach { ancestor ->
                if (expanded.add(ancestor)) loadDirectory(ancestor)
            }
            persistExpanded()
            mutableState.value = mutableState.value.copy(selectedPath = path)
            rebuildRows()
        }
    }

    private fun resetTree() {
        watchers.values.forEach { it.cancel() }
        watchers.clear()
        children.clear()
        expanded.clear()
    }

    private fun restoreExpanded() {
        expanded.addAll(sessionRepository.session.value.expandedFolders)
    }

    private fun persistExpanded() {
        val snapshot = expanded.toList()
        sessionRepository.update { it.copy(expandedFolders = snapshot) }
    }

    private fun resolveParent(requested: FilePath?): FilePath {
        requested?.let { return it }
        val selected = mutableState.value.selectedPath ?: return FilePath.Root
        val node = findNode(selected) ?: return FilePath.Root
        return if (node.isDirectory) node.path else node.path.parent ?: FilePath.Root
    }

    private fun findNode(path: FilePath): FileNode? =
        children.values.firstNotNullOfOrNull { list -> list.firstOrNull { it.path == path } }

    private suspend fun loadDirectory(path: FilePath) {
        val target = fileSystem ?: return
        val loaded = runCatching { target.listChildren(path) }.getOrElse { emptyList() }
        children[path] = loaded
        startWatching(path)
    }

    private fun startWatching(path: FilePath) {
        if (watchers.containsKey(path)) return
        val target = fileSystem ?: return
        watchers[path] = viewModelScope.launch {
            target.watch(path).collect {
                loadDirectory(path)
                watchDebouncer.submit { rebuildRows() }
            }
        }
    }

    private fun toggleFolder(path: FilePath) {
        viewModelScope.launch {
            if (expanded.remove(path)) {
                collapseDescendants(path)
            } else {
                expanded.add(path)
                if (!children.containsKey(path)) loadDirectory(path)
            }
            persistExpanded()
            rebuildRows()
        }
    }

    private fun collapseDescendants(path: FilePath) {
        val removable = expanded.filter { it.isDescendantOf(path) }
        expanded.removeAll(removable.toSet())
        removable.forEach { watchers.remove(it)?.cancel() }
    }

    private fun collapseAll() {
        expanded.clear()
        watchers.keys.filterNot { it.isRoot }.forEach { watchers.remove(it)?.cancel() }
        persistExpanded()
        rebuildRows()
    }

    private fun changeSortOrder(order: SortOrder) {
        settingsStore.update { it.copy(sortOrder = order) }
        mutableState.value = mutableState.value.copy(sortOrder = order)
        rebuildRows()
    }

    private fun activate(node: FileNode) {
        if (node.isDirectory) {
            toggleFolder(node.path)
            return
        }
        mutableState.value = mutableState.value.copy(selectedPath = node.path)
        if (node.kind == FileKind.Other) {
            openExternally(node)
        } else {
            mutableActions.tryEmit(ExplorerAction.OpenFile(node))
        }
    }

    private fun openExternally(node: FileNode) {
        val target = fileSystem ?: return
        systemOpener.openInDefaultApplication(target.absolutePathOf(node.path))
    }

    private fun reveal(node: FileNode) {
        val target = fileSystem ?: return
        systemOpener.revealInFileManager(target.absolutePathOf(node.path))
    }

    private fun openDialog(dialog: ExplorerDialog) {
        mutableState.value = mutableState.value.copy(dialog = dialog, contextMenu = null)
    }

    private fun requestMove(node: FileNode) {
        viewModelScope.launch {
            val target = fileSystem ?: return@launch
            val folders = runCatching { target.listDirectoriesRecursively() }.getOrElse { emptyList() }
            val root = FileNode(
                path = FilePath.Root,
                name = spaceRepository.current.value?.space?.name ?: "Space",
                isDirectory = true,
                kind = FileKind.Other,
                lastModified = 0,
                size = 0,
            )
            val destinations = (listOf(root) + folders)
                .filterNot { it.path == node.path || it.path.isDescendantOf(node.path) }
            openDialog(
                ExplorerDialog.Move(
                    target = node,
                    query = "",
                    destinations = destinations,
                    selected = node.path.parent ?: FilePath.Root,
                ),
            )
        }
    }

    private fun updateDialogName(name: String) {
        val current = mutableState.value.dialog ?: return
        val updated = when (current) {
            is ExplorerDialog.NewNote -> current.copy(name = name, errorMessage = null)
            is ExplorerDialog.NewFolder -> current.copy(name = name, errorMessage = null)
            is ExplorerDialog.Rename -> current.copy(name = name, errorMessage = null)
            else -> current
        }
        mutableState.value = mutableState.value.copy(dialog = updated)
    }

    private fun updateMoveQuery(query: String) {
        val current = mutableState.value.dialog as? ExplorerDialog.Move ?: return
        mutableState.value = mutableState.value.copy(dialog = current.copy(query = query))
    }

    private fun updateMoveDestination(path: FilePath) {
        val current = mutableState.value.dialog as? ExplorerDialog.Move ?: return
        mutableState.value = mutableState.value.copy(dialog = current.copy(selected = path))
    }

    private fun duplicate(node: FileNode) {
        runOperation {
            val target = fileSystem ?: return@runOperation
            CopyNode(target)(node)
            loadDirectory(node.path.parent ?: FilePath.Root)
            rebuildRows()
        }
    }

    private fun confirmDialog() {
        val dialog = mutableState.value.dialog ?: return
        when (dialog) {
            is ExplorerDialog.NewNote -> confirmNamed(dialog.name) { target, name ->
                val created = CreateNote(target)(dialog.parent, name)
                expanded.add(dialog.parent)
                loadDirectory(dialog.parent)
                persistExpanded()
                rebuildRows()
                findNode(created)?.let {
                    mutableActions.tryEmit(ExplorerAction.OpenFile(it, focusContent = true))
                }
            }

            is ExplorerDialog.NewFolder -> confirmNamed(dialog.name) { target, name ->
                CreateFolder(target)(dialog.parent, name)
                expanded.add(dialog.parent)
                loadDirectory(dialog.parent)
                persistExpanded()
                rebuildRows()
            }

            is ExplorerDialog.Rename -> confirmNamed(dialog.name) { target, name ->
                val renamed = RenameNode(target)(dialog.target, name)
                loadDirectory(dialog.target.path.parent ?: FilePath.Root)
                rebuildRows()
                mutableActions.tryEmit(ExplorerAction.FileRenamed(dialog.target.path, renamed))
            }

            is ExplorerDialog.Move -> runOperation {
                val target = fileSystem ?: return@runOperation
                val moved = MoveNode(target)(dialog.target, dialog.selected)
                loadDirectory(dialog.target.path.parent ?: FilePath.Root)
                loadDirectory(dialog.selected)
                rebuildRows()
                mutableActions.tryEmit(ExplorerAction.FileRenamed(dialog.target.path, moved))
                mutableState.value = mutableState.value.copy(dialog = null)
            }

            is ExplorerDialog.ConfirmDelete -> runOperation {
                val target = fileSystem ?: return@runOperation
                DeleteNode(target)(dialog.target)
                loadDirectory(dialog.target.path.parent ?: FilePath.Root)
                rebuildRows()
                mutableActions.tryEmit(ExplorerAction.FileDeleted(dialog.target.path))
                mutableState.value = mutableState.value.copy(dialog = null)
            }
        }
    }

    private fun confirmNamed(
        name: String,
        block: suspend (SpaceFileSystem, String) -> Unit,
    ) {
        when (val validation = validateName(name)) {
            is NameValidation.Invalid -> showDialogError(validation.reason)
            NameValidation.Valid -> runOperation {
                val target = fileSystem ?: return@runOperation
                block(target, name.trim())
                mutableState.value = mutableState.value.copy(dialog = null)
            }
        }
    }

    private fun showDialogError(reason: String) {
        val current = mutableState.value.dialog ?: return
        val updated = when (current) {
            is ExplorerDialog.NewNote -> current.copy(errorMessage = reason)
            is ExplorerDialog.NewFolder -> current.copy(errorMessage = reason)
            is ExplorerDialog.Rename -> current.copy(errorMessage = reason)
            else -> current
        }
        mutableState.value = mutableState.value.copy(dialog = updated)
    }

    private fun runOperation(block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }.onFailure { failure ->
                mutableState.value = mutableState.value.copy(
                    errorMessage = failure.message ?: "Something went wrong",
                )
            }
        }
    }

    private fun rebuildRows() {
        val order = mutableState.value.sortOrder
        val rows = mutableListOf<TreeRow>()
        appendRows(FilePath.Root, 0, order, rows)
        mutableState.value = mutableState.value.copy(rows = rows)
    }

    private fun appendRows(
        parent: FilePath,
        depth: Int,
        order: SortOrder,
        output: MutableList<TreeRow>,
    ) {
        val nodes = children[parent] ?: return
        nodes.sortedBy(order).forEach { node ->
            val isExpanded = node.isDirectory && node.path in expanded
            output.add(TreeRow(node = node, depth = depth, isExpanded = isExpanded))
            if (isExpanded) appendRows(node.path, depth + 1, order, output)
        }
    }

    override fun onCleared() {
        resetTree()
        super.onCleared()
    }
}
