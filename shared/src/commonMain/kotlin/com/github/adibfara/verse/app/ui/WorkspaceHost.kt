package com.github.adibfara.verse.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import com.github.adibfara.verse.app.WorkspaceViewModel
import com.github.adibfara.verse.core.filesystem.SpaceFileSystem
import com.github.adibfara.verse.core.model.FileKind
import com.github.adibfara.verse.core.model.FilePath
import com.github.adibfara.verse.feature.editor.EditorAction
import com.github.adibfara.verse.feature.editor.EditorEvent
import com.github.adibfara.verse.feature.editor.EditorViewModel
import com.github.adibfara.verse.feature.explorer.ExplorerAction
import com.github.adibfara.verse.feature.explorer.ExplorerEvent
import com.github.adibfara.verse.feature.explorer.ExplorerUiState
import com.github.adibfara.verse.feature.explorer.ExplorerViewModel
import com.github.adibfara.verse.feature.viewer.ui.SpaceImageLoader
import com.github.adibfara.verse.feature.space.SpaceRepository
import com.github.adibfara.verse.feature.tabs.TabsEvent
import com.github.adibfara.verse.feature.tabs.TabsUiState
import com.github.adibfara.verse.feature.tabs.TabsViewModel
import kotlinx.coroutines.launch

@Composable
fun WorkspaceEffects(
    workspaceViewModel: WorkspaceViewModel,
    explorerViewModel: ExplorerViewModel,
    tabsViewModel: TabsViewModel,
    editorViewModel: EditorViewModel,
    tabsState: TabsUiState,
    explorerState: ExplorerUiState,
) {
    LaunchedEffect(workspaceViewModel) {
        workspaceViewModel.restores.collect { restore ->
            tabsViewModel.restore(restore.state.tabs, restore.state.activeTabIndex)
            restore.initialFile?.let { tabsViewModel.onEvent(TabsEvent.OpenPath(it)) }
        }
    }

    LaunchedEffect(explorerViewModel) {
        explorerViewModel.actions.collect { action ->
            when (action) {
                is ExplorerAction.OpenFile -> {
                    tabsViewModel.onEvent(TabsEvent.Open(action.node))
                    if (action.focusContent) {
                        editorViewModel.onEvent(EditorEvent.RequestFocusContent(action.node.path))
                    }
                }
                is ExplorerAction.OpenFileInNewTab ->
                    tabsViewModel.onEvent(TabsEvent.Open(action.node))

                is ExplorerAction.FileRenamed -> {
                    tabsViewModel.onEvent(TabsEvent.Renamed(action.from, action.to))
                    editorViewModel.onEvent(EditorEvent.Renamed(action.from, action.to))
                }

                is ExplorerAction.FileDeleted -> {
                    tabsViewModel.onEvent(TabsEvent.Removed(action.path))
                    editorViewModel.onEvent(EditorEvent.Close(action.path))
                }
            }
        }
    }

    LaunchedEffect(editorViewModel) {
        editorViewModel.dirtyChanges.collect { change ->
            tabsViewModel.onEvent(TabsEvent.MarkDirty(change.path, change.isDirty))
        }
    }

    LaunchedEffect(editorViewModel) {
        editorViewModel.actions.collect { action ->
            when (action) {
                is EditorAction.Renamed -> tabsViewModel.onEvent(TabsEvent.Renamed(action.from, action.to))
            }
        }
    }

    val activeTab = tabsState.activeTab
    LaunchedEffect(activeTab?.id) {
        if (activeTab == null) {
            editorViewModel.onEvent(EditorEvent.ClearActive)
        } else {
            editorViewModel.onEvent(EditorEvent.Open(activeTab.path, activeTab.kind))
            if (explorerViewModel.state.value.followEnabled) {
                explorerViewModel.revealPath(activeTab.path)
            } else {
                explorerViewModel.onEvent(ExplorerEvent.Select(activeTab.path))
            }
        }
    }

    LaunchedEffect(explorerState.followEnabled) {
        if (explorerState.followEnabled) {
            activeTab?.path?.let { explorerViewModel.revealPath(it) }
        }
    }
}

@Composable
fun rememberSpaceImageLoader(
    spaceRepository: SpaceRepository,
    activeFile: FilePath?,
): SpaceImageLoader = remember(spaceRepository, activeFile) {
    SpaceImageLoader { target ->
        val fileSystem = spaceRepository.current.value?.fileSystem ?: return@SpaceImageLoader null
        val resolved = resolveImagePath(fileSystem, activeFile, target)
            ?: return@SpaceImageLoader null
        val bytes = runCatching { fileSystem.readBytes(resolved) }.getOrNull()
            ?: return@SpaceImageLoader null
        com.github.adibfara.verse.core.platform.decodeImageBitmap(bytes)
    }
}

@Composable
fun rememberLinkOpener(
    spaceRepository: SpaceRepository,
    activeFile: FilePath?,
    onOpenPath: (FilePath) -> Unit,
    onOpenExternal: (String) -> Unit,
    onUnresolved: (String) -> Unit = {},
): (String) -> Unit {
    val scope = rememberCoroutineScope()
    val openPath by rememberUpdatedState(onOpenPath)
    val openExternal by rememberUpdatedState(onOpenExternal)
    val unresolved by rememberUpdatedState(onUnresolved)
    return remember(scope, spaceRepository, activeFile) {
        { target: String ->
            if (isExternalTarget(target)) {
                openExternal(target)
            } else {
                scope.launch {
                    val fileSystem = spaceRepository.current.value?.fileSystem
                    val resolved = fileSystem?.let {
                        resolveSpacePath(it, activeFile, target, allowMarkdownExtension = true)
                    }
                    if (resolved != null) openPath(resolved) else unresolved(target)
                }
                Unit
            }
        }
    }
}

private fun isExternalTarget(target: String): Boolean =
    target.startsWith("http://") ||
        target.startsWith("https://") ||
        target.startsWith("mailto:")

private suspend fun resolveImagePath(
    fileSystem: SpaceFileSystem,
    activeFile: FilePath?,
    target: String,
): FilePath? {
    if (isExternalTarget(target)) return null
    return resolveSpacePath(fileSystem, activeFile, target, allowMarkdownExtension = false)
}

private suspend fun resolveSpacePath(
    fileSystem: SpaceFileSystem,
    activeFile: FilePath?,
    target: String,
    allowMarkdownExtension: Boolean,
): FilePath? {
    val cleaned = target.trim().substringBefore('#').removePrefix("./")
    if (cleaned.isEmpty()) return null
    val names = if (allowMarkdownExtension && !cleaned.endsWith(".md")) {
        listOf(cleaned, "$cleaned.md")
    } else {
        listOf(cleaned)
    }
    names.forEach { name ->
        val candidates = buildList {
            activeFile?.parent?.let { add(normalize(it.child(name))) }
            add(FilePath(name.trimStart('/')))
        }
        candidates.firstOrNull { fileSystem.exists(it) }?.let { return it }
    }
    return null
}

private fun normalize(path: FilePath): FilePath {
    val segments = mutableListOf<String>()
    path.value.split('/').forEach { segment ->
        when (segment) {
            "", "." -> Unit
            ".." -> if (segments.isNotEmpty()) segments.removeAt(segments.lastIndex)
            else -> segments.add(segment)
        }
    }
    return FilePath(segments.joinToString("/"))
}

fun fileKindOf(path: FilePath): FileKind = FileKind.fromExtension(path.extension)
