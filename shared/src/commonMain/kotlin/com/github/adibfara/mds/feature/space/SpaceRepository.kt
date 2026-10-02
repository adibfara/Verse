package com.github.adibfara.mds.feature.space

import com.github.adibfara.mds.core.common.currentTimeMillis
import com.github.adibfara.mds.core.filesystem.SpaceFileSystem
import com.github.adibfara.mds.core.filesystem.SpaceFileSystemFactory
import com.github.adibfara.mds.core.model.FilePath
import com.github.adibfara.mds.core.model.Space
import com.github.adibfara.mds.core.model.SpaceId
import com.github.adibfara.mds.core.persistence.SettingsStore
import com.github.adibfara.mds.core.platform.LocationResolver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class OpenedSpace(
    val space: Space,
    val fileSystem: SpaceFileSystem,
    val initialFile: FilePath? = null,
)

class SpaceRepository(
    private val fileSystemFactory: SpaceFileSystemFactory,
    private val locationResolver: LocationResolver,
    private val settingsStore: SettingsStore,
) {

    private val openedSpace = MutableStateFlow<OpenedSpace?>(null)

    val current: StateFlow<OpenedSpace?> = openedSpace.asStateFlow()

    fun spaceAt(location: String, ephemeral: Boolean = false): Space = Space(
        id = SpaceId(location),
        name = locationResolver.nameOf(location),
        location = location,
        isEphemeral = ephemeral,
    )

    fun open(space: Space): OpenedSpace {
        val opened = OpenedSpace(space = space, fileSystem = fileSystemFactory.create(space))
        openedSpace.value = opened
        settingsStore.rememberSpace(space, currentTimeMillis())
        return opened
    }

    fun openFolder(location: String): OpenedSpace = open(spaceAt(location))

    fun openSingleFile(filePath: String): OpenedSpace? {
        val parent = locationResolver.parentOf(filePath) ?: return null
        val space = spaceAt(parent, ephemeral = true)
        val fileSystem = fileSystemFactory.create(space)
        val relative = locationResolver.relativize(parent, filePath) ?: return null
        val opened = OpenedSpace(
            space = space,
            fileSystem = fileSystem,
            initialFile = FilePath(relative),
        )
        openedSpace.value = opened
        return opened
    }

    fun close() {
        openedSpace.value = null
    }

    fun isDirectory(location: String): Boolean = locationResolver.isDirectory(location)

    fun isFile(location: String): Boolean = locationResolver.isFile(location)
}
