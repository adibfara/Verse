package com.github.adibfara.mds.feature.editor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.github.adibfara.mds.core.designsystem.component.MdsIconButton
import com.github.adibfara.mds.core.designsystem.component.MdsIcons
import com.github.adibfara.mds.core.designsystem.component.MdsText
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
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(MdsTheme.sizes.toolbarHeight)
            .padding(horizontal = MdsTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MdsTheme.spacing.xs),
    ) {
        Breadcrumb(
            segments = breadcrumb,
            isDirty = isDirty,
            onRenameTitle = onRenameTitle,
            modifier = Modifier.weight(1f),
        )

        MdsIconButton(
            icon = MdsIcons.Heart,
            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
            onClick = onToggleFavorite,
            selected = isFavorite,
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
        MdsIconButton(
            icon = MdsIcons.ExternalLink,
            contentDescription = "Show in system explorer",
            onClick = onRevealInFileManager,
        )
    }
}

@Composable
private fun Breadcrumb(
    segments: List<String>,
    isDirty: Boolean,
    onRenameTitle: (String) -> Unit,
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
                EditableTitle(title = segment, onRename = onRenameTitle)
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
private fun EditableTitle(title: String, onRename: (String) -> Unit) {
    val colors = MdsTheme.colors
    var editing by remember { mutableStateOf(false) }
    var text by remember(title) { mutableStateOf(title) }
    val focusRequester = remember { FocusRequester() }

    if (editing) {
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            textStyle = MdsTheme.typography.uiLabel.copy(color = colors.textPrimary),
            cursorBrush = SolidColor(colors.caret),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { editing = false }),
            modifier = Modifier
                .widthIn(min = 24.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { state ->
                    if (!state.isFocused && editing) {
                        editing = false
                        val trimmed = text.trim()
                        if (trimmed.isNotEmpty() && trimmed != title) {
                            onRename(trimmed)
                        } else {
                            text = title
                        }
                    }
                },
        )
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    } else {
        MdsText(
            text = title,
            style = MdsTheme.typography.uiLabel,
            color = colors.textPrimary,
            maxLines = 1,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                text = title
                editing = true
            },
        )
    }
}
