package com.github.adibfara.verse.feature.editor.ui

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
import androidx.compose.ui.graphics.lerp
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
import androidx.compose.ui.unit.Dp
import com.github.adibfara.verse.core.designsystem.component.VerseFloatingBar
import com.github.adibfara.verse.core.designsystem.component.VerseIconButton
import com.github.adibfara.verse.core.designsystem.component.VerseIcons
import com.github.adibfara.verse.core.designsystem.component.VerseText
import com.github.adibfara.verse.core.designsystem.component.MenuEntry
import com.github.adibfara.verse.core.designsystem.theme.VerseTheme
import com.github.adibfara.verse.core.model.EditorMode

@Composable
fun EditorHeader(
    breadcrumb: List<String>,
    mode: EditorMode,
    isFavorite: Boolean,
    isLocked: Boolean = false,
    onModeChange: (EditorMode) -> Unit,
    onRevealInFileManager: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleLock: () -> Unit = {},
    canUndo: Boolean = false,
    canRedo: Boolean = false,
    onUndo: () -> Unit = {},
    onRedo: () -> Unit = {},
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
    fontSize: Float? = null,
    onAdjustFontSize: ((Float) -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(if (compactActions) VerseTheme.sizes.mobileTopBarHeight else VerseTheme.sizes.toolbarHeight)
            .padding(horizontal = VerseTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VerseTheme.spacing.xs),
    ) {
        leading?.invoke()

        val pathSegments = if (showBreadcrumbPath) breadcrumb.dropLast(1) else emptyList()
        if (centerBreadcrumb) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Breadcrumb(
                    segments = pathSegments,
                )
            }
        } else {
            Breadcrumb(
                segments = pathSegments,
                modifier = Modifier.weight(1f),
            )
        }

        if (compactActions) {
            CompactHeaderActions(
                mode = mode,
                isFavorite = isFavorite,
                isLocked = isLocked,
                onModeChange = onModeChange,
                onToggleFavorite = onToggleFavorite,
                onToggleLock = onToggleLock,
                canUndo = canUndo,
                canRedo = canRedo,
                onUndo = onUndo,
                onRedo = onRedo,
                onOpenSettings = onOpenSettings,
                onVoiceInput = onVoiceInput,
                onOverflowAction = onOverflowAction,
                fontSize = fontSize,
                onAdjustFontSize = onAdjustFontSize,
            )
        } else {
            onVoiceInput?.let {
                VerseIconButton(
                    icon = VerseIcons.Mic,
                    contentDescription = "Dictate text",
                    onClick = it,
                )
            }
            VerseIconButton(
                icon = VerseIcons.LockOpen,
                contentDescription = if (isLocked) "Unlock document" else "Lock document (read only)",
                onClick = onToggleLock,
                selected = isLocked,
                selectedIcon = VerseIcons.Lock,
            )
            VerseIconButton(
                icon = VerseIcons.Heart,
                contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                onClick = onToggleFavorite,
                selected = isFavorite,
                selectedIcon = VerseIcons.HeartFilled,
            )
            VerseIconButton(
                icon = VerseIcons.Eye,
                contentDescription = "Rendered editing mode",
                onClick = { onModeChange(EditorMode.Preview) },
                selected = mode == EditorMode.Preview,
            )
            VerseIconButton(
                icon = VerseIcons.Pencil,
                contentDescription = "Live editing mode",
                onClick = { onModeChange(EditorMode.Live) },
                selected = mode == EditorMode.Live,
            )
            VerseIconButton(
                icon = VerseIcons.Code,
                contentDescription = "Raw markdown mode",
                onClick = { onModeChange(EditorMode.Raw) },
                selected = mode == EditorMode.Raw,
            )
            if (showRevealAction) {
                VerseIconButton(
                    icon = VerseIcons.ExternalLink,
                    contentDescription = "Show in system explorer",
                    onClick = onRevealInFileManager,
                )
            }
            onToggleTheme?.let {
                VerseIconButton(
                    icon = if (isDarkTheme) VerseIcons.Sun else VerseIcons.Moon,
                    contentDescription = "Toggle theme",
                    onClick = it,
                )
            }
            EditorOverflowButton(
                onAction = onOverflowAction,
                fontSize = fontSize,
                onAdjustFontSize = onAdjustFontSize,
            )
        }
    }
}

@Composable
private fun CompactHeaderActions(
    mode: EditorMode,
    isFavorite: Boolean,
    isLocked: Boolean,
    onModeChange: (EditorMode) -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleLock: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onOpenSettings: (() -> Unit)?,
    onVoiceInput: (() -> Unit)?,
    onOverflowAction: (EditorOverflowAction) -> Unit,
    fontSize: Float?,
    onAdjustFontSize: ((Float) -> Unit)?,
) {
    val isRaw = mode == EditorMode.Raw
    val extraEntries = buildList {
        onOpenSettings?.let {
            add(MenuEntry.Action(label = "Options...", icon = VerseIcons.Sliders, onClick = it))
        }
        add(
            MenuEntry.Action(
                label = if (isRaw) "Rendered editing mode" else "Raw markdown mode",
                icon = if (isRaw) VerseIcons.Eye else VerseIcons.Code,
                onClick = { onModeChange(if (isRaw) EditorMode.Preview else EditorMode.Raw) },
            ),
        )
        add(
            MenuEntry.Action(
                label = if (mode == EditorMode.Live) "Hide markup on every line" else "Show markup on caret line",
                icon = VerseIcons.Pencil,
                onClick = {
                    onModeChange(if (mode == EditorMode.Live) EditorMode.Preview else EditorMode.Live)
                },
            ),
        )
        onVoiceInput?.let {
            add(MenuEntry.Action(label = "Dictate text", icon = VerseIcons.Mic, onClick = it))
        }
    }
    VerseFloatingBar {
        VerseIconButton(
            icon = VerseIcons.LockOpen,
            contentDescription = if (isLocked) "Unlock document" else "Lock document (read only)",
            onClick = onToggleLock,
            selected = isLocked,
            selectedIcon = VerseIcons.Lock,
        )
        VerseIconButton(
            icon = VerseIcons.Heart,
            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
            onClick = onToggleFavorite,
            selected = isFavorite,
            selectedIcon = VerseIcons.HeartFilled,
        )
        VerseIconButton(
            icon = VerseIcons.Undo,
            contentDescription = "Undo",
            onClick = onUndo,
            enabled = canUndo,
        )
        VerseIconButton(
            icon = VerseIcons.Redo,
            contentDescription = "Redo",
            onClick = onRedo,
            enabled = canRedo,
        )
        EditorOverflowButton(
            onAction = onOverflowAction,
            leadingEntries = extraEntries,
            fontSize = fontSize,
            onAdjustFontSize = onAdjustFontSize,
        )
    }
}

@Composable
private fun Breadcrumb(
    segments: List<String>,
    modifier: Modifier = Modifier,
) {
    val colors = VerseTheme.colors
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VerseTheme.spacing.xs),
    ) {
        segments.forEachIndexed { index, segment ->
            VerseText(
                text = segment,
                style = VerseTheme.typography.uiLabel,
                color = colors.textMuted,
                maxLines = 1,
            )
            if (index != segments.lastIndex) {
                VerseText(
                    text = "/",
                    style = VerseTheme.typography.uiLabel,
                    color = colors.textMuted,
                )
            }
        }
    }
}

@Composable
fun EditorTitleBar(
    title: String,
    isDirty: Boolean,
    onRename: (String) -> Unit,
    horizontalPadding: Dp,
    modifier: Modifier = Modifier,
    onStartNoteBody: (() -> Unit)? = null,
    focusRequestId: Int? = null,
    highlightOf: () -> Float = { 0f },
) {
    val colors = VerseTheme.colors
    val titleFocusRequester = remember { FocusRequester() }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = VerseTheme.spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = VerseTheme.sizes.editorMaxWidth)
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(VerseTheme.spacing.xs),
        ) {
            EditableTitle(
                title = title,
                onRename = onRename,
                onStartNoteBody = onStartNoteBody,
                focusRequester = titleFocusRequester,
                focusRequestId = focusRequestId,
                highlightOf = highlightOf,
                style = VerseTheme.typography.noteTitle,
                modifier = Modifier.weight(1f),
            )
            if (isDirty) {
                VerseText(
                    text = "•",
                    style = VerseTheme.typography.noteTitle,
                    color = colors.accent,
                )
            }
        }
    }
}

@Composable
private fun EditableTitle(
    title: String,
    onRename: (String) -> Unit,
    style: TextStyle,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    onStartNoteBody: (() -> Unit)? = null,
    focusRequestId: Int? = null,
    highlightOf: () -> Float = { 0f },
) {
    val colors = VerseTheme.colors
    val focusManager = LocalFocusManager.current
    var value by remember(title) { mutableStateOf(TextFieldValue(title)) }
    var focused by remember { mutableStateOf(false) }
    val startBody: () -> Unit = {
        val start = onStartNoteBody
        if (start != null) start() else focusManager.clearFocus()
    }

    LaunchedEffect(focusRequestId) {
        if (focusRequestId == null) return@LaunchedEffect
        value = value.copy(selection = TextRange(value.text.length))
        focusRequester.requestFocus()
    }

    BasicTextField(
        value = value,
        onValueChange = { updated ->
            if (updated.text.contains('\n')) {
                startBody()
            } else {
                value = updated
            }
        },
        singleLine = false,
        textStyle = style.copy(color = lerp(colors.textPrimary, colors.accent, highlightOf())),
        cursorBrush = SolidColor(colors.caret),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { startBody() }, onDone = { startBody() }),
        modifier = modifier
            .widthIn(min = VerseTheme.sizes.badgeMinWidth)
            .padding(vertical = VerseTheme.spacing.sm)
            .focusRequester(focusRequester)
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
