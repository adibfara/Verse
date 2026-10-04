package com.github.adibfara.verse.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.github.adibfara.verse.core.designsystem.component.VerseButton
import com.github.adibfara.verse.core.designsystem.component.VerseDialog
import com.github.adibfara.verse.core.designsystem.component.VerseText
import com.github.adibfara.verse.core.designsystem.theme.VerseTheme
import com.github.adibfara.verse.core.designsystem.theme.ThemeMode
import com.github.adibfara.verse.core.model.EditorMode
import com.github.adibfara.verse.core.model.SortOrder
import com.github.adibfara.verse.core.persistence.AppSettings
import com.github.adibfara.verse.core.persistence.LockBehavior
import com.github.adibfara.verse.core.persistence.StartupBehavior

@Composable
fun SettingsPanel(
    settings: AppSettings,
    sortOrder: SortOrder,
    followEnabled: Boolean,
    onSettingsChange: ((AppSettings) -> AppSettings) -> Unit,
    onSortOrderChange: (SortOrder) -> Unit,
    onToggleFollow: () -> Unit,
    onAddSpace: () -> Unit,
    onCloseSpace: () -> Unit,
    onDismiss: () -> Unit,
) {
    VerseDialog(
        title = "Options",
        onDismiss = onDismiss,
        actions = { VerseButton(label = "Done", onClick = onDismiss, primary = true) },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(VerseTheme.spacing.lg)) {
            SettingsSection(title = "Appearance") {
                OptionRow(
                    label = "Theme",
                    options = ThemeMode.entries.map { it to it.label },
                    selected = settings.themeMode,
                    onSelect = { mode -> onSettingsChange { it.copy(themeMode = mode) } },
                )
                OptionRow(
                    label = "Editor text size",
                    options = listOf(13.5f to "Small", 15.5f to "Medium", 18f to "Large"),
                    selected = settings.editorFontSize,
                    onSelect = { size -> onSettingsChange { it.copy(editorFontSize = size) } },
                )
                OptionRow(
                    label = "Status bar",
                    options = listOf(true to "Shown", false to "Hidden"),
                    selected = settings.showStatusBar,
                    onSelect = { shown -> onSettingsChange { it.copy(showStatusBar = shown) } },
                )
                OptionRow(
                    label = "Formatting toolbar",
                    options = listOf(true to "Shown", false to "Hidden"),
                    selected = settings.showFormattingToolbar,
                    onSelect = { shown ->
                        onSettingsChange { it.copy(showFormattingToolbar = shown) }
                    },
                )
            }

            SettingsSection(title = "Editing") {
                OptionRow(
                    label = "Default mode",
                    options = listOf(
                        EditorMode.Preview to "Rendered",
                        EditorMode.Live to "Markers on cursor line",
                        EditorMode.Raw to "Raw markdown",
                    ),
                    selected = settings.defaultEditorMode,
                    onSelect = { mode -> onSettingsChange { it.copy(defaultEditorMode = mode) } },
                )
                OptionRow(
                    label = "Autosave delay",
                    options = listOf(300L to "Fast", 800L to "Normal", 2000L to "Relaxed"),
                    selected = settings.autoSaveDelayMillis,
                    onSelect = { delay -> onSettingsChange { it.copy(autoSaveDelayMillis = delay) } },
                )
                OptionRow(
                    label = "Locked documents",
                    options = LockBehavior.entries.map { it to it.label },
                    selected = settings.lockBehavior,
                    onSelect = { behavior -> onSettingsChange { it.copy(lockBehavior = behavior) } },
                )
            }

            SettingsSection(title = "Files") {
                OptionRow(
                    label = "Sort by",
                    options = SortOrder.entries.map { it to it.label },
                    selected = sortOrder,
                    onSelect = onSortOrderChange,
                )
                OptionRow(
                    label = "Follow active file",
                    options = listOf(true to "On", false to "Off"),
                    selected = followEnabled,
                    onSelect = { enabled -> if (enabled != followEnabled) onToggleFollow() },
                )
            }

            SettingsSection(title = "On startup") {
                OptionRow(
                    label = "When MDs opens",
                    options = StartupBehavior.entries.map { it to it.label },
                    selected = settings.startupBehavior,
                    onSelect = { behavior ->
                        onSettingsChange { it.copy(startupBehavior = behavior) }
                    },
                )
            }

            SettingsSection(title = "Spaces") {
                Row(horizontalArrangement = Arrangement.spacedBy(VerseTheme.spacing.sm)) {
                    VerseButton(label = "Add a space...", onClick = onAddSpace)
                    VerseButton(label = "Close this space", onClick = onCloseSpace)
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(VerseTheme.spacing.sm)) {
        VerseText(
            text = title,
            style = VerseTheme.typography.uiLabel,
            color = VerseTheme.colors.textMuted,
        )
        content()
    }
}

@Composable
private fun <T> OptionRow(
    label: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    val colors = VerseTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VerseTheme.spacing.sm),
    ) {
        VerseText(
            text = label,
            modifier = Modifier.weight(1f),
            style = VerseTheme.typography.uiBody,
            color = colors.textSecondary,
        )
        options.forEach { (value, optionLabel) ->
            val isSelected = value == selected
            VerseText(
                text = optionLabel,
                modifier = Modifier
                    .background(
                        if (isSelected) colors.surfaceSelected else colors.surfaceHover,
                        VerseTheme.shapes.small,
                    )
                    .clickable { onSelect(value) }
                    .padding(horizontal = VerseTheme.spacing.sm, vertical = VerseTheme.spacing.xs),
                style = VerseTheme.typography.uiSmall,
                color = if (isSelected) colors.textPrimary else colors.textMuted,
                maxLines = 1,
            )
        }
    }
}
