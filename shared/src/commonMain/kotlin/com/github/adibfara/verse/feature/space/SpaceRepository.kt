package com.github.adibfara.verse.feature.space

import com.github.adibfara.verse.core.common.currentTimeMillis
import com.github.adibfara.verse.core.filesystem.SpaceFileSystem
import com.github.adibfara.verse.core.filesystem.SpaceFileSystemFactory
import com.github.adibfara.verse.core.model.FilePath
import com.github.adibfara.verse.core.model.Space
import com.github.adibfara.verse.core.model.SpaceId
import com.github.adibfara.verse.core.persistence.SettingsStore
import com.github.adibfara.verse.core.platform.LocationResolver
import com.github.adibfara.verse.feature.favorites.FavoritesRepository
import com.github.adibfara.verse.feature.favorites.ReferenceTrackingFileSystem
import com.github.adibfara.verse.feature.locks.LockedRepository
import com.github.adibfara.verse.feature.references.ReferencesStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class OpenedSpace(
    val space: Space,
    val fileSystem: SpaceFileSystem,
    val favorites: FavoritesRepository,
    val locked: LockedRepository,
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
        val raw = fileSystemFactory.create(space)
        val references = ReferencesStore(raw)
        val favorites = FavoritesRepository(raw, references)
        val locked = LockedRepository(raw, references)
        val fileSystem = ReferenceTrackingFileSystem(raw, favorites, locked, references)
        val opened = OpenedSpace(
            space = space,
            fileSystem = fileSystem,
            favorites = favorites,
            locked = locked,
        )
        openedSpace.value = opened
        settingsStore.rememberSpace(space, currentTimeMillis())
        return opened
    }

    fun openFolder(location: String): OpenedSpace = open(spaceAt(location))

    fun openSingleFile(filePath: String): OpenedSpace? {
        val parent = locationResolver.parentOf(filePath) ?: return null
        val space = spaceAt(parent, ephemeral = true)
        val raw = fileSystemFactory.create(space)
        val references = ReferencesStore(raw)
        val favorites = FavoritesRepository(raw, references)
        val locked = LockedRepository(raw, references)
        val fileSystem = ReferenceTrackingFileSystem(raw, favorites, locked, references)
        val relative = locationResolver.relativize(parent, filePath) ?: return null
        val opened = OpenedSpace(
            space = space,
            fileSystem = fileSystem,
            favorites = favorites,
            locked = locked,
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
