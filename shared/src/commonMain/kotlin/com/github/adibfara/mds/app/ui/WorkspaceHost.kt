package com.github.adibfara.mds.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.github.adibfara.mds.app.WorkspaceViewModel
import com.github.adibfara.mds.core.filesystem.SpaceFileSystem
import com.github.adibfara.mds.core.model.FileKind
import com.github.adibfara.mds.core.model.FilePath
import com.github.adibfara.mds.feature.editor.EditorEvent
import com.github.adibfara.mds.feature.editor.EditorViewModel
import com.github.adibfara.mds.feature.explorer.ExplorerAction
import com.github.adibfara.mds.feature.explorer.ExplorerViewModel
import com.github.adibfara.mds.feature.editor.ui.preview.MarkdownImageLoader
import com.github.adibfara.mds.feature.space.SpaceRepository
import com.github.adibfara.mds.feature.tabs.TabsEvent
import com.github.adibfara.mds.feature.tabs.TabsUiState
import com.github.adibfara.mds.feature.tabs.TabsViewModel

@Composable
fun WorkspaceEffects(
    workspaceViewModel: WorkspaceViewModel,
    explorerViewModel: ExplorerViewModel,
    tabsViewModel: TabsViewModel,
    editorViewModel: EditorViewModel,
    tabsState: TabsUiState,
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
                is ExplorerAction.OpenFile -> tabsViewModel.onEvent(TabsEvent.Open(action.node))
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

    val activeTab = tabsState.activeTab
    LaunchedEffect(activeTab?.id) {
        if (activeTab == null) {
            editorViewModel.onEvent(EditorEvent.ClearActive)
        } else {
            editorViewModel.onEvent(EditorEvent.Open(activeTab.path, activeTab.kind))
        }
    }
}

@Composable
fun rememberMarkdownImageLoader(
    spaceRepository: SpaceRepository,
    activeFile: FilePath?,
): MarkdownImageLoader = remember(spaceRepository, activeFile) {
    MarkdownImageLoader { target ->
        val fileSystem = spaceRepository.current.value?.fileSystem ?: return@MarkdownImageLoader null
        val resolved = resolveImagePath(fileSystem, activeFile, target)
            ?: return@MarkdownImageLoader null
        val bytes = runCatching { fileSystem.readBytes(resolved) }.getOrNull()
            ?: return@MarkdownImageLoader null
        com.github.adibfara.mds.core.platform.decodeImageBitmap(bytes)
    }
}

private suspend fun resolveImagePath(
    fileSystem: SpaceFileSystem,
    activeFile: FilePath?,
    target: String,
): FilePath? {
    if (target.startsWith("http://") || target.startsWith("https://")) return null

    val cleaned = target.trim().removePrefix("./")
    val candidates = buildList {
        activeFile?.parent?.let { add(normalize(it.child(cleaned))) }
        add(FilePath(cleaned.trimStart('/')))
    }
    return candidates.firstOrNull { fileSystem.exists(it) }
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
