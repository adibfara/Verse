# feature/favorites

## Files

- `FavoritesRepository.kt` — favourites, persisted as a set of reference ids in `.favorites`.
- `ReferenceTrackingFileSystem.kt` — a `SpaceFileSystem` decorator that repairs references on rename, move and delete.

## Seam

- `paths: StateFlow<Set<FilePath>>` is what UI reads. It reaches the tree through `ExplorerViewModel`, which `flatMapLatest`s it into `ExplorerUiState.favoritedPaths`. `toggleFavorite`, `isFavorite` and `favoritedPaths()` are suspend.
- Favourites are stored as `Long` ids minted by `feature/references`, never as paths, so a rename keeps the star. Only `publishLocked()` emits `paths` — load, toggle, relocate and delete are the only ways it can change.
- `ReferenceTrackingFileSystem` overrides exactly `rename`, `move` and `delete`: rename/move call `onPathsRelocated` on favourites *and* locks; delete calls `references.removeEntries(path)` and then `dropIds` on both.

## Gotchas

- `onPathDeleted` exists and is covered by tests, but the production delete path does not call it (delete goes through `dropIds`). Do not "tidy up" one to match the other without changing the wrapper.
- Constructed only by `SpaceRepository` — see `feature/space/AGENTS.md`.

Covered by `shared/src/commonTest/.../FavoritesRepositoryTest.kt`.
