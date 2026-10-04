package com.github.adibfara.verse.feature.explorer.usecase

import com.github.adibfara.verse.core.filesystem.SpaceFileSystem
import com.github.adibfara.verse.core.model.FileNode
import com.github.adibfara.verse.core.model.FilePath

class CopyNode(private val fileSystem: SpaceFileSystem) {

    suspend operator fun invoke(node: FileNode): FilePath =
        fileSystem.copy(node.path, node.path.parent ?: FilePath.Root)
}
