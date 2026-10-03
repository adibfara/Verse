package com.github.adibfara.mds.feature.editor

import androidx.compose.ui.text.input.TextFieldValue
import com.github.adibfara.mds.core.markdown.FormatAction
import com.github.adibfara.mds.core.markdown.MdDocument
import com.github.adibfara.mds.core.markdown.MdRange
import com.github.adibfara.mds.core.model.EditorMode
import com.github.adibfara.mds.core.model.FileKind
import com.github.adibfara.mds.core.model.FilePath

data class EditorUiState(
    val path: FilePath? = null,
    val breadcrumb: List<String> = emptyList(),
    val kind: FileKind = FileKind.Markdown,
    val mode: EditorMode = EditorMode.Live,
    val value: TextFieldValue = TextFieldValue(),
    val document: MdDocument = MdDocument.Empty,
    val absolutePath: String = "",
    val isLoading: Boolean = false,
    val isDirty: Boolean = false,
    val isFavorite: Boolean = false,
    val errorMessage: String? = null,
    val focusContentRequestId: Int? = null,
) {
    val hasFile: Boolean get() = path != null
}

sealed interface EditorEvent {
    data class Open(val path: FilePath, val kind: FileKind) : EditorEvent
    data class RequestFocusContent(val path: FilePath) : EditorEvent
    data class Close(val path: FilePath) : EditorEvent
    data class TextChanged(val value: TextFieldValue) : EditorEvent
    data class SetMode(val mode: EditorMode) : EditorEvent
    data object ToggleLivePreview : EditorEvent
    data object ToggleRawMode : EditorEvent
    data object Save : EditorEvent
    data class ToggleTask(val range: MdRange, val checked: Boolean) : EditorEvent
    data class ApplyFormat(val action: FormatAction) : EditorEvent
    data class Renamed(val from: FilePath, val to: FilePath) : EditorEvent
    data object DismissError : EditorEvent
    data object ClearActive : EditorEvent
    data object ToggleFavorite : EditorEvent
    data class RequestRename(val name: String) : EditorEvent
}

sealed interface EditorAction {
    data class Renamed(val from: FilePath, val to: FilePath) : EditorAction
}
