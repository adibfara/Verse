package com.github.adibfara.mds.feature.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.adibfara.mds.core.common.Debouncer
import com.github.adibfara.mds.core.common.DispatcherProvider
import com.github.adibfara.mds.core.markdown.FormatAction
import com.github.adibfara.mds.core.markdown.MarkdownParser
import com.github.adibfara.mds.core.markdown.MdDocument
import com.github.adibfara.mds.core.markdown.MdRange
import com.github.adibfara.mds.core.markdown.anchorOffsetIn
import com.github.adibfara.mds.core.markdown.applyFormatAction
import com.github.adibfara.mds.core.markdown.computeStats
import com.github.adibfara.mds.core.markdown.documentWithTaskToggled
import com.github.adibfara.mds.core.markdown.hasListContinuation
import com.github.adibfara.mds.core.model.EditorMode
import com.github.adibfara.mds.core.model.FileKind
import com.github.adibfara.mds.core.model.FilePath
import com.github.adibfara.mds.core.persistence.SettingsStore
import com.github.adibfara.mds.feature.explorer.usecase.NameValidation
import com.github.adibfara.mds.feature.explorer.usecase.validateName
import com.github.adibfara.mds.feature.space.SpaceRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val ParseDebounceMillis = 80L

private class EditorSession(
    val path: FilePath,
    val kind: FileKind,
    var value: TextFieldValue,
    var mode: EditorMode,
    var document: MdDocument,
    var savedText: String,
    var isLoaded: Boolean,
    var isFavorite: Boolean = false,
) {
    val isDirty: Boolean get() = value.text != savedText

    val breadcrumb: List<String> = breadcrumbOf(path)

    var absolutePath: String = ""
}

private fun breadcrumbOf(path: FilePath): List<String> {
    val segments = path.segments
    if (segments.isEmpty()) return emptyList()
    return segments.dropLast(1) + path.nameWithoutExtension
}

data class DirtyChange(val path: FilePath, val isDirty: Boolean)

class EditorViewModel(
    private val spaceRepository: SpaceRepository,
    private val settingsStore: SettingsStore,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    private val parser = MarkdownParser()
    private val sessions = mutableMapOf<FilePath, EditorSession>()
    private var activePath: FilePath? = null
    private var pendingFocusPath: FilePath? = null
    private var focusRequestCounter = 0

    private val mutableState = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = mutableState.asStateFlow()

    private val mutableDirtyChanges = MutableSharedFlow<DirtyChange>(extraBufferCapacity = 32)
    val dirtyChanges: SharedFlow<DirtyChange> = mutableDirtyChanges.asSharedFlow()

    private val mutableActions = MutableSharedFlow<EditorAction>(extraBufferCapacity = 8)
    val actions: SharedFlow<EditorAction> = mutableActions.asSharedFlow()

    private val parseDebouncer = Debouncer(viewModelScope, ParseDebounceMillis)
    private val saveDebouncer = Debouncer(
        viewModelScope,
        settingsStore.settings.value.autoSaveDelayMillis,
    )

    fun onEvent(event: EditorEvent) {
        when (event) {
            is EditorEvent.Open -> open(event.path, event.kind)
            is EditorEvent.RequestFocusContent -> requestFocusContent(event.path)
            is EditorEvent.Close -> closeSession(event.path)
            is EditorEvent.TextChanged -> textChanged(event.value)
            is EditorEvent.SetMode -> setMode(event.mode)
            EditorEvent.ToggleLivePreview -> toggleLivePreview()
            EditorEvent.ToggleRawMode -> toggleRawMode()
            EditorEvent.Save -> viewModelScope.launch { flushAll() }
            is EditorEvent.ToggleTask -> toggleTask(event.range, event.checked)
            is EditorEvent.JumpToAnchor -> jumpToAnchor(event.target)
            is EditorEvent.ApplyFormat -> applyFormat(event.action)
            is EditorEvent.Renamed -> renamed(event.from, event.to)
            is EditorEvent.ReportError -> mutableState.value =
                mutableState.value.copy(errorMessage = event.message)

            EditorEvent.DismissError -> mutableState.value =
                mutableState.value.copy(errorMessage = null)

            EditorEvent.ClearActive -> clearActive()
            EditorEvent.ToggleFavorite -> toggleFavorite()
            is EditorEvent.RequestRename -> renameActiveFile(event.name)
        }
    }

    private fun renameActiveFile(newName: String) {
        val session = activeSession() ?: return
        val validation = validateName(newName)
        if (validation is NameValidation.Invalid) {
            mutableState.value = mutableState.value.copy(errorMessage = validation.reason)
            return
        }
        val trimmed = newName.trim()
        val extension = session.path.extension
        val finalName = if (trimmed.contains('.') || extension.isEmpty()) trimmed else "$trimmed.$extension"
        if (finalName == session.path.name) return

        viewModelScope.launch {
            val fileSystem = spaceRepository.current.value?.fileSystem ?: return@launch
            val from = session.path
            val renamed = runCatching { fileSystem.rename(from, finalName) }.getOrElse { failure ->
                mutableState.value = mutableState.value.copy(
                    errorMessage = failure.message ?: "Could not rename this file",
                )
                return@launch
            }
            sessions.remove(from)
            val updated = EditorSession(
                path = renamed,
                kind = FileKind.fromExtension(renamed.extension),
                value = session.value,
                mode = session.mode,
                document = session.document,
                savedText = session.savedText,
                isLoaded = session.isLoaded,
                isFavorite = session.isFavorite,
            )
            sessions[renamed] = updated
            if (activePath == from) {
                activePath = renamed
                publish(updated)
            }
            mutableActions.tryEmit(EditorAction.Renamed(from, renamed))
        }
    }

    private fun toggleFavorite() {
        val session = activeSession() ?: return
        val favorites = spaceRepository.current.value?.favorites ?: return
        viewModelScope.launch {
            val isFavorite = favorites.toggleFavorite(session.path)
            session.isFavorite = isFavorite
            if (activePath == session.path) publish(session)
        }
    }

    fun closeSpace() {
        viewModelScope.launch { flushAll() }
        sessions.clear()
        activePath = null
        mutableState.value = EditorUiState()
    }

    private fun clearActive() {
        activePath = null
        mutableState.value = EditorUiState()
    }

    private fun requestFocusContent(path: FilePath) {
        pendingFocusPath = path
        if (activePath == path) consumePendingFocus(path)
    }

    private fun consumePendingFocus(path: FilePath) {
        if (pendingFocusPath != path) return
        pendingFocusPath = null
        focusRequestCounter++
        mutableState.value = mutableState.value.copy(focusContentRequestId = focusRequestCounter)
    }

    private fun open(path: FilePath, kind: FileKind) {
        if (activePath == path && sessions[path]?.isLoaded == true) return
        activePath = path
        val existing = sessions[path]
        if (existing != null && existing.isLoaded) {
            publish(existing)
            consumePendingFocus(path)
            return
        }
        val session = existing ?: EditorSession(
            path = path,
            kind = kind,
            value = TextFieldValue(""),
            mode = defaultModeFor(kind),
            document = MdDocument.Empty,
            savedText = "",
            isLoaded = false,
        ).also { sessions[path] = it }

        mutableState.value = stateOf(session).copy(isLoading = true)
        viewModelScope.launch {
            val fileSystem = spaceRepository.current.value?.fileSystem ?: return@launch
            val text = runCatching { fileSystem.readText(path) }.getOrElse { failure ->
                mutableState.value = mutableState.value.copy(
                    isLoading = false,
                    errorMessage = failure.message ?: "Could not open this file",
                )
                return@launch
            }
            session.value = TextFieldValue(text, TextRange(0))
            session.savedText = text
            session.isLoaded = true
            session.document = if (session.kind == FileKind.Markdown) {
                withContext(dispatchers.default) { parser.parse(text) }
            } else {
                MdDocument.Empty.copy(text = text, stats = computeStats(text))
            }
            session.isFavorite = spaceRepository.current.value?.favorites?.isFavorite(path) ?: false
            if (activePath == path) {
                publish(session)
                consumePendingFocus(path)
            }
        }
    }

    private fun defaultModeFor(kind: FileKind): EditorMode = when (kind) {
        FileKind.Markdown -> settingsStore.settings.value.defaultEditorMode
        else -> EditorMode.Raw
    }

    private fun closeSession(path: FilePath) {
        viewModelScope.launch {
            sessions[path]?.let { save(it) }
            sessions.remove(path)
            if (activePath == path) clearActive()
        }
    }

    private fun textChanged(value: TextFieldValue, reparse: Boolean = true) {
        val session = activeSession() ?: return
        if (reparse && continuedList(session, value)) return
        val textChanged = session.value.text != value.text
        val wasDirty = session.isDirty
        session.value = value
        publish(session)

        if (!textChanged) return

        if (session.isDirty != wasDirty) {
            mutableDirtyChanges.tryEmit(DirtyChange(session.path, session.isDirty))
        }

        if (session.kind == FileKind.Markdown) {
            if (!reparse) {
                saveDebouncer.submit { flushAll() }
                return
            }
            parseDebouncer.submit {
                val snapshot = session.value.text
                val parsed = withContext(dispatchers.default) { parser.parse(snapshot) }
                if (session.value.text == snapshot) {
                    session.document = parsed
                    if (activePath == session.path) publish(session)
                }
            }
        } else {
            session.document = MdDocument.Empty.copy(
                text = value.text,
                stats = computeStats(value.text),
            )
            publish(session)
        }

        saveDebouncer.submit { flushAll() }
    }

    private fun setMode(mode: EditorMode) {
        val session = activeSession() ?: return
        session.mode = mode
        publish(session)
    }

    private fun toggleLivePreview() {
        val session = activeSession() ?: return
        session.mode = if (session.mode == EditorMode.Preview) EditorMode.Live else EditorMode.Preview
        publish(session)
    }

    private fun toggleRawMode() {
        val session = activeSession() ?: return
        session.mode = if (session.mode == EditorMode.Raw) EditorMode.Live else EditorMode.Raw
        publish(session)
    }

    private fun toggleTask(range: MdRange, checked: Boolean) {
        val session = activeSession() ?: return
        val text = session.value.text
        if (range.start < 0 || range.end > text.length) return
        val replacement = if (checked) "[x]" else "[ ]"
        if (text.substring(range.start, range.end) == replacement) return
        val updated = text.replaceRange(range.start, range.end, replacement)
        session.document = documentWithTaskToggled(session.document, updated, range, checked)
        textChanged(session.value.copy(text = updated))
    }

    private fun jumpToAnchor(target: String) {
        val session = activeSession() ?: return
        val document = if (session.document.text == session.value.text) {
            session.document
        } else {
            parser.parse(session.value.text).also { session.document = it }
        }
        val offset = anchorOffsetIn(document, target) ?: return
        session.value = session.value.copy(selection = TextRange(offset))
        publish(session)
    }

    private fun applyFormat(action: FormatAction) {
        val session = activeSession() ?: return
        if (session.kind != FileKind.Markdown) return
        val current = session.value
        applyFormatTo(
            session = session,
            text = current.text,
            selection = MdRange(current.selection.min, current.selection.max),
            reversed = current.selection.start > current.selection.end,
            action = action,
        )
    }

    private fun applyFormatTo(
        session: EditorSession,
        text: String,
        selection: MdRange,
        reversed: Boolean,
        action: FormatAction,
    ): Boolean {
        val edit = applyFormatAction(text = text, selection = selection, action = action)
        if (edit.text == text && edit.selection == selection) return false
        session.document = parser.parse(edit.text)
        textChanged(
            value = TextFieldValue(
                text = edit.text,
                selection = if (reversed) {
                    TextRange(edit.selection.end, edit.selection.start)
                } else {
                    TextRange(edit.selection.start, edit.selection.end)
                },
            ),
            reparse = false,
        )
        return true
    }

    private fun continuedList(session: EditorSession, value: TextFieldValue): Boolean {
        if (session.kind != FileKind.Markdown) return false
        val caret = insertedNewlineOffset(session.value, value) ?: return false
        if (!hasListContinuation(session.value.text, caret)) return false
        return applyFormatTo(
            session = session,
            text = session.value.text,
            selection = MdRange(caret, caret),
            reversed = false,
            action = FormatAction.ContinueList,
        )
    }

    private fun insertedNewlineOffset(before: TextFieldValue, after: TextFieldValue): Int? {
        if (after.text.length != before.text.length + 1) return null
        if (!after.selection.collapsed) return null
        val offset = after.selection.start - 1
        if (offset < 0 || offset > before.text.length) return null
        if (after.text[offset] != '\n') return null
        return offset
    }

    private fun renamed(from: FilePath, to: FilePath) {
        val session = sessions.remove(from) ?: return
        val moved = EditorSession(
            path = to,
            kind = FileKind.fromExtension(to.extension),
            value = session.value,
            mode = session.mode,
            document = session.document,
            savedText = session.savedText,
            isLoaded = session.isLoaded,
            isFavorite = session.isFavorite,
        )
        sessions[to] = moved
        if (activePath == from) {
            activePath = to
            publish(moved)
        }
    }

    private fun activeSession(): EditorSession? = activePath?.let { sessions[it] }

    private suspend fun save(session: EditorSession) {
        if (!session.isLoaded || !session.isDirty) return
        val fileSystem = spaceRepository.current.value?.fileSystem ?: return
        val snapshot = session.value.text
        runCatching { fileSystem.writeText(session.path, snapshot) }
            .onSuccess {
                session.savedText = snapshot
                mutableDirtyChanges.tryEmit(DirtyChange(session.path, session.isDirty))
                if (activePath == session.path) publish(session)
            }
            .onFailure { failure ->
                mutableState.value = mutableState.value.copy(
                    errorMessage = failure.message ?: "Could not save this file",
                )
            }
    }

    suspend fun flushAll() {
        sessions.values.toList().forEach { save(it) }
    }

    private fun publish(session: EditorSession) {
        mutableState.value = stateOf(session)
    }

    private fun stateOf(session: EditorSession): EditorUiState {
        if (session.absolutePath.isEmpty()) {
            val fileSystem = spaceRepository.current.value?.fileSystem
            session.absolutePath = fileSystem?.absolutePathOf(session.path).orEmpty()
        }
        return EditorUiState(
            path = session.path,
            breadcrumb = session.breadcrumb,
            kind = session.kind,
            mode = session.mode,
            value = session.value,
            document = session.document,
            absolutePath = session.absolutePath,
            isLoading = !session.isLoaded,
            isDirty = session.isDirty,
            isFavorite = session.isFavorite,
        )
    }

    override fun onCleared() {
        parseDebouncer.cancel()
        saveDebouncer.cancel()
        super.onCleared()
    }
}
