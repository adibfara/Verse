package com.github.adibfara.mds.core.filesystem

import com.github.adibfara.mds.core.model.Space

private val DemoSpaceContent = mapOf(
    "Welcome.md" to """
        # Welcome to MDs

        A deliberately small markdown workspace.

        ## What works here

        - Browse folders in the sidebar
        - Open files as tabs
        - Edit with **live** styling

        - [ ] Try the preview mode
        - [x] Open a note

        Switch to *raw* mode to see the markdown source exactly as written.
    """.trimIndent(),
    "Projects/MDs.md" to """
        # Test header

        ## Test subtitle

        test text

        - Bullets
            - bullets
            - bullet 2

        - [ ] Check list

        *italic* and **bold**
    """.trimIndent(),
    "Projects/Roadmap.md" to """
        # Roadmap

        1. Indexing
        2. Backlinks
        3. Graph view

        > Everything here is stored in memory on Android for now.
    """.trimIndent(),
    "Daily/2026-10-02.md" to """
        # 2026-10-02

        - Morning notes
        - Afternoon notes

        `inline code` and a [link](https://example.com).
    """.trimIndent(),
    "Resources/Notes.txt" to "A plain text file opens in raw mode.",
)

private class AndroidSpaceFileSystemFactory : SpaceFileSystemFactory {

    private val instances = mutableMapOf<String, SpaceFileSystem>()

    override fun create(space: Space): SpaceFileSystem = instances.getOrPut(space.location) {
        InMemorySpaceFileSystem(rootLocation = space.location, seed = DemoSpaceContent)
    }
}

actual fun createSpaceFileSystemFactory(): SpaceFileSystemFactory = AndroidSpaceFileSystemFactory()
