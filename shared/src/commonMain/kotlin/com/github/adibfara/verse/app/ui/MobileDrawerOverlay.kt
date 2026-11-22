package com.github.adibfara.verse.app.ui

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
import com.github.adibfara.verse.core.designsystem.component.VerseContextMenu
import com.github.adibfara.verse.core.designsystem.component.VerseDivider
import com.github.adibfara.verse.core.designsystem.component.VerseIcon
import com.github.adibfara.verse.core.designsystem.component.VerseIcons
import com.github.adibfara.verse.core.designsystem.component.VerseText
import com.github.adibfara.verse.core.designsystem.component.MenuEntry
import com.github.adibfara.verse.core.designsystem.theme.VerseTheme
import com.github.adibfara.verse.core.model.RecentSpace
import com.github.adibfara.verse.feature.explorer.ExplorerEvent
import com.github.adibfara.verse.feature.explorer.ExplorerUiState
import com.github.adibfara.verse.feature.explorer.ui.FileTree

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
            .background(VerseTheme.colors.sidebar)
            .safeDrawingPadding(),
    ) {
        SpaceSwitcher(
            spaceName = explorerState.spaceName,
            recentSpaces = recentSpaces,
            onOpenSpace = onOpenSpace,
            onAddSpace = onAddSpace,
            onOpenSettings = onOpenSettings,
        )
        VerseDivider()
        Box(modifier = Modifier.fillMaxSize()) {
            FileTree(
                state = explorerState,
                onEvent = onExplorerEvent,
                modifier = Modifier.fillMaxSize(),
                showToolbar = true,
                showFooter = false,
                showListModes = true,
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
    val colors = VerseTheme.colors
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(VerseTheme.sizes.mobileTopBarHeight)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { expanded = true }
                .padding(horizontal = VerseTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(VerseTheme.spacing.xs),
        ) {
            VerseText(
                text = spaceName,
                modifier = Modifier.weight(1f, fill = false),
                style = VerseTheme.typography.uiTitle,
                color = colors.textPrimary,
                maxLines = 1,
            )
            VerseIcon(
                icon = VerseIcons.ChevronDown,
                contentDescription = "Switch space",
                tint = colors.textSecondary,
            )
        }
        if (expanded) {
            val others = recentSpaces.filter { it.name != spaceName }
            VerseContextMenu(
                entries = others.map { recent ->
                    MenuEntry.Action(
                        label = recent.name,
                        icon = VerseIcons.Folder,
                        onClick = { onOpenSpace(recent) },
                    )
                } + MenuEntry.Separator + MenuEntry.Action(
                    label = "Add a space...",
                    icon = VerseIcons.FolderPlus,
                    onClick = onAddSpace,
                ) + MenuEntry.Action(
                    label = "Options...",
                    icon = VerseIcons.Sliders,
                    onClick = onOpenSettings,
                ),
                offset = IntOffset.Zero,
                onDismiss = { expanded = false },
            )
        }
    }
}
