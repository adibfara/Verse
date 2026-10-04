package com.github.adibfara.verse.feature.explorer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.github.adibfara.verse.core.designsystem.component.VerseDivider
import com.github.adibfara.verse.core.designsystem.component.VerseText
import com.github.adibfara.verse.core.designsystem.theme.VerseTheme
import com.github.adibfara.verse.core.persistence.ExplorerLayoutMode
import com.github.adibfara.verse.feature.explorer.ExplorerEvent
import com.github.adibfara.verse.feature.explorer.ExplorerUiState
import com.github.adibfara.verse.feature.explorer.TreeRow

@Composable
fun FileTree(
    state: ExplorerUiState,
    onEvent: (ExplorerEvent) -> Unit,
    modifier: Modifier = Modifier,
    showToolbar: Boolean = true,
    showFooter: Boolean = true,
) {
    val colors = VerseTheme.colors
    val listState = rememberLazyListState()

    val visibleRows = if (state.favoritesFilterEnabled) {
        state.rows.filter { row ->
            if (row.node.isDirectory) {
                state.favoritedPaths.any { it.isDescendantOf(row.node.path) }
            } else {
                row.node.path in state.favoritedPaths
            }
        }
    } else {
        state.rows
    }

    LaunchedEffect(state.selectedPath, visibleRows) {
        val target = state.selectedPath ?: return@LaunchedEffect
        val index = visibleRows.indexOfFirst { it.node.path == target }
        if (index >= 0) listState.animateScrollToItem(index)
    }

    Column(modifier = modifier.fillMaxSize().background(colors.sidebar)) {
        if (showToolbar) {
            ExplorerToolbar(
                sortOrder = state.sortOrder,
                onNewNote = { onEvent(ExplorerEvent.RequestNewNote(null)) },
                onNewFolder = { onEvent(ExplorerEvent.RequestNewFolder(null)) },
                onSortOrderChange = { onEvent(ExplorerEvent.ChangeSortOrder(it)) },
                onCollapseAll = { onEvent(ExplorerEvent.CollapseAll) },
                followEnabled = state.followEnabled,
                onToggleFollow = { onEvent(ExplorerEvent.ToggleFollowSelection) },
                dualPaneEnabled = state.layoutMode == ExplorerLayoutMode.DualPane,
                onToggleDualPane = { onEvent(ExplorerEvent.ToggleLayoutMode) },
                favoritesFilterEnabled = state.favoritesFilterEnabled,
                onToggleFavoritesFilter = { onEvent(ExplorerEvent.ToggleFavoritesFilter) },
            )
            VerseDivider()
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.isLoading -> TreeMessage("Loading...")
                visibleRows.isEmpty() -> TreeMessage(
                    if (state.favoritesFilterEnabled) "No favorites yet" else "This space is empty",
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(vertical = VerseTheme.spacing.xs),
                    state = listState,
                ) {
                    items(items = visibleRows, key = { row: TreeRow -> row.node.path.value }) { row ->
                        FileTreeRow(
                            row = row,
                            isSelected = state.selectedPath == row.node.path,
                            isFavorite = row.node.path in state.favoritedPaths,
                            onActivate = { onEvent(ExplorerEvent.Activate(row.node)) },
                            onSecondaryClick = { x, y ->
                                onEvent(ExplorerEvent.OpenContextMenu(row.node, x, y))
                            },
                        )
                    }
                }
            }
        }

        if (showFooter) {
            VerseDivider()
            SpaceFooter(spaceName = state.spaceName)
        }
    }
}

@Composable
private fun TreeMessage(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        VerseText(
            text = message,
            style = VerseTheme.typography.uiSmall,
            color = VerseTheme.colors.textMuted,
        )
    }
}

@Composable
internal fun SpaceFooter(spaceName: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = VerseTheme.spacing.md, vertical = VerseTheme.spacing.sm),
        contentAlignment = Alignment.CenterStart,
    ) {
        VerseText(
            text = spaceName,
            style = VerseTheme.typography.uiLabel,
            color = VerseTheme.colors.textPrimary,
            maxLines = 1,
        )
    }
}
