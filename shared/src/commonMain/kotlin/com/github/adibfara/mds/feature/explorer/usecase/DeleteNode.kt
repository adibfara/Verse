package com.github.adibfara.mds.feature.explorer.usecase

import com.github.adibfara.mds.core.filesystem.SpaceFileSystem
import com.github.adibfara.mds.core.model.FileNode

class DeleteNode(private val fileSystem: SpaceFileSystem) {

    suspend operator fun invoke(node: FileNode) {
        fileSystem.delete(node.path)
    }
}
