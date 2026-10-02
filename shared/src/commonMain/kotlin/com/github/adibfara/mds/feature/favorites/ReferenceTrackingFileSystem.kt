package com.github.adibfara.mds.feature.favorites

import com.github.adibfara.mds.core.filesystem.SpaceFileSystem
import com.github.adibfara.mds.core.model.FilePath

class ReferenceTrackingFileSystem(
    private val delegate: SpaceFileSystem,
    private val favorites: FavoritesRepository,
) : SpaceFileSystem by delegate {

    override suspend fun rename(path: FilePath, newName: String): FilePath {
        val result = delegate.rename(path, newName)
        favorites.onPathsRelocated(path, result)
        return result
    }

    override suspend fun move(path: FilePath, newParent: FilePath): FilePath {
        val result = delegate.move(path, newParent)
        favorites.onPathsRelocated(path, result)
        return result
    }

    override suspend fun delete(path: FilePath) {
        favorites.onPathDeleted(path)
        delegate.delete(path)
    }
}
