package com.github.adibfara.verse

import com.github.adibfara.verse.core.filesystem.InMemorySpaceFileSystem
import com.github.adibfara.verse.core.model.FilePath
import com.github.adibfara.verse.feature.favorites.FavoritesRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FavoritesRepositoryTest {

    private fun fileSystem() = InMemorySpaceFileSystem(
        seed = mapOf(
            "notes/one.md" to "one",
            "notes/two.md" to "two",
            "other.md" to "other",
        ),
    )

    @Test
    fun togglingFavoriteFlipsItsState() = runTest {
        val repository = FavoritesRepository(fileSystem())
        val path = FilePath("notes/one.md")

        assertFalse(repository.isFavorite(path))
        assertTrue(repository.toggleFavorite(path))
        assertTrue(repository.isFavorite(path))
        assertFalse(repository.toggleFavorite(path))
        assertFalse(repository.isFavorite(path))
    }

    @Test
    fun favoritedPathsReflectsEveryToggledOnFile() = runTest {
        val repository = FavoritesRepository(fileSystem())
        repository.toggleFavorite(FilePath("notes/one.md"))
        repository.toggleFavorite(FilePath("other.md"))

        assertEquals(
            setOf(FilePath("notes/one.md"), FilePath("other.md")),
            repository.favoritedPaths(),
        )
    }

    @Test
    fun relocatingAFavoritedFileUpdatesItsReference() = runTest {
        val repository = FavoritesRepository(fileSystem())
        val original = FilePath("notes/one.md")
        repository.toggleFavorite(original)

        val renamed = FilePath("notes/renamed.md")
        repository.onPathsRelocated(original, renamed)

        assertFalse(repository.isFavorite(original))
        assertTrue(repository.isFavorite(renamed))
        assertEquals(setOf(renamed), repository.favoritedPaths())
    }

    @Test
    fun relocatingAFolderRewritesEveryDescendantReference() = runTest {
        val repository = FavoritesRepository(fileSystem())
        repository.toggleFavorite(FilePath("notes/one.md"))
        repository.toggleFavorite(FilePath("notes/two.md"))

        repository.onPathsRelocated(FilePath("notes"), FilePath("archive/notes"))

        assertEquals(
            setOf(FilePath("archive/notes/one.md"), FilePath("archive/notes/two.md")),
            repository.favoritedPaths(),
        )
    }

    @Test
    fun deletingAFavoritedFileRemovesItFromTheFilter() = runTest {
        val repository = FavoritesRepository(fileSystem())
        repository.toggleFavorite(FilePath("notes/one.md"))
        repository.toggleFavorite(FilePath("other.md"))

        repository.onPathDeleted(FilePath("notes/one.md"))

        assertEquals(setOf(FilePath("other.md")), repository.favoritedPaths())
    }
}
