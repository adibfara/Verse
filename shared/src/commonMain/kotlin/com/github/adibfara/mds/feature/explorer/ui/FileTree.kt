package com.github.adibfara.mds.feature.explorer.ui

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.github.adibfara.mds.core.designsystem.component.MdsDivider
import com.github.adibfara.mds.core.designsystem.component.MdsText
import com.github.adibfara.mds.core.designsystem.theme.MdsTheme
import com.github.adibfara.mds.feature.explorer.ExplorerEvent
import com.github.adibfara.mds.feature.explorer.ExplorerUiState
import com.github.adibfara.mds.feature.explorer.TreeRow

@Composable
fun FileTree(
    state: ExplorerUiState,
    onEvent: (ExplorerEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MdsTheme.colors
    val listState = rememberLazyListState()

    Column(modifier = modifier.fillMaxSize().background(colors.sidebar)) {
        ExplorerToolbar(
            sortOrder = state.sortOrder,
            onNewNote = { onEvent(ExplorerEvent.RequestNewNote(null)) },
            onNewFolder = { onEvent(ExplorerEvent.RequestNewFolder(null)) },
            onSortOrderChange = { onEvent(ExplorerEvent.ChangeSortOrder(it)) },
            onCollapseAll = { onEvent(ExplorerEvent.CollapseAll) },
        )
        MdsDivider()

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.isLoading -> TreeMessage("Loading...")
                state.rows.isEmpty() -> TreeMessage("This space is empty")
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(vertical = MdsTheme.spacing.xs),
                    state = listState,
                ) {
                    items(items = state.rows, key = { row: TreeRow -> row.node.path.value }) { row ->
                        FileTreeRow(
                            row = row,
                            isSelected = state.selectedPath == row.node.path,
                            onActivate = { onEvent(ExplorerEvent.Activate(row.node)) },
                            onSecondaryClick = { x, y ->
                                onEvent(ExplorerEvent.OpenContextMenu(row.node, x, y))
                            },
                        )
                    }
                }
            }
        }

        MdsDivider()
        SpaceFooter(spaceName = state.spaceName)
    }
}

@Composable
private fun TreeMessage(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        MdsText(
            text = message,
            style = MdsTheme.typography.uiSmall,
            color = MdsTheme.colors.textMuted,
        )
    }
}

@Composable
private fun SpaceFooter(spaceName: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MdsTheme.spacing.md, vertical = MdsTheme.spacing.sm),
        contentAlignment = Alignment.CenterStart,
    ) {
        MdsText(
            text = spaceName,
            style = MdsTheme.typography.uiLabel,
            color = MdsTheme.colors.textPrimary,
            maxLines = 1,
        )
    }
}
