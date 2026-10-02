package com.github.adibfara.mds.feature.explorer.usecase

import com.github.adibfara.mds.core.filesystem.SpaceFileSystem
import com.github.adibfara.mds.core.model.FilePath

class CreateFolder(private val fileSystem: SpaceFileSystem) {

    suspend operator fun invoke(parent: FilePath, name: String): FilePath =
        fileSystem.createDirectory(parent, name.trim())
}
