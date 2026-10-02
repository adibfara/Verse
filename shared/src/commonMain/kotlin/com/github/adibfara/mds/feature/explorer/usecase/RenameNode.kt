package com.github.adibfara.mds.feature.explorer.usecase

import com.github.adibfara.mds.core.filesystem.SpaceFileSystem
import com.github.adibfara.mds.core.model.FileNode
import com.github.adibfara.mds.core.model.FilePath

class RenameNode(private val fileSystem: SpaceFileSystem) {

    suspend operator fun invoke(node: FileNode, newName: String): FilePath {
        val target = if (node.isDirectory) newName.trim() else keepExtension(node.path, newName)
        return fileSystem.rename(node.path, target)
    }

    private fun keepExtension(path: FilePath, newName: String): String {
        val trimmed = newName.trim()
        if (trimmed.contains('.')) return trimmed
        val extension = path.extension
        return if (extension.isEmpty()) trimmed else "$trimmed.$extension"
    }
}
