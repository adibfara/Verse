# feature/explorer

## Files

- `ExplorerViewModel.kt` — the only VM: tree cache, watchers, dialogs, menus, CRUD.
- `ExplorerUiState.kt` — `ExplorerUiState`, `TreeRow`, `ContextMenuTarget`, `EmptyAreaMenuTarget`, `ExplorerDialog`.
- `ExplorerEvent.kt` — `ExplorerEvent` (UI → VM) and `ExplorerAction` (VM → app).
- `ui/FileTree.kt` — tree layout: toolbar, `LazyColumn`, footer, and the empty-area right-click.
- `ui/FileTreeRow.kt` — one row: hover, indent, `itemActions`.
- `ui/ExplorerToolbar.kt`, `ui/ExplorerDialogs.kt` — toolbar buttons; New note / New folder / Rename / Move / Delete dialogs.
- `ui/FileContextMenu.kt` — row context menu, both the desktop popup and the mobile bottom sheet, plus the empty-area entries.
- `ui/DualPaneExplorer.kt` — the alternate two-pane layout.
- `usecase/` — `CreateNote`, `CreateFolder`, `RenameNode`, `MoveNode`, `CopyNode`, `DeleteNode`, `validateName` / `withMarkdownExtension`.

## Seam

- `state: StateFlow<ExplorerUiState>`, `onEvent(event: ExplorerEvent)`, `actions: SharedFlow<ExplorerAction>`.
- **Nothing in this package renders a popup.** `app/ui/DesktopShell.kt` renders `FileContextMenu` and `EmptyAreaContextMenu`; `app/ui/MobileShell.kt` renders `FileActionsBottomSheet` and `EmptyAreaActionsBottomSheet`. A new menu means a VM event plus a change in all three files.
- Creating a note or folder is always the same path: event → `openDialog(ExplorerDialog.NewNote/NewFolder)` → `NamedDialog` → `confirmNamed` → `CreateNote`/`CreateFolder`. `openDialog` also clears whichever menu is open, so the menu actions must go through it.
- `resolveParent(null)` resolves the creation parent from `selectedPath` (the folder itself, or the selected file's parent, else root). The toolbar and `Ctrl+N` / `Ctrl+Shift+N` pass null, and so do the empty-area menu entries.
- Directories are listed only when expanded (`loadDirectory` on expand); `watch()` invalidates one directory and the rebuild is debounced 250 ms.

- `FileTree(showListModes = true)` (the mobile drawer only) hides the sort and favourites-filter buttons and adds
  Recent / All / Favorite pages (`ExplorerListMode`) in a `HorizontalPager`, so they can be swiped as well as tapped.
  The pager drives the VM through `settledPage` and follows `listMode` through `targetPage`; using `currentPage` would
  emit the middle page while animating Recent → Favorite and cancel its own animation. Recent is `listFilesRecursively`
  sorted by `lastModified` descending, capped at 200; Favorite is `metadata()` per favourited path, folders first. Both
  load when the drawer opens (`RefreshList`), never at startup.
- Recent is stale-while-revalidate: the last walk is persisted per space as `WorkspaceState.recentFiles`
  (`RecentFileEntry`, in `workspace.json`) and shown immediately, then the walk runs in the background and replaces it.
  "Loading..." only appears when there is no cache at all. A walk that finishes after the space changed is dropped
  (`fileSystem !== target`). A favourited folder opens the All page revealed and expanded (`RevealFolder`).

## Gotchas

- `ContextMenuTarget` carries a `FileNode`, so it cannot express empty space. Empty space uses `EmptyAreaMenuTarget` + `ExplorerEvent.OpenEmptyAreaMenu(x, y)`.
- Menu and row coordinates are root-relative integers (`positionInRoot()` plus the local offset) because the popup is aligned to the shell's root `Box`. Convert in the composable that owns the origin, never in the VM.
- `favoritedPaths` is pushed into this state from `spaceRepository.current.favorites.paths`; the VM does not own favourites.
- `resolveParent` is also what a "new note here" from a folder row uses, so an empty-area menu added elsewhere must not bypass it.

No tests exist for this feature.
