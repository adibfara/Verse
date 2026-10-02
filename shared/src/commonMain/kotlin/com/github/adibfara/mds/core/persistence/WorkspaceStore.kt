package com.github.adibfara.mds.core.persistence

import com.github.adibfara.mds.core.common.Debouncer
import com.github.adibfara.mds.core.common.DispatcherProvider
import com.github.adibfara.mds.core.model.Space
import com.github.adibfara.mds.core.platform.AppPaths
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext

private const val MetadataDirectoryName = ".mds"
private const val WorkspaceFileName = "workspace.json"
private const val MemoryLocationPrefix = "memory://"

class WorkspaceStore(
    private val storage: FileStorage,
    private val appPaths: AppPaths,
    private val dispatchers: DispatcherProvider,
    private val scope: CoroutineScope,
) {

    private val debouncer = Debouncer(scope, delayMillis = 500)

    private fun pathFor(space: Space): String {
        val storesInsideSpace = !space.isEphemeral && !space.location.startsWith(MemoryLocationPrefix)
        return if (storesInsideSpace) {
            storage.join(space.location, MetadataDirectoryName, WorkspaceFileName)
        } else {
            storage.join(appPaths.appDataDirectory, "workspaces", "${sanitize(space.id.value)}.json")
        }
    }

    private fun sanitize(value: String): String =
        value.map { character -> if (character.isLetterOrDigit()) character else '-' }
            .joinToString("")
            .take(120)

    suspend fun load(space: Space): WorkspaceState = withContext(dispatchers.io) {
        val raw = storage.read(pathFor(space))
        raw
            ?.withoutByteOrderMark()
            ?.let { runCatching { MdsJson.decodeFromString<WorkspaceState>(it) }.getOrNull() }
            ?: WorkspaceState.Empty
    }

    fun save(space: Space, state: WorkspaceState) {
        debouncer.submit {
            withContext(dispatchers.io) {
                storage.write(pathFor(space), MdsJson.encodeToString(state))
            }
        }
    }

    suspend fun flush(space: Space, state: WorkspaceState) = withContext(dispatchers.io) {
        debouncer.cancel()
        storage.write(pathFor(space), MdsJson.encodeToString(state))
    }
}
