# feature/space

## Files

- `SpaceRepository.kt` — opens and closes spaces; owns the whole `OpenedSpace` graph.
- `ui/SpacePickerScreen.kt` — the start screen for choosing or opening a space.

## Seam

- `current: StateFlow<OpenedSpace?>` is the root of every reactive chain in the app. Everything else reads it: `current.value?.fileSystem`, `.favorites`, `.locked`.
- `open()` builds the graph in one order and shares a single `ReferencesStore` id space across it:
  `ReferencesStore(raw)` → `FavoritesRepository(raw, references)` → `LockedRepository(raw, references)` → `ReferenceTrackingFileSystem(raw, favorites, locked, references)`.
- `open()` remembers the space in settings; `openSingleFile()` builds an ephemeral space, does not remember it, and sets `OpenedSpace.initialFile`.
- `close()` only nulls `current`. Flushing settings/session is the caller's job (`WorkspaceViewModel`).

## Gotchas

- Never construct `FavoritesRepository`, `LockedRepository`, `ReferencesStore` or `ReferenceTrackingFileSystem` anywhere else. Every one of them defaults to creating its own `ReferencesStore`, which silently gives it a separate id space, and favourites and locks then stop tracking renames instead of failing loudly.
- There is no `onEvent` and no sealed event here — this is a repository, not a ViewModel.
- No test targets this package directly; `EditorFormattingTest` drives it through a real instance over `InMemorySpaceFileSystem`.
