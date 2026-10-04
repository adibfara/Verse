package com.github.adibfara.mds.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import com.github.adibfara.mds.core.designsystem.component.MdsContextMenu
import com.github.adibfara.mds.core.designsystem.component.MdsDivider
import com.github.adibfara.mds.core.designsystem.component.MdsIcon
import com.github.adibfara.mds.core.designsystem.component.MdsIcons
import com.github.adibfara.mds.core.designsystem.component.MdsText
import com.github.adibfara.mds.core.designsystem.component.MenuEntry
import com.github.adibfara.mds.core.designsystem.theme.MdsTheme
import com.github.adibfara.mds.core.model.RecentSpace
import com.github.adibfara.mds.feature.explorer.ExplorerEvent
import com.github.adibfara.mds.feature.explorer.ExplorerUiState
import com.github.adibfara.mds.feature.explorer.ui.FileTree

@Composable
fun MobileDrawerPanel(
    explorerState: ExplorerUiState,
    onExplorerEvent: (ExplorerEvent) -> Unit,
    recentSpaces: List<RecentSpace>,
    onOpenSpace: (RecentSpace) -> Unit,
    onAddSpace: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MdsTheme.colors.sidebar)
            .safeDrawingPadding(),
    ) {
        SpaceSwitcher(
            spaceName = explorerState.spaceName,
            recentSpaces = recentSpaces,
            onOpenSpace = onOpenSpace,
            onAddSpace = onAddSpace,
            onOpenSettings = onOpenSettings,
        )
        MdsDivider()
        Box(modifier = Modifier.fillMaxSize()) {
            FileTree(
                state = explorerState,
                onEvent = onExplorerEvent,
                modifier = Modifier.fillMaxSize(),
                showToolbar = true,
                showFooter = false,
            )
        }
    }
}

@Composable
private fun SpaceSwitcher(
    spaceName: String,
    recentSpaces: List<RecentSpace>,
    onOpenSpace: (RecentSpace) -> Unit,
    onAddSpace: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val colors = MdsTheme.colors
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(MdsTheme.sizes.mobileTopBarHeight)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { expanded = true }
                .padding(horizontal = MdsTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MdsTheme.spacing.xs),
        ) {
            MdsText(
                text = spaceName,
                modifier = Modifier.weight(1f, fill = false),
                style = MdsTheme.typography.uiTitle,
                color = colors.textPrimary,
                maxLines = 1,
            )
            MdsIcon(
                icon = MdsIcons.ChevronDown,
                contentDescription = "Switch space",
                tint = colors.textSecondary,
            )
        }
        if (expanded) {
            val others = recentSpaces.filter { it.name != spaceName }
            MdsContextMenu(
                entries = others.map { recent ->
                    MenuEntry.Action(
                        label = recent.name,
                        icon = MdsIcons.Folder,
                        onClick = { onOpenSpace(recent) },
                    )
                } + MenuEntry.Separator + MenuEntry.Action(
                    label = "Add a space...",
                    icon = MdsIcons.FolderPlus,
                    onClick = onAddSpace,
                ) + MenuEntry.Action(
                    label = "Options...",
                    icon = MdsIcons.Sliders,
                    onClick = onOpenSettings,
                ),
                offset = IntOffset.Zero,
                onDismiss = { expanded = false },
            )
        }
    }
}
