package com.github.adibfara.mds.feature.explorer.usecase

import com.github.adibfara.mds.core.filesystem.SpaceFileSystem
import com.github.adibfara.mds.core.model.FileNode
import com.github.adibfara.mds.core.model.FilePath

class CopyNode(private val fileSystem: SpaceFileSystem) {

    suspend operator fun invoke(node: FileNode): FilePath =
        fileSystem.copy(node.path, node.path.parent ?: FilePath.Root)
}
