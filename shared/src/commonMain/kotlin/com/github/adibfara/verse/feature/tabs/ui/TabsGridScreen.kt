package com.github.adibfara.verse.feature.tabs.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.github.adibfara.verse.core.designsystem.component.VerseIcon
import com.github.adibfara.verse.core.designsystem.component.VerseIconButton
import com.github.adibfara.verse.core.designsystem.component.VerseIcons
import com.github.adibfara.verse.core.designsystem.component.VerseText
import com.github.adibfara.verse.core.designsystem.theme.VerseTheme
import com.github.adibfara.verse.core.model.OpenTab
import com.github.adibfara.verse.core.model.TabId

@Composable
fun TabsGridScreen(
    tabs: List<OpenTab>,
    activeTabId: TabId?,
    onActivate: (TabId) -> Unit,
    onClose: (TabId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = VerseTheme.colors
    if (tabs.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            VerseText(
                text = "No open tabs",
                style = VerseTheme.typography.uiBody,
                color = colors.textMuted,
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(VerseTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(VerseTheme.spacing.sm),
    ) {
        items(items = tabs, key = { it.id.value }) { tab ->
            TabCard(
                tab = tab,
                isActive = tab.id == activeTabId,
                onActivate = { onActivate(tab.id) },
                onClose = { onClose(tab.id) },
            )
        }
    }
}

@Composable
private fun TabCard(
    tab: OpenTab,
    isActive: Boolean,
    onActivate: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = VerseTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isActive) colors.surfaceSelected else colors.surfaceRaised, VerseTheme.shapes.medium)
            .border(VerseTheme.sizes.borderWidth, colors.border, VerseTheme.shapes.medium)
            .clickable(onClick = onActivate)
            .padding(horizontal = VerseTheme.spacing.lg, vertical = VerseTheme.spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VerseTheme.spacing.md),
    ) {
        VerseIcon(
            icon = VerseIcons.FileText,
            contentDescription = null,
            tint = colors.textMuted,
        )
        Column(modifier = Modifier.weight(1f)) {
            VerseText(
                text = tab.title,
                style = VerseTheme.typography.uiTitle,
                color = colors.textPrimary,
                maxLines = 1,
            )
            VerseText(
                text = tab.path.value,
                style = VerseTheme.typography.uiSmall,
                color = colors.textMuted,
                maxLines = 1,
            )
        }
        VerseIconButton(
            icon = VerseIcons.Close,
            contentDescription = "Close tab",
            onClick = onClose,
        )
    }
}
