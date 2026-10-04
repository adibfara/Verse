package com.github.adibfara.mds.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.github.adibfara.mds.core.designsystem.component.MdsFloatingBar
import com.github.adibfara.mds.core.designsystem.component.MdsIcon
import com.github.adibfara.mds.core.designsystem.component.MdsIcons
import com.github.adibfara.mds.core.designsystem.component.MdsText
import com.github.adibfara.mds.core.designsystem.theme.MdsTheme

@Composable
fun MobileBottomBar(
    tabCount: Int,
    onSearch: () -> Unit,
    onAdd: () -> Unit,
    onTabs: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MdsFloatingBar(modifier = modifier) {
        BottomBarAction(icon = MdsIcons.Search, label = "Search", onClick = onSearch)
        BottomBarAction(icon = MdsIcons.Plus, label = "New note", onClick = onAdd)
        TabCountAction(tabCount = tabCount, onClick = onTabs)
        BottomBarAction(icon = MdsIcons.Menu, label = "Open menu", onClick = onMenu)
    }
}

@Composable
private fun BottomBarAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    ActionSlot(label = label, onClick = onClick) {
        MdsIcon(
            icon = icon,
            contentDescription = label,
            tint = MdsTheme.colors.textSecondary,
            size = MdsTheme.sizes.iconLarge,
        )
    }
}

@Composable
private fun TabCountAction(tabCount: Int, onClick: () -> Unit) {
    val colors = MdsTheme.colors
    ActionSlot(label = "Tabs", onClick = onClick) {
        Box(
            modifier = Modifier
                .widthIn(min = MdsTheme.sizes.badgeMinWidth)
                .background(colors.surfaceSelected, MdsTheme.shapes.medium)
                .padding(horizontal = MdsTheme.spacing.xs, vertical = MdsTheme.spacing.xxs),
            contentAlignment = Alignment.Center,
        ) {
            MdsText(
                text = tabCount.toString(),
                style = MdsTheme.typography.uiLabel,
                color = colors.textSecondary,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ActionSlot(
    label: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(MdsTheme.sizes.touchTarget)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = label,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
