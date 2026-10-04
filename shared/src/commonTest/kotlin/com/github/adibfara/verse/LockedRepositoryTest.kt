package com.github.adibfara.verse

import com.github.adibfara.verse.core.filesystem.InMemorySpaceFileSystem
import com.github.adibfara.verse.core.model.FilePath
import com.github.adibfara.verse.feature.locks.LockedRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LockedRepositoryTest {

    private fun fileSystem() = InMemorySpaceFileSystem(
        seed = mapOf(
            "notes/one.md" to "one",
            "other.md" to "other",
        ),
    )

    @Test
    fun lockingSurvivesAFreshRepositoryOnTheSameSpace() = runTest {
        val fileSystem = fileSystem()
        assertTrue(LockedRepository(fileSystem).setLocked(FilePath("notes/one.md"), true))

        assertTrue(LockedRepository(fileSystem).isLocked(FilePath("notes/one.md")))
    }

    @Test
    fun unlockingSurvivesAFreshRepositoryOnTheSameSpace() = runTest {
        val fileSystem = fileSystem()
        LockedRepository(fileSystem).setLocked(FilePath("notes/one.md"), true)

        assertFalse(LockedRepository(fileSystem).setLocked(FilePath("notes/one.md"), false))
        assertFalse(LockedRepository(fileSystem).isLocked(FilePath("notes/one.md")))
    }

    @Test
    fun lockedPathsResolveAfterAFreshRepository() = runTest {
        val fileSystem = fileSystem()
        LockedRepository(fileSystem).setLocked(FilePath("other.md"), true)

        assertTrue(FilePath("other.md") in LockedRepository(fileSystem).lockedPaths())
    }
}
