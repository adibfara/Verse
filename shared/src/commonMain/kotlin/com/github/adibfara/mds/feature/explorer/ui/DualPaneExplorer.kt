package com.github.adibfara.mds.feature.explorer.ui

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
import com.github.adibfara.mds.core.designsystem.component.MdsDivider
import com.github.adibfara.mds.core.designsystem.component.MdsIcon
import com.github.adibfara.mds.core.designsystem.component.MdsIcons
import com.github.adibfara.mds.core.designsystem.component.MdsText
import com.github.adibfara.mds.core.designsystem.theme.MdsTheme
import com.github.adibfara.mds.core.model.FileNode
import com.github.adibfara.mds.core.model.FilePath
import com.github.adibfara.mds.core.platform.PlatformKind
import com.github.adibfara.mds.core.platform.currentPlatformKind
import com.github.adibfara.mds.feature.explorer.ExplorerEvent
import com.github.adibfara.mds.feature.explorer.ExplorerUiState

@Composable
fun DualPaneExplorer(
    state: ExplorerUiState,
    onEvent: (ExplorerEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MdsTheme.colors
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
        MdsDivider()

        if (currentPlatformKind() == PlatformKind.Desktop) {
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                FolderPane(
                    folders = state.dualPaneFolders,
                    selected = selectedFolder,
                    spaceName = state.spaceName,
                    onSelect = { onEvent(ExplorerEvent.SelectDualPaneFolder(it)) },
                    modifier = Modifier.weight(0.42f).fillMaxHeight(),
                )
                MdsDivider(vertical = true)
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
                MdsDivider()
                FilePane(
                    files = state.dualPaneFiles,
                    selectedPath = state.selectedPath,
                    onEvent = onEvent,
                    modifier = Modifier.weight(0.55f).fillMaxWidth(),
                )
            }
        }

        MdsDivider()
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
    val colors = MdsTheme.colors
    LazyColumn(modifier = modifier.fillMaxSize().padding(vertical = MdsTheme.spacing.xs)) {
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
    val colors = MdsTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) colors.surfaceSelected else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(
                start = MdsTheme.spacing.sm + MdsTheme.sizes.indentStep * depth,
                end = MdsTheme.spacing.sm,
                top = MdsTheme.spacing.xs,
                bottom = MdsTheme.spacing.xs,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MdsIcon(icon = MdsIcons.Folder, contentDescription = null, tint = colors.textMuted)
        MdsText(
            text = name,
            style = MdsTheme.typography.treeItem,
            color = colors.textPrimary,
            maxLines = 1,
            modifier = Modifier.padding(start = MdsTheme.spacing.xs),
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
    val colors = MdsTheme.colors
    val notes = files.filter { !it.isDirectory }
    if (notes.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            MdsText(text = "No notes here", style = MdsTheme.typography.uiSmall, color = colors.textMuted)
        }
        return
    }
    LazyColumn(modifier = modifier.fillMaxSize().padding(vertical = MdsTheme.spacing.xs)) {
        items(items = notes, key = { it.path.value }) { node ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (selectedPath == node.path) colors.surfaceSelected else Color.Transparent,
                    )
                    .clickable { onEvent(ExplorerEvent.Activate(node)) }
                    .padding(horizontal = MdsTheme.spacing.sm, vertical = MdsTheme.spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MdsIcon(icon = MdsIcons.FileText, contentDescription = null, tint = colors.textMuted)
                MdsText(
                    text = node.path.nameWithoutExtension,
                    style = MdsTheme.typography.treeItem,
                    color = colors.textSecondary,
                    maxLines = 1,
                    modifier = Modifier.padding(start = MdsTheme.spacing.xs),
                )
            }
        }
    }
}
