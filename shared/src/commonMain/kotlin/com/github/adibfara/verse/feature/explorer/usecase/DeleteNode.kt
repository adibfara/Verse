package com.github.adibfara.verse.feature.explorer.usecase

import com.github.adibfara.verse.core.filesystem.SpaceFileSystem
import com.github.adibfara.verse.core.model.FileNode

class DeleteNode(private val fileSystem: SpaceFileSystem) {

    suspend operator fun invoke(node: FileNode) {
        fileSystem.delete(node.path)
    }
}
