# feature/tabs

## Files

- `TabsViewModel.kt` — the open-tab list: activation, close, reorder, dirty flags.
- `ui/TabStrip.kt` — desktop strip with drag reorder; `ui/TabItem.kt` — one tab chip; `ui/TabsGridScreen.kt` — mobile grid.

## Seam

- `state: StateFlow<TabsUiState>` (`tabs: List<OpenTab>`, `activeTabId: TabId?`), `onEvent(event: TabsEvent)`, plus `restore(tabs, activeIndex)` and `clear()`.
- Tab identity is `TabId(path.value)`, built by `tabIdOf(path)`. Nothing keys off the file itself.
- Every mutation goes through a private `update {}` (no-op when unchanged) and then persists `WorkspaceTab(path)` / `activeTabIndex` into `WorkspaceSessionRepository`. That `WorkspaceState` is shared with `feature/explorer`, so write it through `update`, never directly.
- This VM never opens a file: it only tracks which paths are open. `app/ui/WorkspaceHost.kt` reacts to the active tab and calls `EditorEvent.Open`; explorer clicks arrive as `TabsEvent.Open` / `OpenPath`.
- `restore` is deliberately not an event: it is fed by `WorkspaceViewModel.restores`, a replay-1 SharedFlow that emits during `start()`, before the collector is composed.

Covered by `shared/src/commonTest/.../TabsViewModelTest.kt`.
