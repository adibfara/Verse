package com.github.adibfara.verse.feature.favorites

import com.github.adibfara.verse.core.filesystem.SpaceFileSystem
import com.github.adibfara.verse.core.model.FilePath
import com.github.adibfara.verse.feature.locks.LockedRepository
import com.github.adibfara.verse.feature.references.ReferencesStore

class ReferenceTrackingFileSystem(
    private val delegate: SpaceFileSystem,
    private val favorites: FavoritesRepository,
    private val locked: LockedRepository,
    private val references: ReferencesStore,
) : SpaceFileSystem by delegate {

    override suspend fun rename(path: FilePath, newName: String): FilePath {
        val result = delegate.rename(path, newName)
        favorites.onPathsRelocated(path, result)
        locked.onPathsRelocated(path, result)
        return result
    }

    override suspend fun move(path: FilePath, newParent: FilePath): FilePath {
        val result = delegate.move(path, newParent)
        favorites.onPathsRelocated(path, result)
        locked.onPathsRelocated(path, result)
        return result
    }

    override suspend fun delete(path: FilePath) {
        val removed = references.removeEntries(path)
        favorites.dropIds(removed)
        locked.dropIds(removed)
        delegate.delete(path)
    }
}
