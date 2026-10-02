package com.github.adibfara.mds.feature.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                onModeChange = { onEvent(EditorEvent.SetMode(it)) },
                onRevealInFileManager = onRevealInFileManager,
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

                else -> EditorContent(state = state, onEvent = onEvent)
            }
        }

        if (showStatusBar && state.hasFile) {
            MdsDivider()
            EditorStatusBar(stats = state.document.stats, isDirty = state.isDirty)
        }
    }
}

@Composable
private fun EditorContent(state: EditorUiState, onEvent: (EditorEvent) -> Unit) {
    val listState = rememberLazyListState()
    val scrollState = rememberScrollState()
    val columnModifier = Modifier
        .widthIn(max = MdsTheme.sizes.editorMaxWidth)
        .fillMaxSize()

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        when (state.mode) {
            EditorMode.Preview -> MarkdownPreview(
                document = state.document,
                listState = listState,
                onToggleTask = { range, checked ->
                    onEvent(EditorEvent.ToggleTask(range, checked))
                },
                onOpenImage = {},
                modifier = columnModifier.padding(
                    horizontal = MdsTheme.spacing.xl,
                    vertical = MdsTheme.spacing.lg,
                ),
            )

            EditorMode.Raw -> Column(
                modifier = columnModifier
                    .verticalScroll(scrollState)
                    .padding(horizontal = MdsTheme.spacing.xl, vertical = MdsTheme.spacing.lg),
            ) {
                RawMarkdownField(
                    value = state.value,
                    onValueChange = { onEvent(EditorEvent.TextChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            EditorMode.Live -> Column(
                modifier = columnModifier
                    .verticalScroll(scrollState)
                    .padding(horizontal = MdsTheme.spacing.xl, vertical = MdsTheme.spacing.lg),
            ) {
                LiveMarkdownField(
                    value = state.value,
                    document = state.document,
                    onValueChange = { onEvent(EditorEvent.TextChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
