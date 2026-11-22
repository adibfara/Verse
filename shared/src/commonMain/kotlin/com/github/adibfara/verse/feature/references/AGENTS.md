# feature/references

## Files

- `ReferencesStore.kt` — stable numeric ids for space-relative paths, persisted in `.references`. No UI, no state flow, no events.

## Seam

- `idFor(path): Long?`, `mintId(path): Long` (idempotent — returns the existing id), `entries(): Map<String, String>`, `relocate(old, new): Boolean`, `removeEntries(path): Set<Long>`.
- The whole point of the id space: favourites and locks store ids, this store maps ids back to paths, so a rename or a move does not lose a star or a lock.
- `relocate` rewrites the exact entry and every descendant, and reports whether anything changed. `removeEntries` does the same and returns the removed ids so callers can prune their own documents.
- `SpaceRepository.open()` creates one instance and passes it explicitly to favourites, locks and the filesystem wrapper. The default constructor argument would create an isolated store.

## Gotchas

- Like its callers, this is lazy: `loadIfNeeded()` reads `.references` once and every operation runs under one `Mutex`. Assume every method is suspend and may hit storage.
- A new consumer that forgets to take the shared instance still compiles and still works in isolation — it just misses relocations. That is the failure mode to watch for.

No direct test; behaviour is covered through `FavoritesRepositoryTest` and `LockedRepositoryTest`.
