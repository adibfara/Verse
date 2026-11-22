# feature/locks

## Files

- `LockedRepository.kt` — the persisted set of locked document ids for a space. No UI, no ViewModel, no events.

## Seam

- `isLocked(path)`, `setLocked(path, isLocked): Boolean`, `lockedPaths()`, `onPathsRelocated(old, new)`, `onPathDeleted(path)`, `dropIds(ids)`.
- Consumed in two places only: `SpaceRepository` exposes it as `OpenedSpace.locked`, and `EditorViewModel` uses it under `LockBehavior.RememberPerFile` — `EditorEvent.ToggleLock` writes, `isLocked` decides whether the field is editable.
- Locked records are `Long` reference ids from `feature/references`, not paths. `lockedPaths()` reverse-maps them through `references.entries()`.

## Gotchas

- The document is loaded lazily into an in-memory cache guarded by a `Mutex`, persisted to `.locked`, and read/write failures are swallowed by `runCatching` — a broken `.locked` degrades to "nothing locked" rather than an error.
- `onPathDeleted` and `lockedPaths()` have no production caller today; deletion is handled by `dropIds` from `ReferenceTrackingFileSystem`. If you add a delete path elsewhere, route it through the same wrapper.

Covered by `shared/src/commonTest/.../LockedRepositoryTest.kt`.
