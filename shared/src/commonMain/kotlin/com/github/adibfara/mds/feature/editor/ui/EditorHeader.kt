package com.github.adibfara.mds.feature.editor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import com.github.adibfara.mds.core.designsystem.component.MdsFloatingBar
import com.github.adibfara.mds.core.designsystem.component.MdsIconButton
import com.github.adibfara.mds.core.designsystem.component.MdsIcons
import com.github.adibfara.mds.core.designsystem.component.MdsText
import com.github.adibfara.mds.core.designsystem.component.MenuEntry
import com.github.adibfara.mds.core.designsystem.theme.MdsTheme
import com.github.adibfara.mds.core.model.EditorMode

@Composable
fun EditorHeader(
    breadcrumb: List<String>,
    mode: EditorMode,
    isDirty: Boolean,
    isFavorite: Boolean,
    onModeChange: (EditorMode) -> Unit,
    onRevealInFileManager: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRenameTitle: (String) -> Unit,
    onOverflowAction: (EditorOverflowAction) -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    isDarkTheme: Boolean = false,
    onToggleTheme: (() -> Unit)? = null,
    showBreadcrumbPath: Boolean = true,
    showRevealAction: Boolean = true,
    onVoiceInput: (() -> Unit)? = null,
    centerBreadcrumb: Boolean = false,
    compactActions: Boolean = false,
    onOpenSettings: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(if (compactActions) MdsTheme.sizes.mobileTopBarHeight else MdsTheme.sizes.toolbarHeight)
            .padding(horizontal = MdsTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MdsTheme.spacing.xs),
    ) {
        leading?.invoke()

        val titleStyle = if (compactActions) MdsTheme.typography.uiTitle else MdsTheme.typography.uiLabel
        if (centerBreadcrumb) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Breadcrumb(
                    segments = if (showBreadcrumbPath) breadcrumb else breadcrumb.takeLast(1),
                    isDirty = isDirty,
                    onRenameTitle = onRenameTitle,
                    titleStyle = titleStyle,
                )
            }
        } else {
            Breadcrumb(
                segments = if (showBreadcrumbPath) breadcrumb else breadcrumb.takeLast(1),
                isDirty = isDirty,
                onRenameTitle = onRenameTitle,
                titleStyle = titleStyle,
                modifier = Modifier.weight(1f),
            )
        }

        if (compactActions) {
            CompactHeaderActions(
                mode = mode,
                isFavorite = isFavorite,
                onModeChange = onModeChange,
                onToggleFavorite = onToggleFavorite,
                onOpenSettings = onOpenSettings,
                onVoiceInput = onVoiceInput,
                onOverflowAction = onOverflowAction,
            )
        } else {
            onVoiceInput?.let {
                MdsIconButton(
                    icon = MdsIcons.Mic,
                    contentDescription = "Dictate text",
                    onClick = it,
                )
            }
            MdsIconButton(
                icon = MdsIcons.Heart,
                contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                onClick = onToggleFavorite,
                selected = isFavorite,
                selectedIcon = MdsIcons.HeartFilled,
            )
            MdsIconButton(
                icon = MdsIcons.Eye,
                contentDescription = "Rendered editing mode",
                onClick = { onModeChange(EditorMode.Preview) },
                selected = mode == EditorMode.Preview,
            )
            MdsIconButton(
                icon = MdsIcons.Pencil,
                contentDescription = "Live editing mode",
                onClick = { onModeChange(EditorMode.Live) },
                selected = mode == EditorMode.Live,
            )
            MdsIconButton(
                icon = MdsIcons.Code,
                contentDescription = "Raw markdown mode",
                onClick = { onModeChange(EditorMode.Raw) },
                selected = mode == EditorMode.Raw,
            )
            if (showRevealAction) {
                MdsIconButton(
                    icon = MdsIcons.ExternalLink,
                    contentDescription = "Show in system explorer",
                    onClick = onRevealInFileManager,
                )
            }
            onToggleTheme?.let {
                MdsIconButton(
                    icon = if (isDarkTheme) MdsIcons.Sun else MdsIcons.Moon,
                    contentDescription = "Toggle theme",
                    onClick = it,
                )
            }
            EditorOverflowButton(onAction = onOverflowAction)
        }
    }
}

@Composable
private fun CompactHeaderActions(
    mode: EditorMode,
    isFavorite: Boolean,
    onModeChange: (EditorMode) -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenSettings: (() -> Unit)?,
    onVoiceInput: (() -> Unit)?,
    onOverflowAction: (EditorOverflowAction) -> Unit,
) {
    val isRaw = mode == EditorMode.Raw
    val extraEntries = buildList {
        onOpenSettings?.let {
            add(MenuEntry.Action(label = "Options...", icon = MdsIcons.Sliders, onClick = it))
        }
        add(
            MenuEntry.Action(
                label = if (mode == EditorMode.Live) "Hide markup on every line" else "Show markup on caret line",
                icon = MdsIcons.Pencil,
                onClick = {
                    onModeChange(if (mode == EditorMode.Live) EditorMode.Preview else EditorMode.Live)
                },
            ),
        )
        onVoiceInput?.let {
            add(MenuEntry.Action(label = "Dictate text", icon = MdsIcons.Mic, onClick = it))
        }
    }
    MdsFloatingBar {
        MdsIconButton(
            icon = MdsIcons.Heart,
            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
            onClick = onToggleFavorite,
            selected = isFavorite,
            selectedIcon = MdsIcons.HeartFilled,
        )
        MdsIconButton(
            icon = if (isRaw) MdsIcons.Eye else MdsIcons.Code,
            contentDescription = if (isRaw) "Rendered editing mode" else "Raw markdown mode",
            onClick = { onModeChange(if (isRaw) EditorMode.Preview else EditorMode.Raw) },
            selected = isRaw,
        )
        EditorOverflowButton(
            onAction = onOverflowAction,
            leadingEntries = extraEntries,
            alignment = Alignment.TopEnd,
        )
    }
}

@Composable
private fun Breadcrumb(
    segments: List<String>,
    isDirty: Boolean,
    onRenameTitle: (String) -> Unit,
    titleStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    val colors = MdsTheme.colors
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MdsTheme.spacing.xs),
    ) {
        segments.forEachIndexed { index, segment ->
            val isLast = index == segments.lastIndex
            if (isLast) {
                EditableTitle(
                    title = segment,
                    onRename = onRenameTitle,
                    style = titleStyle,
                    modifier = Modifier.weight(1f),
                )
            } else {
                MdsText(
                    text = segment,
                    style = MdsTheme.typography.uiLabel,
                    color = colors.textMuted,
                    maxLines = 1,
                )
                MdsText(
                    text = "/",
                    style = MdsTheme.typography.uiLabel,
                    color = colors.textMuted,
                )
            }
        }
        if (isDirty) {
            MdsText(
                text = "•",
                style = MdsTheme.typography.uiLabel,
                color = colors.accent,
            )
        }
    }
}

@Composable
private fun EditableTitle(
    title: String,
    onRename: (String) -> Unit,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    val colors = MdsTheme.colors
    val focusManager = LocalFocusManager.current
    var value by remember(title) { mutableStateOf(TextFieldValue(title)) }
    var focused by remember { mutableStateOf(false) }

    BasicTextField(
        value = value,
        onValueChange = { value = it },
        singleLine = true,
        textStyle = style.copy(color = colors.textPrimary),
        cursorBrush = SolidColor(colors.caret),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        modifier = modifier
            .widthIn(min = MdsTheme.sizes.badgeMinWidth)
            .padding(vertical = MdsTheme.spacing.sm)
            .onFocusChanged { state ->
                if (focused && !state.isFocused) {
                    val trimmed = value.text.trim()
                    if (trimmed.isNotEmpty() && trimmed != title) {
                        onRename(trimmed)
                    } else {
                        value = TextFieldValue(title)
                    }
                }
                focused = state.isFocused
            },
    )
}
