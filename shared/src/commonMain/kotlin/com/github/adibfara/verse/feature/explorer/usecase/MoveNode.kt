package com.github.adibfara.verse.feature.explorer.usecase

import com.github.adibfara.verse.core.filesystem.FileSystemException
import com.github.adibfara.verse.core.filesystem.SpaceFileSystem
import com.github.adibfara.verse.core.model.FileNode
import com.github.adibfara.verse.core.model.FilePath

class MoveNode(private val fileSystem: SpaceFileSystem) {

    suspend operator fun invoke(node: FileNode, destination: FilePath): FilePath {
        if (node.isDirectory && destination.isDescendantOf(node.path)) {
            throw FileSystemException("A folder cannot be moved into itself")
        }
        if (node.path.parent?.value == destination.value) return node.path
        return fileSystem.move(node.path, destination)
    }
}
