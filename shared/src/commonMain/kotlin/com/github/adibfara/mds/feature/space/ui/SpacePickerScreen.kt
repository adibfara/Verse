package com.github.adibfara.mds.feature.space.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.adibfara.mds.core.designsystem.component.MdsButton
import com.github.adibfara.mds.core.designsystem.component.MdsIconButton
import com.github.adibfara.mds.core.designsystem.component.MdsIcons
import com.github.adibfara.mds.core.designsystem.component.MdsText
import com.github.adibfara.mds.core.designsystem.theme.MdsTheme
import com.github.adibfara.mds.core.model.RecentSpace

@Composable
fun SpacePickerScreen(
    recentSpaces: List<RecentSpace>,
    canChooseFolder: Boolean,
    onOpenRecent: (RecentSpace) -> Unit,
    onForgetRecent: (RecentSpace) -> Unit,
    onChooseFolder: () -> Unit,
    modifier: Modifier = Modifier,
    showStorageOnboarding: Boolean = false,
    isStorageAccessGranted: Boolean = true,
    onRequestStorageAccess: () -> Unit = {},
    onUseInternalFolder: () -> Unit = {},
) {
    val colors = MdsTheme.colors
    Box(
        modifier = modifier.fillMaxSize().background(colors.window),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 540.dp)
                .padding(MdsTheme.spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(MdsTheme.spacing.lg),
        ) {
            MdsText(
                text = "MDs",
                style = MdsTheme.typography.editorHeading2,
                color = colors.textPrimary,
            )
            MdsText(
                text = "Open a folder as a space. Everything inside it becomes browsable, and your notes stay plain markdown on disk.",
                style = MdsTheme.typography.uiBody,
                color = colors.textSecondary,
            )

            if (showStorageOnboarding && !isStorageAccessGranted) {
                MdsButton(
                    label = "Grant full storage access",
                    onClick = onRequestStorageAccess,
                    primary = true,
                )
            }

            if (canChooseFolder) {
                MdsButton(
                    label = "Add a space...",
                    onClick = onChooseFolder,
                    primary = !showStorageOnboarding || isStorageAccessGranted,
                )
            }

            if (showStorageOnboarding) {
                MdsButton(
                    label = "Use the app's internal folder",
                    onClick = onUseInternalFolder,
                    primary = false,
                )
            }

            if (recentSpaces.isNotEmpty()) {
                MdsText(
                    text = "Recent spaces",
                    style = MdsTheme.typography.uiLabel,
                    color = colors.textMuted,
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .border(MdsTheme.sizes.borderWidth, colors.border, MdsTheme.shapes.medium),
                    verticalArrangement = Arrangement.spacedBy(MdsTheme.sizes.borderWidth),
                ) {
                    items(items = recentSpaces, key = { it.id.value }) { recent ->
                        RecentSpaceRow(
                            recent = recent,
                            onOpen = { onOpenRecent(recent) },
                            onForget = { onForgetRecent(recent) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentSpaceRow(
    recent: RecentSpace,
    onOpen: () -> Unit,
    onForget: () -> Unit,
) {
    val colors = MdsTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceRaised)
            .clickable(onClick = onOpen)
            .padding(horizontal = MdsTheme.spacing.md, vertical = MdsTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            MdsText(
                text = recent.name,
                style = MdsTheme.typography.uiTitle,
                color = colors.textPrimary,
                maxLines = 1,
            )
            MdsText(
                text = recent.location,
                style = MdsTheme.typography.uiSmall,
                color = colors.textMuted,
                maxLines = 1,
            )
        }
        MdsIconButton(
            icon = MdsIcons.Close,
            contentDescription = "Remove from recent spaces",
            onClick = onForget,
        )
    }
}
