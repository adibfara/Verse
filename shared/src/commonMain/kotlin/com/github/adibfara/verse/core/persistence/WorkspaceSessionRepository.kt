package com.github.adibfara.verse.core.persistence

import com.github.adibfara.verse.core.model.Space
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WorkspaceSessionRepository(
    private val store: WorkspaceStore,
) {

    private val state = MutableStateFlow(WorkspaceState.Empty)
    private var attachedSpace: Space? = null

    val session: StateFlow<WorkspaceState> = state.asStateFlow()

    suspend fun attach(space: Space): WorkspaceState {
        attachedSpace = space
        val restored = store.load(space)
        state.value = restored
        return restored
    }

    fun detach() {
        attachedSpace = null
        state.value = WorkspaceState.Empty
    }

    fun update(transform: (WorkspaceState) -> WorkspaceState) {
        val updated = transform(state.value)
        if (updated == state.value) return
        state.value = updated
        attachedSpace?.let { store.save(it, updated) }
    }

    suspend fun flush() {
        attachedSpace?.let { store.flush(it, state.value) }
    }
}
