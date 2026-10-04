package com.github.adibfara.mds.feature.explorer.ui

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
import com.github.adibfara.mds.core.designsystem.component.MdsContextMenu
import com.github.adibfara.mds.core.designsystem.component.MdsIconButton
import com.github.adibfara.mds.core.designsystem.component.MdsIcons
import com.github.adibfara.mds.core.designsystem.component.MenuEntry
import com.github.adibfara.mds.core.designsystem.theme.MdsTheme
import com.github.adibfara.mds.core.model.SortOrder

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
    modifier: Modifier = Modifier,
) {
    var sortMenuVisible by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(MdsTheme.sizes.toolbarHeight)
            .padding(horizontal = MdsTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MdsTheme.spacing.xxs),
    ) {
        MdsIconButton(
            icon = MdsIcons.FilePlus,
            contentDescription = "New note",
            onClick = onNewNote,
        )
        MdsIconButton(
            icon = MdsIcons.FolderPlus,
            contentDescription = "New folder",
            onClick = onNewFolder,
        )
        MdsIconButton(
            icon = MdsIcons.SortLines,
            contentDescription = "Change sort order",
            onClick = { sortMenuVisible = true },
            selected = sortMenuVisible,
        )
        MdsIconButton(
            icon = MdsIcons.CollapseAll,
            contentDescription = "Collapse all",
            onClick = onCollapseAll,
        )
        MdsIconButton(
            icon = MdsIcons.Target,
            contentDescription = "Follow active file",
            onClick = onToggleFollow,
            selected = followEnabled,
        )
        MdsIconButton(
            icon = MdsIcons.Columns,
            contentDescription = "Toggle dual-pane layout",
            onClick = onToggleDualPane,
            selected = dualPaneEnabled,
        )
        MdsIconButton(
            icon = MdsIcons.Heart,
            selectedIcon = MdsIcons.HeartFilled,
            contentDescription = "Show only favorites",
            onClick = onToggleFavoritesFilter,
            selected = favoritesFilterEnabled,
        )

        if (sortMenuVisible) {
            MdsContextMenu(
                entries = SortOrder.entries.map { order ->
                    MenuEntry.Action(
                        label = order.label,
                        icon = if (order == sortOrder) MdsIcons.Check else null,
                        onClick = { onSortOrderChange(order) },
                    )
                },
                offset = IntOffset(0, 0),
                onDismiss = { sortMenuVisible = false },
            )
        }
    }
}
