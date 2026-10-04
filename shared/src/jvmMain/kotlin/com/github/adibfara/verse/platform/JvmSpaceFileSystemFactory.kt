package com.github.adibfara.verse.core.filesystem

import com.github.adibfara.verse.core.model.Space
import java.nio.file.Paths

private class JvmSpaceFileSystemFactory : SpaceFileSystemFactory {
    override fun create(space: Space): SpaceFileSystem =
        JvmSpaceFileSystem(Paths.get(space.location).toAbsolutePath().normalize())
}

actual fun createSpaceFileSystemFactory(): SpaceFileSystemFactory = JvmSpaceFileSystemFactory()
