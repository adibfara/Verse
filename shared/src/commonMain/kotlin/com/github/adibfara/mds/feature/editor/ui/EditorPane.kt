package com.github.adibfara.mds.feature.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.github.adibfara.mds.core.markdown.FormatAction
import com.github.adibfara.mds.core.markdown.MdRange
import com.github.adibfara.mds.core.designsystem.component.MdsDivider
import com.github.adibfara.mds.core.designsystem.theme.MdsTheme
import com.github.adibfara.mds.core.model.EditorMode
import com.github.adibfara.mds.core.model.FileKind
import com.github.adibfara.mds.feature.editor.EditorEvent
import com.github.adibfara.mds.feature.editor.EditorUiState
import com.github.adibfara.mds.feature.viewer.ui.EmptyEditorPlaceholder
import com.github.adibfara.mds.feature.viewer.ui.ImageViewer
import com.github.adibfara.mds.feature.viewer.ui.UnsupportedFileCard

@Composable
fun EditorPane(
    state: EditorUiState,
    onEvent: (EditorEvent) -> Unit,
    onOpenInDefaultApplication: () -> Unit,
    onRevealInFileManager: () -> Unit,
    onOpenLink: (String) -> Unit,
    showStatusBar: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MdsTheme.colors
    Column(modifier = modifier.fillMaxSize().background(colors.surface)) {
        if (state.hasFile) {
            EditorHeader(
                breadcrumb = state.breadcrumb,
                mode = state.mode,
                isDirty = state.isDirty,
                isFavorite = state.isFavorite,
                onModeChange = { onEvent(EditorEvent.SetMode(it)) },
                onRevealInFileManager = onRevealInFileManager,
                onToggleFavorite = { onEvent(EditorEvent.ToggleFavorite) },
                onRenameTitle = { onEvent(EditorEvent.RequestRename(it)) },
            )
            MdsDivider()
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                !state.hasFile -> EmptyEditorPlaceholder()
                state.kind == FileKind.Image -> ImageViewer(target = state.absolutePath)
                state.kind == FileKind.Other -> UnsupportedFileCard(
                    fileName = state.path?.name.orEmpty(),
                    onOpenInDefaultApplication = onOpenInDefaultApplication,
                    onRevealInFileManager = onRevealInFileManager,
                )

                else -> EditorContent(state = state, onEvent = onEvent, onOpenLink = onOpenLink)
            }
        }

        if (showStatusBar && state.hasFile) {
            MdsDivider()
            EditorStatusBar(stats = state.document.stats, isDirty = state.isDirty)
        }
    }
}

@Composable
private fun EditorContent(
    state: EditorUiState,
    onEvent: (EditorEvent) -> Unit,
    onOpenLink: (String) -> Unit,
) {
    val scrollState = remember(state.path) { ScrollState(0) }
    val focusRequester = remember(state.path) { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }
    val onValueChange: (TextFieldValue) -> Unit = { onEvent(EditorEvent.TextChanged(it)) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = MdsTheme.sizes.editorMaxWidth)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .clickable(interactionSource = interactionSource, indication = null) {
                    onValueChange(state.value.copy(selection = TextRange(state.value.text.length)))
                    focusRequester.requestFocus()
                }
                .padding(horizontal = MdsTheme.spacing.xl, vertical = MdsTheme.spacing.lg),
        ) {
            val onToggleTask: (MdRange, Boolean) -> Unit = { range, checked ->
                onEvent(EditorEvent.ToggleTask(range, checked))
            }
            val onFormat: (FormatAction) -> Unit = { onEvent(EditorEvent.ApplyFormat(it)) }
            when (state.mode) {
                EditorMode.Preview -> RenderedMarkdownField(
                    value = state.value,
                    document = state.document,
                    onValueChange = onValueChange,
                    onToggleTask = onToggleTask,
                    onOpenLink = onOpenLink,
                    onFormat = onFormat,
                    focusRequester = focusRequester,
                    modifier = Modifier.fillMaxWidth(),
                )

                EditorMode.Live -> LiveMarkdownField(
                    value = state.value,
                    document = state.document,
                    onValueChange = onValueChange,
                    onToggleTask = onToggleTask,
                    onOpenLink = onOpenLink,
                    onFormat = onFormat,
                    focusRequester = focusRequester,
                    modifier = Modifier.fillMaxWidth(),
                )

                EditorMode.Raw -> RawMarkdownField(
                    value = state.value,
                    onValueChange = onValueChange,
                    focusRequester = focusRequester,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(modifier = Modifier.height(MdsTheme.sizes.editorBottomPadding))
        }
    }
}
