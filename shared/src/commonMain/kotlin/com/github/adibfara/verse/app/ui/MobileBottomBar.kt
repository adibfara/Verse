package com.github.adibfara.verse.app.ui

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
import com.github.adibfara.verse.core.designsystem.component.VerseFloatingBar
import com.github.adibfara.verse.core.designsystem.component.VerseIcon
import com.github.adibfara.verse.core.designsystem.component.VerseIcons
import com.github.adibfara.verse.core.designsystem.component.VerseText
import com.github.adibfara.verse.core.designsystem.theme.VerseTheme

@Composable
fun MobileBottomBar(
    tabCount: Int,
    onSearch: () -> Unit,
    onAdd: () -> Unit,
    onTabs: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    VerseFloatingBar(modifier = modifier) {
        BottomBarAction(icon = VerseIcons.Search, label = "Search", onClick = onSearch)
        BottomBarAction(icon = VerseIcons.Plus, label = "New note", onClick = onAdd)
        TabCountAction(tabCount = tabCount, onClick = onTabs)
        BottomBarAction(icon = VerseIcons.Menu, label = "Open menu", onClick = onMenu)
    }
}

@Composable
private fun BottomBarAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    ActionSlot(label = label, onClick = onClick) {
        VerseIcon(
            icon = icon,
            contentDescription = label,
            tint = VerseTheme.colors.textSecondary,
            size = VerseTheme.sizes.iconLarge,
        )
    }
}

@Composable
private fun TabCountAction(tabCount: Int, onClick: () -> Unit) {
    val colors = VerseTheme.colors
    ActionSlot(label = "Tabs", onClick = onClick) {
        Box(
            modifier = Modifier
                .widthIn(min = VerseTheme.sizes.badgeMinWidth)
                .background(colors.surfaceSelected, VerseTheme.shapes.medium)
                .padding(horizontal = VerseTheme.spacing.xs, vertical = VerseTheme.spacing.xxs),
            contentAlignment = Alignment.Center,
        ) {
            VerseText(
                text = tabCount.toString(),
                style = VerseTheme.typography.uiLabel,
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
            .size(VerseTheme.sizes.touchTarget)
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
