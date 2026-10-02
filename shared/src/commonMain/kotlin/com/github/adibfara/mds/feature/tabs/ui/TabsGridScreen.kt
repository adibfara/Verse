package com.github.adibfara.mds.feature.tabs.ui

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
import com.github.adibfara.mds.core.designsystem.component.MdsIcon
import com.github.adibfara.mds.core.designsystem.component.MdsIconButton
import com.github.adibfara.mds.core.designsystem.component.MdsIcons
import com.github.adibfara.mds.core.designsystem.component.MdsText
import com.github.adibfara.mds.core.designsystem.theme.MdsTheme
import com.github.adibfara.mds.core.model.OpenTab
import com.github.adibfara.mds.core.model.TabId

@Composable
fun TabsGridScreen(
    tabs: List<OpenTab>,
    activeTabId: TabId?,
    onActivate: (TabId) -> Unit,
    onClose: (TabId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MdsTheme.colors
    if (tabs.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            MdsText(
                text = "No open tabs",
                style = MdsTheme.typography.uiBody,
                color = colors.textMuted,
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(MdsTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(MdsTheme.spacing.sm),
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
    val colors = MdsTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isActive) colors.surfaceSelected else colors.surfaceRaised, MdsTheme.shapes.medium)
            .border(MdsTheme.sizes.borderWidth, colors.border, MdsTheme.shapes.medium)
            .clickable(onClick = onActivate)
            .padding(horizontal = MdsTheme.spacing.lg, vertical = MdsTheme.spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MdsTheme.spacing.md),
    ) {
        MdsIcon(
            icon = MdsIcons.FileText,
            contentDescription = null,
            tint = colors.textMuted,
        )
        Column(modifier = Modifier.weight(1f)) {
            MdsText(
                text = tab.title,
                style = MdsTheme.typography.uiTitle,
                color = colors.textPrimary,
                maxLines = 1,
            )
            MdsText(
                text = tab.path.value,
                style = MdsTheme.typography.uiSmall,
                color = colors.textMuted,
                maxLines = 1,
            )
        }
        MdsIconButton(
            icon = MdsIcons.Close,
            contentDescription = "Close tab",
            onClick = onClose,
        )
    }
}
