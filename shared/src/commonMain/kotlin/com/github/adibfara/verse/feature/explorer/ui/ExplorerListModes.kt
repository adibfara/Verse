package com.github.adibfara.verse.feature.explorer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import com.github.adibfara.verse.core.designsystem.component.VerseIcon
import com.github.adibfara.verse.core.designsystem.component.VerseIcons
import com.github.adibfara.verse.core.designsystem.component.VerseText
import com.github.adibfara.verse.core.designsystem.component.itemActions
import com.github.adibfara.verse.core.designsystem.theme.VerseTheme
import com.github.adibfara.verse.core.model.FileNode
import com.github.adibfara.verse.core.model.FilePath
import com.github.adibfara.verse.feature.explorer.ExplorerListMode

@Composable
fun ExplorerListTabs(
    selected: ExplorerListMode,
    onSelect: (ExplorerListMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(VerseTheme.sizes.tabHeight)
            .padding(horizontal = VerseTheme.spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(VerseTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ExplorerListMode.entries.forEach { mode ->
            ExplorerListTab(
                label = mode.label,
                isSelected = mode == selected,
                onClick = { onSelect(mode) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ExplorerListTab(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = VerseTheme.colors
    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = VerseTheme.spacing.xxs)
            .background(
                if (isSelected) colors.surfaceSelected else Color.Transparent,
                VerseTheme.shapes.small,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        VerseText(
            text = label,
            style = VerseTheme.typography.uiLabel,
            color = if (isSelected) colors.accent else colors.textSecondary,
            maxLines = 1,
        )
    }
}

@Composable
fun FlatFileList(
    nodes: List<FileNode>,
    selectedPath: FilePath?,
    favoritedPaths: Set<FilePath>,
    onActivate: (FileNode) -> Unit,
    onSecondaryClick: (FileNode, Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(vertical = VerseTheme.spacing.xs),
    ) {
        items(items = nodes, key = { node: FileNode -> node.path.value }) { node ->
            FlatFileRow(
                node = node,
                isSelected = node.path == selectedPath,
                isFavorite = node.path in favoritedPaths,
                onActivate = { onActivate(node) },
                onSecondaryClick = { x, y -> onSecondaryClick(node, x, y) },
            )
        }
    }
}

@Composable
private fun FlatFileRow(
    node: FileNode,
    isSelected: Boolean,
    isFavorite: Boolean,
    onActivate: () -> Unit,
    onSecondaryClick: (Int, Int) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val colors = VerseTheme.colors
    val sizes = VerseTheme.sizes
    var rowOrigin by remember { mutableStateOf(Offset.Zero) }
    val folder = node.path.parent?.segments.orEmpty().joinToString(" / ")

    val background = when {
        isSelected -> colors.surfaceSelected
        hovered -> colors.surfaceHover
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = VerseTheme.spacing.xs, vertical = VerseTheme.spacing.none)
            .background(background, VerseTheme.shapes.small)
            .hoverable(interactionSource)
            .onGloballyPositioned { rowOrigin = it.positionInRoot() }
            .itemActions(
                key = node.path,
                interactionSource = interactionSource,
                onActivate = onActivate,
                onContextMenu = { position ->
                    onSecondaryClick(
                        (rowOrigin.x + position.x).toInt(),
                        (rowOrigin.y + position.y).toInt(),
                    )
                },
            )
            .padding(horizontal = VerseTheme.spacing.sm, vertical = VerseTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VerseTheme.spacing.sm),
    ) {
        VerseIcon(
            icon = if (node.isDirectory) VerseIcons.Folder else VerseIcons.FileText,
            contentDescription = if (node.isDirectory) "Folder" else "File",
            tint = colors.textMuted,
            size = sizes.iconSmall,
        )
        Column(modifier = Modifier.weight(1f)) {
            if (folder.isNotEmpty()) {
                VerseText(
                    text = folder,
                    style = VerseTheme.typography.uiSmall,
                    color = colors.textMuted,
                    maxLines = 1,
                )
            }
            VerseText(
                text = if (node.isDirectory) node.name else node.path.nameWithoutExtension,
                style = VerseTheme.typography.treeItem,
                color = if (node.isDirectory) colors.textPrimary else colors.textSecondary,
                maxLines = 1,
            )
        }
        if (isFavorite) {
            VerseIcon(
                icon = VerseIcons.HeartFilled,
                contentDescription = "Favorite",
                tint = colors.accent,
                size = sizes.iconSmall,
            )
        }
    }
}
