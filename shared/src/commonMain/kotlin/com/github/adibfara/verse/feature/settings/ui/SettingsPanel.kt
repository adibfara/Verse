package com.github.adibfara.verse.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import com.github.adibfara.verse.core.command.Command
import com.github.adibfara.verse.core.command.CommandId
import com.github.adibfara.verse.core.command.Commands
import com.github.adibfara.verse.core.command.KeyBinding
import com.github.adibfara.verse.core.command.ShortcutKey
import com.github.adibfara.verse.core.command.toBinding
import com.github.adibfara.verse.core.designsystem.component.VerseButton
import com.github.adibfara.verse.core.designsystem.component.VerseDialog
import com.github.adibfara.verse.core.designsystem.component.VerseStepper
import com.github.adibfara.verse.core.designsystem.component.VerseText
import com.github.adibfara.verse.core.designsystem.theme.VerseTheme
import com.github.adibfara.verse.core.designsystem.theme.ThemeMode
import com.github.adibfara.verse.core.model.EditorMode
import com.github.adibfara.verse.core.model.SortOrder
import com.github.adibfara.verse.core.persistence.AppSettings
import com.github.adibfara.verse.core.persistence.EditorFontSizeStep
import com.github.adibfara.verse.core.persistence.LockBehavior
import com.github.adibfara.verse.core.persistence.MaxEditorFontSize
import com.github.adibfara.verse.core.persistence.MinEditorFontSize
import com.github.adibfara.verse.core.persistence.StartupBehavior
import com.github.adibfara.verse.core.persistence.formatEditorFontSize
import com.github.adibfara.verse.core.persistence.withAdjustedEditorFontSize

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
    var recording by remember { mutableStateOf<CommandId?>(null) }
    val recorderFocus = remember { FocusRequester() }

    LaunchedEffect(recording) {
        if (recording != null) recorderFocus.requestFocus()
    }

    val overrides = settings.keyBindingOverrides
    val effectiveBinding: (Command) -> KeyBinding? = { command ->
        if (overrides.containsKey(command.id.value)) overrides[command.id.value] else command.binding
    }

    fun assignBinding(command: Command, binding: KeyBinding) {
        onSettingsChange { current ->
            val updated = current.keyBindingOverrides.toMutableMap()
            Commands.all.forEach { other ->
                if (other.id != command.id) {
                    val effective = if (updated.containsKey(other.id.value)) {
                        updated[other.id.value]
                    } else {
                        other.binding
                    }
                    if (effective == binding) updated[other.id.value] = null
                }
            }
            updated[command.id.value] = binding
            current.copy(keyBindingOverrides = updated)
        }
        recording = null
    }

    VerseDialog(
        title = "Options",
        onDismiss = onDismiss,
        actions = { VerseButton(label = "Done", onClick = onDismiss, primary = true) },
    ) {
        Column(
            modifier = Modifier
                .focusRequester(recorderFocus)
                .focusable()
                .onPreviewKeyEvent { event ->
                    val target = recording ?: return@onPreviewKeyEvent false
                    val binding = event.toBinding() ?: return@onPreviewKeyEvent true
                    when {
                        binding.key == ShortcutKey.Escape -> recording = null
                        binding.control || binding.alt -> Commands.all
                            .firstOrNull { it.id == target }
                            ?.let { command -> assignBinding(command, binding) }
                    }
                    true
                },
            verticalArrangement = Arrangement.spacedBy(VerseTheme.spacing.lg),
        ) {
            SettingsSection(title = "Appearance") {
                OptionRow(
                    label = "Theme",
                    options = ThemeMode.entries.map { it to it.label },
                    selected = settings.themeMode,
                    onSelect = { mode -> onSettingsChange { it.copy(themeMode = mode) } },
                )
                FontSizeRow(
                    value = settings.editorFontSize,
                    onAdjust = { delta ->
                        onSettingsChange { it.withAdjustedEditorFontSize(delta) }
                    },
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

            SettingsSection(title = "Keybindings") {
                VerseText(
                    text = "Click a shortcut, then press the keys you want. Esc cancels.",
                    style = VerseTheme.typography.uiSmall,
                    color = VerseTheme.colors.textMuted,
                )
                Commands.all.groupBy { it.category }.forEach { (category, categoryCommands) ->
                    VerseText(
                        text = category.label,
                        modifier = Modifier.padding(top = VerseTheme.spacing.xs),
                        style = VerseTheme.typography.uiLabel,
                        color = VerseTheme.colors.textMuted,
                    )
                    categoryCommands.forEach { command ->
                        KeybindingRow(
                            title = command.title,
                            binding = effectiveBinding(command),
                            isRecording = recording == command.id,
                            isOverridden = overrides.containsKey(command.id.value),
                            onStartRecording = { recording = command.id },
                            onReset = {
                                onSettingsChange { current ->
                                    current.copy(
                                        keyBindingOverrides =
                                            current.keyBindingOverrides - command.id.value,
                                    )
                                }
                            },
                        )
                    }
                }
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
private fun FontSizeRow(value: Float, onAdjust: (Float) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VerseTheme.spacing.sm),
    ) {
        VerseText(
            text = "Editor text size",
            modifier = Modifier.weight(1f),
            style = VerseTheme.typography.uiBody,
            color = VerseTheme.colors.textSecondary,
        )
        VerseStepper(
            value = formatEditorFontSize(value),
            onDecrease = { onAdjust(-EditorFontSizeStep) },
            onIncrease = { onAdjust(EditorFontSizeStep) },
            decreaseEnabled = value > MinEditorFontSize,
            increaseEnabled = value < MaxEditorFontSize,
        )
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

@Composable
private fun KeybindingRow(
    title: String,
    binding: KeyBinding?,
    isRecording: Boolean,
    isOverridden: Boolean,
    onStartRecording: () -> Unit,
    onReset: () -> Unit,
) {
    val colors = VerseTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VerseTheme.spacing.sm),
    ) {
        VerseText(
            text = title,
            modifier = Modifier.weight(1f),
            style = VerseTheme.typography.uiBody,
            color = colors.textSecondary,
            maxLines = 1,
        )
        if (isOverridden) {
            VerseText(
                text = "Reset",
                modifier = Modifier
                    .clickable(onClick = onReset)
                    .padding(horizontal = VerseTheme.spacing.sm, vertical = VerseTheme.spacing.xs),
                style = VerseTheme.typography.uiSmall,
                color = colors.textMuted,
                maxLines = 1,
            )
        }
        VerseText(
            text = when {
                isRecording -> "Press keys..."
                binding == null -> "Unassigned"
                else -> binding.label
            },
            modifier = Modifier
                .background(
                    if (isRecording) colors.surfaceSelected else colors.surfaceHover,
                    VerseTheme.shapes.small,
                )
                .clickable(onClick = onStartRecording)
                .padding(horizontal = VerseTheme.spacing.sm, vertical = VerseTheme.spacing.xs),
            style = VerseTheme.typography.uiSmall,
            color = if (isRecording) colors.accent else colors.textPrimary,
            maxLines = 1,
        )
    }
}
