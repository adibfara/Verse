package com.github.adibfara.verse.feature.explorer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import com.github.adibfara.verse.core.designsystem.component.VerseContextMenu
import com.github.adibfara.verse.core.designsystem.component.VerseIconButton
import com.github.adibfara.verse.core.designsystem.component.VerseIcons
import com.github.adibfara.verse.core.designsystem.component.MenuEntry
import com.github.adibfara.verse.core.designsystem.theme.VerseTheme
import com.github.adibfara.verse.core.model.SortOrder

@Composable
fun ExplorerToolbar(
    sortOrder: SortOrder,
    onNewNote: () -> Unit,
    onNewFolder: () -> Unit,
    onSortOrderChange: (SortOrder) -> Unit,
    onCollapseAll: () -> Unit,
    followEnabled: Boolean = false,
    onToggleFollow: () -> Unit = {},
    dualPaneEnabled: Boolean = false,
    onToggleDualPane: () -> Unit = {},
    favoritesFilterEnabled: Boolean = false,
    onToggleFavoritesFilter: () -> Unit = {},
    showSortOrder: Boolean = true,
    showFavoritesFilter: Boolean = true,
    modifier: Modifier = Modifier,
) {
    var sortMenuVisible by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(VerseTheme.sizes.toolbarHeight)
            .padding(horizontal = VerseTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VerseTheme.spacing.xxs),
    ) {
        VerseIconButton(
            icon = VerseIcons.FilePlus,
            contentDescription = "New note",
            onClick = onNewNote,
        )
        VerseIconButton(
            icon = VerseIcons.FolderPlus,
            contentDescription = "New folder",
            onClick = onNewFolder,
        )
        if (showSortOrder) {
            VerseIconButton(
                icon = VerseIcons.SortLines,
                contentDescription = "Change sort order",
                onClick = { sortMenuVisible = true },
                selected = sortMenuVisible,
            )
        }
        VerseIconButton(
            icon = VerseIcons.CollapseAll,
            contentDescription = "Collapse all",
            onClick = onCollapseAll,
        )
        VerseIconButton(
            icon = VerseIcons.Target,
            contentDescription = "Follow active file",
            onClick = onToggleFollow,
            selected = followEnabled,
        )
        VerseIconButton(
            icon = VerseIcons.Columns,
            contentDescription = "Toggle dual-pane layout",
            onClick = onToggleDualPane,
            selected = dualPaneEnabled,
        )
        if (showFavoritesFilter) {
            VerseIconButton(
                icon = VerseIcons.Heart,
                selectedIcon = VerseIcons.HeartFilled,
                contentDescription = "Show only favorites",
                onClick = onToggleFavoritesFilter,
                selected = favoritesFilterEnabled,
            )
        }

        if (sortMenuVisible) {
            VerseContextMenu(
                entries = SortOrder.entries.map { order ->
                    MenuEntry.Action(
                        label = order.label,
                        icon = if (order == sortOrder) VerseIcons.Check else null,
                        onClick = { onSortOrderChange(order) },
                    )
                },
                offset = IntOffset(0, 0),
                onDismiss = { sortMenuVisible = false },
            )
        }
    }
}
