package com.github.adibfara.verse.feature.explorer.usecase

import com.github.adibfara.verse.core.filesystem.SpaceFileSystem
import com.github.adibfara.verse.core.model.FilePath

class CreateFolder(private val fileSystem: SpaceFileSystem) {

    suspend operator fun invoke(parent: FilePath, name: String): FilePath =
        fileSystem.createDirectory(parent, name.trim())
}
