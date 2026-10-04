package com.github.adibfara.mds.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.adibfara.mds.core.command.Command
import com.github.adibfara.mds.core.command.CommandId
import com.github.adibfara.mds.core.command.CommandRegistry
import com.github.adibfara.mds.core.command.fuzzyScore
import com.github.adibfara.mds.core.common.DispatcherProvider
import com.github.adibfara.mds.core.model.FileNode
import com.github.adibfara.mds.core.model.FilePath
import com.github.adibfara.mds.feature.explorer.usecase.CreateNote
import com.github.adibfara.mds.feature.space.SpaceRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SearchMode {
    QuickOpen,
    ContentSearch,
    CommandPalette;

    val title: String
        get() = when (this) {
            QuickOpen -> "Open a file by name"
            ContentSearch -> "Search in files"
            CommandPalette -> "Run a command"
        }

    val placeholder: String
        get() = when (this) {
            QuickOpen -> "Type part of a file name"
            ContentSearch -> "Type text to find"
            CommandPalette -> "Type a command name"
        }
}

sealed interface SearchResult {
    data class File(val node: FileNode) : SearchResult
    data class Match(val path: FilePath, val lineNumber: Int, val line: String) : SearchResult
    data class Action(val command: Command) : SearchResult
    data class CreateNote(val name: String) : SearchResult
}

data class SearchUiState(
    val mode: SearchMode? = null,
    val query: String = "",
    val results: List<SearchResult> = emptyList(),
    val selectedIndex: Int = 0,
    val isSearching: Boolean = false,
) {
    val isOpen: Boolean get() = mode != null

    val selected: SearchResult? get() = results.getOrNull(selectedIndex)
}

sealed interface SearchAction {
    data class OpenFile(val path: FilePath) : SearchAction
    data class RunCommand(val id: CommandId) : SearchAction
}

private const val MaxContentMatches = 200
private const val SearchDebounceMillis = 120L

class SearchViewModel(
    private val spaceRepository: SpaceRepository,
    private val registry: CommandRegistry,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    private val mutableState = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = mutableState.asStateFlow()

    private val mutableActions = MutableSharedFlow<SearchAction>(extraBufferCapacity = 8)
    val actions: SharedFlow<SearchAction> = mutableActions.asSharedFlow()

    private var searchJob: Job? = null
    private var cachedFiles: List<FileNode>? = null

    fun open(mode: SearchMode) {
        mutableState.value = SearchUiState(mode = mode)
        runSearch("")
    }

    fun close() {
        searchJob?.cancel()
        mutableState.value = SearchUiState()
    }

    fun invalidateFileCache() {
        cachedFiles = null
    }

    fun queryChanged(query: String) {
        mutableState.value = mutableState.value.copy(query = query, selectedIndex = 0)
        runSearch(query)
    }

    fun moveSelection(delta: Int) {
        val current = mutableState.value
        if (current.results.isEmpty()) return
        val next = (current.selectedIndex + delta).coerceIn(0, current.results.lastIndex)
        mutableState.value = current.copy(selectedIndex = next)
    }

    fun activateSelected() {
        val selection = mutableState.value.selected ?: return
        activate(selection)
    }

    fun activate(result: SearchResult) {
        when (result) {
            is SearchResult.File -> mutableActions.tryEmit(SearchAction.OpenFile(result.node.path))
            is SearchResult.Match -> mutableActions.tryEmit(SearchAction.OpenFile(result.path))
            is SearchResult.Action -> mutableActions.tryEmit(SearchAction.RunCommand(result.command.id))
            is SearchResult.CreateNote -> {
                createNote(result.name)
                return
            }
        }
        close()
    }

    private fun createNote(name: String) {
        val fileSystem = spaceRepository.current.value?.fileSystem ?: return
        viewModelScope.launch {
            val created = runCatching { CreateNote(fileSystem)(FilePath.Root, name) }.getOrNull()
            if (created != null) {
                cachedFiles = null
                mutableActions.tryEmit(SearchAction.OpenFile(created))
            }
            close()
        }
    }

    private fun runSearch(query: String) {
        val mode = mutableState.value.mode ?: return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (query.isNotEmpty()) delay(SearchDebounceMillis)
            mutableState.value = mutableState.value.copy(isSearching = true)
            val results = when (mode) {
                SearchMode.CommandPalette -> registry.search(query).map(SearchResult::Action)
                SearchMode.QuickOpen -> searchFileNames(query)
                SearchMode.ContentSearch -> searchContents(query)
            }
            mutableState.value = mutableState.value.copy(
                results = results,
                isSearching = false,
                selectedIndex = 0,
            )
        }
    }

    private suspend fun allFiles(): List<FileNode> {
        cachedFiles?.let { return it }
        val fileSystem = spaceRepository.current.value?.fileSystem ?: return emptyList()
        val files = runCatching { fileSystem.listFilesRecursively() }.getOrElse { emptyList() }
        cachedFiles = files
        return files
    }

    private suspend fun searchFileNames(query: String): List<SearchResult> {
        val files = allFiles()
        if (query.isBlank()) return files.take(50).map(SearchResult::File)
        val matches = withContext(dispatchers.default) {
            files
                .mapNotNull { file ->
                    val score = fuzzyScore(file.name, query) ?: fuzzyScore(file.path.value, query)
                    score?.let { file to it }
                }
                .sortedBy { it.second }
                .take(50)
                .map { SearchResult.File(it.first) }
        }
        return matches.ifEmpty { listOf(SearchResult.CreateNote(query.trim())) }
    }

    private suspend fun searchContents(query: String): List<SearchResult> {
        if (query.isBlank()) return emptyList()
        val fileSystem = spaceRepository.current.value?.fileSystem ?: return emptyList()
        val files = allFiles().filter { it.kind.isEditable }
        val matches = mutableListOf<SearchResult>()
        files.forEach { file ->
            if (matches.size >= MaxContentMatches) return@forEach
            val content = runCatching { fileSystem.readText(file.path) }.getOrNull() ?: return@forEach
            withContext(dispatchers.default) {
                if (!content.contains(query, ignoreCase = true)) return@withContext
                content.lineSequence().forEachIndexed { index, line ->
                    if (matches.size < MaxContentMatches && line.contains(query, ignoreCase = true)) {
                        matches.add(
                            SearchResult.Match(
                                path = file.path,
                                lineNumber = index + 1,
                                line = line.trim().take(160),
                            ),
                        )
                    }
                }
            }
        }
        return matches
    }
}
