package com.github.adibfara.verse.feature.explorer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.github.adibfara.verse.core.designsystem.component.VerseDivider
import com.github.adibfara.verse.core.designsystem.component.VerseIcon
import com.github.adibfara.verse.core.designsystem.component.VerseIcons
import com.github.adibfara.verse.core.designsystem.component.VerseText
import com.github.adibfara.verse.core.designsystem.theme.VerseTheme
import com.github.adibfara.verse.core.model.FileNode
import com.github.adibfara.verse.core.model.FilePath
import com.github.adibfara.verse.core.platform.PlatformKind
import com.github.adibfara.verse.core.platform.currentPlatformKind
import com.github.adibfara.verse.feature.explorer.ExplorerEvent
import com.github.adibfara.verse.feature.explorer.ExplorerUiState

@Composable
fun DualPaneExplorer(
    state: ExplorerUiState,
    onEvent: (ExplorerEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = VerseTheme.colors
    val selectedFolder = state.dualPaneSelectedFolder ?: FilePath.Root

    Column(modifier = modifier.fillMaxSize().background(colors.sidebar)) {
        ExplorerToolbar(
            sortOrder = state.sortOrder,
            onNewNote = { onEvent(ExplorerEvent.RequestNewNote(selectedFolder)) },
            onNewFolder = { onEvent(ExplorerEvent.RequestNewFolder(selectedFolder)) },
            onSortOrderChange = { onEvent(ExplorerEvent.ChangeSortOrder(it)) },
            onCollapseAll = {},
            followEnabled = state.followEnabled,
            onToggleFollow = { onEvent(ExplorerEvent.ToggleFollowSelection) },
            dualPaneEnabled = true,
            onToggleDualPane = { onEvent(ExplorerEvent.ToggleLayoutMode) },
            favoritesFilterEnabled = state.favoritesFilterEnabled,
            onToggleFavoritesFilter = { onEvent(ExplorerEvent.ToggleFavoritesFilter) },
        )
        VerseDivider()

        if (currentPlatformKind() == PlatformKind.Desktop) {
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                FolderPane(
                    folders = state.dualPaneFolders,
                    selected = selectedFolder,
                    spaceName = state.spaceName,
                    onSelect = { onEvent(ExplorerEvent.SelectDualPaneFolder(it)) },
                    modifier = Modifier.weight(0.42f).fillMaxHeight(),
                )
                VerseDivider(vertical = true)
                FilePane(
                    files = state.dualPaneFiles,
                    selectedPath = state.selectedPath,
                    onEvent = onEvent,
                    modifier = Modifier.weight(0.58f).fillMaxHeight(),
                )
            }
        } else {
            Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                FolderPane(
                    folders = state.dualPaneFolders,
                    selected = selectedFolder,
                    spaceName = state.spaceName,
                    onSelect = { onEvent(ExplorerEvent.SelectDualPaneFolder(it)) },
                    modifier = Modifier.weight(0.45f).fillMaxWidth(),
                )
                VerseDivider()
                FilePane(
                    files = state.dualPaneFiles,
                    selectedPath = state.selectedPath,
                    onEvent = onEvent,
                    modifier = Modifier.weight(0.55f).fillMaxWidth(),
                )
            }
        }

        VerseDivider()
        SpaceFooter(spaceName = state.spaceName)
    }
}

@Composable
private fun FolderPane(
    folders: List<FileNode>,
    selected: FilePath,
    spaceName: String,
    onSelect: (FilePath) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = VerseTheme.colors
    LazyColumn(modifier = modifier.fillMaxSize().padding(vertical = VerseTheme.spacing.xs)) {
        item(key = "__root__") {
            FolderRow(
                name = spaceName,
                depth = 0,
                isSelected = selected.isRoot,
                onClick = { onSelect(FilePath.Root) },
            )
        }
        items(items = folders, key = { it.path.value }) { folder ->
            FolderRow(
                name = folder.name,
                depth = folder.path.segments.size,
                isSelected = folder.path == selected,
                onClick = { onSelect(folder.path) },
            )
        }
    }
}

@Composable
private fun FolderRow(
    name: String,
    depth: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = VerseTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) colors.surfaceSelected else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(
                start = VerseTheme.spacing.sm + VerseTheme.sizes.indentStep * depth,
                end = VerseTheme.spacing.sm,
                top = VerseTheme.spacing.xs,
                bottom = VerseTheme.spacing.xs,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VerseIcon(icon = VerseIcons.Folder, contentDescription = null, tint = colors.textMuted)
        VerseText(
            text = name,
            style = VerseTheme.typography.treeItem,
            color = colors.textPrimary,
            maxLines = 1,
            modifier = Modifier.padding(start = VerseTheme.spacing.xs),
        )
    }
}

@Composable
private fun FilePane(
    files: List<FileNode>,
    selectedPath: FilePath?,
    onEvent: (ExplorerEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = VerseTheme.colors
    val notes = files.filter { !it.isDirectory }
    if (notes.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            VerseText(text = "No notes here", style = VerseTheme.typography.uiSmall, color = colors.textMuted)
        }
        return
    }
    LazyColumn(modifier = modifier.fillMaxSize().padding(vertical = VerseTheme.spacing.xs)) {
        items(items = notes, key = { it.path.value }) { node ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (selectedPath == node.path) colors.surfaceSelected else Color.Transparent,
                    )
                    .clickable { onEvent(ExplorerEvent.Activate(node)) }
                    .padding(horizontal = VerseTheme.spacing.sm, vertical = VerseTheme.spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VerseIcon(icon = VerseIcons.FileText, contentDescription = null, tint = colors.textMuted)
                VerseText(
                    text = node.path.nameWithoutExtension,
                    style = VerseTheme.typography.treeItem,
                    color = colors.textSecondary,
                    maxLines = 1,
                    modifier = Modifier.padding(start = VerseTheme.spacing.xs),
                )
            }
        }
    }
}
