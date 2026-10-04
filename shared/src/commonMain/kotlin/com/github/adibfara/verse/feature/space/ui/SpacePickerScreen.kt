package com.github.adibfara.verse.feature.space.ui

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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.adibfara.verse.core.designsystem.component.VerseButton
import com.github.adibfara.verse.core.designsystem.component.VerseIconButton
import com.github.adibfara.verse.core.designsystem.component.VerseIcons
import com.github.adibfara.verse.core.designsystem.component.VerseText
import com.github.adibfara.verse.core.designsystem.theme.VerseTheme
import com.github.adibfara.verse.core.model.RecentSpace

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
    val colors = VerseTheme.colors
    Box(
        modifier = modifier.fillMaxSize().background(colors.window).safeDrawingPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 540.dp)
                .padding(VerseTheme.spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(VerseTheme.spacing.lg),
        ) {
            VerseText(
                text = "Verse",
                style = VerseTheme.typography.editorHeading2,
                color = colors.textPrimary,
            )
            VerseText(
                text = "Open a folder as a space. Everything inside it becomes browsable, and your notes stay plain markdown on disk.",
                style = VerseTheme.typography.uiBody,
                color = colors.textSecondary,
            )

            if (showStorageOnboarding && !isStorageAccessGranted) {
                VerseButton(
                    label = "Grant full storage access",
                    onClick = onRequestStorageAccess,
                    primary = true,
                )
            }

            if (canChooseFolder) {
                VerseButton(
                    label = "Add a space...",
                    onClick = onChooseFolder,
                    primary = !showStorageOnboarding || isStorageAccessGranted,
                )
            }

            if (showStorageOnboarding) {
                VerseButton(
                    label = "Use the app's internal folder",
                    onClick = onUseInternalFolder,
                    primary = false,
                )
            }

            if (recentSpaces.isNotEmpty()) {
                VerseText(
                    text = "Recent spaces",
                    style = VerseTheme.typography.uiLabel,
                    color = colors.textMuted,
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .border(VerseTheme.sizes.borderWidth, colors.border, VerseTheme.shapes.medium),
                    verticalArrangement = Arrangement.spacedBy(VerseTheme.sizes.borderWidth),
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
    val colors = VerseTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceRaised)
            .clickable(onClick = onOpen)
            .padding(horizontal = VerseTheme.spacing.md, vertical = VerseTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            VerseText(
                text = recent.name,
                style = VerseTheme.typography.uiTitle,
                color = colors.textPrimary,
                maxLines = 1,
            )
            VerseText(
                text = recent.location,
                style = VerseTheme.typography.uiSmall,
                color = colors.textMuted,
                maxLines = 1,
            )
        }
        VerseIconButton(
            icon = VerseIcons.Close,
            contentDescription = "Remove from recent spaces",
            onClick = onForget,
        )
    }
}
