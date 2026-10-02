package com.github.adibfara.mds.feature.explorer.usecase

import com.github.adibfara.mds.core.filesystem.SpaceFileSystem
import com.github.adibfara.mds.core.model.FilePath

class CreateNote(private val fileSystem: SpaceFileSystem) {

    suspend operator fun invoke(parent: FilePath, name: String): FilePath {
        val fileName = withMarkdownExtension(name)
        val created = fileSystem.createFile(parent, fileName)
        fileSystem.writeText(created, "")
        return created
    }
}
