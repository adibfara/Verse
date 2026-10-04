package com.github.adibfara.mds.feature.explorer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import com.github.adibfara.mds.core.designsystem.component.MdsIcon
import com.github.adibfara.mds.core.designsystem.component.MdsIcons
import com.github.adibfara.mds.core.designsystem.component.MdsText
import com.github.adibfara.mds.core.designsystem.component.itemActions
import com.github.adibfara.mds.core.designsystem.theme.MdsTheme
import com.github.adibfara.mds.feature.explorer.TreeRow

@Composable
fun FileTreeRow(
    row: TreeRow,
    isSelected: Boolean,
    onActivate: () -> Unit,
    onSecondaryClick: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val colors = MdsTheme.colors
    val sizes = MdsTheme.sizes
    var rowOrigin by remember { mutableStateOf(Offset.Zero) }

    val background = when {
        isSelected -> colors.surfaceSelected
        hovered -> colors.surfaceHover
        else -> Color.Transparent
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(sizes.rowHeight)
            .padding(horizontal = MdsTheme.spacing.xs, vertical = 1.dp)
            .background(background, MdsTheme.shapes.small)
            .hoverable(interactionSource)
            .onGloballyPositioned { rowOrigin = it.positionInRoot() }
            .itemActions(
                key = row.node.path,
                interactionSource = interactionSource,
                onActivate = onActivate,
                onContextMenu = { position ->
                    onSecondaryClick(
                        (rowOrigin.x + position.x).toInt(),
                        (rowOrigin.y + position.y).toInt(),
                    )
                },
            )
            .padding(start = sizes.indentStep * row.depth),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(sizes.icon), contentAlignment = Alignment.Center) {
            if (row.node.isDirectory) {
                MdsIcon(
                    icon = if (row.isExpanded) MdsIcons.ChevronDown else MdsIcons.ChevronRight,
                    contentDescription = if (row.isExpanded) "Collapse" else "Expand",
                    tint = colors.textMuted,
                    size = 14.dp,
                )
            }
        }
        Spacer(modifier = Modifier.width(MdsTheme.spacing.xs))
        MdsText(
            text = if (row.node.isDirectory) row.node.name else row.node.path.nameWithoutExtension,
            modifier = Modifier.weight(1f, fill = false),
            style = MdsTheme.typography.treeItem,
            color = if (row.node.isDirectory) colors.textPrimary else colors.textSecondary,
            maxLines = 1,
        )
        if (isFavorite) {
            Spacer(modifier = Modifier.width(MdsTheme.spacing.xs))
            MdsIcon(
                icon = MdsIcons.HeartFilled,
                contentDescription = "Favorite",
                tint = colors.accent,
                size = sizes.iconSmall,
            )
        }
    }
}
