package com.github.adibfara.verse.feature.explorer.usecase

import com.github.adibfara.verse.core.filesystem.SpaceFileSystem
import com.github.adibfara.verse.core.model.FilePath

class CreateNote(private val fileSystem: SpaceFileSystem) {

    suspend operator fun invoke(parent: FilePath, name: String): FilePath {
        val fileName = withMarkdownExtension(name)
        val created = fileSystem.createFile(parent, fileName)
        fileSystem.writeText(created, "")
        return created
    }
}
