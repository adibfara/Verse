# feature/search

## Files

- `SearchViewModel.kt` — quick open, content search and the command palette in one VM.
- `ui/SearchOverlay.kt` — the overlay and its result rows.

## Seam

- `state: StateFlow<SearchUiState>` (`mode: SearchMode?`, `query`, `results`, `selectedIndex`, `isSearching`; `isOpen` and `selected` are derived) and `actions: SharedFlow<SearchAction>` (`OpenFile`, `RunCommand`).
- Entry points are `open(mode)`, `close()`, `queryChanged(...)`, `moveSelection(...)`, `activateSelected()`, `activate(result)` and `invalidateFileCache()` — this VM deliberately does **not** follow the `onEvent` convention.
- It never opens a tab and never writes a file: results leave as `SearchAction` and `AppRoot` turns them into `TabsEvent.OpenPath` or `handleCommand`. The one exception is `SearchResult.CreateNote`, which goes through `feature.explorer.usecase.CreateNote`.
- Content search walks the space once and caches the file list, so `invalidateFileCache()` must be called whenever the tree changes. `AppRoot` does that by collecting `ExplorerViewModel.actions`.
- Content search only scans `FileKind.isEditable` files. `MaxContentMatches = 200`, debounce 120 ms.

No tests.
