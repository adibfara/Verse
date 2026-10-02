package com.github.adibfara.mds.feature.tabs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.adibfara.mds.core.model.FileKind
import com.github.adibfara.mds.core.model.FileNode
import com.github.adibfara.mds.core.model.FilePath
import com.github.adibfara.mds.core.model.OpenTab
import com.github.adibfara.mds.core.model.TabId
import com.github.adibfara.mds.core.persistence.WorkspaceSessionRepository
import com.github.adibfara.mds.core.persistence.WorkspaceTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TabsUiState(
    val tabs: List<OpenTab> = emptyList(),
    val activeTabId: TabId? = null,
) {
    val activeTab: OpenTab? get() = tabs.firstOrNull { it.id == activeTabId }

    val activeIndex: Int get() = tabs.indexOfFirst { it.id == activeTabId }
}

sealed interface TabsEvent {
    data class Open(val node: FileNode) : TabsEvent
    data class OpenPath(val path: FilePath) : TabsEvent
    data class Activate(val id: TabId) : TabsEvent
    data class Close(val id: TabId) : TabsEvent
    data object CloseActive : TabsEvent
    data class CloseOthers(val id: TabId) : TabsEvent
    data object CloseAll : TabsEvent
    data class Move(val from: Int, val to: Int) : TabsEvent
    data object Next : TabsEvent
    data object Previous : TabsEvent
    data class JumpTo(val index: Int) : TabsEvent
    data class MarkDirty(val path: FilePath, val isDirty: Boolean) : TabsEvent
    data class Renamed(val from: FilePath, val to: FilePath) : TabsEvent
    data class Removed(val path: FilePath) : TabsEvent
}

fun tabIdOf(path: FilePath) = TabId(path.value)

class TabsViewModel(
    private val sessionRepository: WorkspaceSessionRepository,
) : ViewModel() {

    private val mutableState = MutableStateFlow(TabsUiState())
    val state: StateFlow<TabsUiState> = mutableState.asStateFlow()

    fun restore(tabs: List<WorkspaceTab>, activeIndex: Int) {
        val restored = tabs.map { tab ->
            OpenTab(
                id = tabIdOf(tab.path),
                path = tab.path,
                title = tab.path.nameWithoutExtension,
                kind = FileKind.fromExtension(tab.path.extension),
            )
        }
        mutableState.value = TabsUiState(
            tabs = restored,
            activeTabId = restored.getOrNull(activeIndex)?.id ?: restored.firstOrNull()?.id,
        )
    }

    fun clear() {
        mutableState.value = TabsUiState()
    }

    fun onEvent(event: TabsEvent) {
        when (event) {
            is TabsEvent.Open -> open(event.node.path, event.node.kind)
            is TabsEvent.OpenPath -> open(event.path, FileKind.fromExtension(event.path.extension))
            is TabsEvent.Activate -> activate(event.id)
            is TabsEvent.Close -> close(event.id)
            TabsEvent.CloseActive -> mutableState.value.activeTabId?.let(::close)
            is TabsEvent.CloseOthers -> closeOthers(event.id)
            TabsEvent.CloseAll -> update { TabsUiState() }
            is TabsEvent.Move -> move(event.from, event.to)
            TabsEvent.Next -> cycle(1)
            TabsEvent.Previous -> cycle(-1)
            is TabsEvent.JumpTo -> jumpTo(event.index)
            is TabsEvent.MarkDirty -> markDirty(event.path, event.isDirty)
            is TabsEvent.Renamed -> renamed(event.from, event.to)
            is TabsEvent.Removed -> close(tabIdOf(event.path))
        }
    }

    private fun open(path: FilePath, kind: FileKind) {
        val id = tabIdOf(path)
        update { current ->
            if (current.tabs.any { it.id == id }) {
                current.copy(activeTabId = id)
            } else {
                val tab = OpenTab(
                    id = id,
                    path = path,
                    title = path.nameWithoutExtension,
                    kind = kind,
                )
                current.copy(tabs = current.tabs + tab, activeTabId = id)
            }
        }
    }

    private fun activate(id: TabId) = update { it.copy(activeTabId = id) }

    private fun close(id: TabId) = update { current ->
        val index = current.tabs.indexOfFirst { it.id == id }
        if (index < 0) return@update current
        val remaining = current.tabs.filterNot { it.id == id }
        val nextActive = when {
            current.activeTabId != id -> current.activeTabId
            remaining.isEmpty() -> null
            else -> remaining[index.coerceAtMost(remaining.lastIndex)].id
        }
        current.copy(tabs = remaining, activeTabId = nextActive)
    }

    private fun closeOthers(id: TabId) = update { current ->
        val kept = current.tabs.filter { it.id == id }
        current.copy(tabs = kept, activeTabId = kept.firstOrNull()?.id)
    }

    private fun move(from: Int, to: Int) = update { current ->
        if (from !in current.tabs.indices || to !in current.tabs.indices || from == to) {
            return@update current
        }
        val reordered = current.tabs.toMutableList()
        reordered.add(to, reordered.removeAt(from))
        current.copy(tabs = reordered)
    }

    private fun cycle(step: Int) = update { current ->
        if (current.tabs.isEmpty()) return@update current
        val index = current.activeIndex.takeIf { it >= 0 } ?: 0
        val size = current.tabs.size
        val next = ((index + step) % size + size) % size
        current.copy(activeTabId = current.tabs[next].id)
    }

    private fun jumpTo(index: Int) = update { current ->
        current.tabs.getOrNull(index)?.let { current.copy(activeTabId = it.id) } ?: current
    }

    private fun markDirty(path: FilePath, isDirty: Boolean) = update { current ->
        val id = tabIdOf(path)
        if (current.tabs.none { it.id == id && it.isDirty != isDirty }) return@update current
        current.copy(
            tabs = current.tabs.map { if (it.id == id) it.copy(isDirty = isDirty) else it },
        )
    }

    private fun renamed(from: FilePath, to: FilePath) = update { current ->
        val oldId = tabIdOf(from)
        if (current.tabs.none { it.id == oldId }) return@update current
        val newId = tabIdOf(to)
        current.copy(
            tabs = current.tabs.map { tab ->
                if (tab.id == oldId) {
                    tab.copy(id = newId, path = to, title = to.nameWithoutExtension)
                } else {
                    tab
                }
            },
            activeTabId = if (current.activeTabId == oldId) newId else current.activeTabId,
        )
    }

    private fun update(transform: (TabsUiState) -> TabsUiState) {
        val updated = transform(mutableState.value)
        if (updated == mutableState.value) return
        mutableState.value = updated
        persist(updated)
    }

    private fun persist(state: TabsUiState) {
        viewModelScope.launch {
            sessionRepository.update { session ->
                session.copy(
                    tabs = state.tabs.map { WorkspaceTab(path = it.path) },
                    activeTabIndex = state.activeIndex,
                )
            }
        }
    }
}
