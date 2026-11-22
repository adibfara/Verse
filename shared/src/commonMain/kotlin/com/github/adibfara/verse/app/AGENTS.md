# app

## Files

- `AppRoot.kt` — top-level composable, `handleCommand`, composition of every feature.
- `WorkspaceViewModel.kt` — app-level state, space open/close, settings plumbing.
- `ui/WorkspaceHost.kt` — `WorkspaceEffects` (the only cross-feature wiring) plus `rememberLinkOpener` / `rememberSpaceImageLoader`.
- `ui/DesktopShell.kt`, `ui/MobileShell.kt`, `ui/MobileDrawerOverlay.kt`, `ui/MobileBottomBar.kt` — the shells.
- `di/AppModule.kt` — Koin wiring for everything; `di/Startup.kt` — `initializeDependencies()`, `externalOpenRequests()`.

## Who renders what

- `AppRoot`: no space → `SpacePickerScreen`; else `WorkspaceScreen`. It also composes `SettingsPanel`, `SearchOverlay` and `PlatformFolderPickerOverlay()`.
- `DesktopShell`: `TabStrip`; sidebar (`FileTree` or `DualPaneExplorer`); `EditorPane`; explorer overlays `FileContextMenu`, `EmptyAreaContextMenu`, `ExplorerDialogHost`. Window buttons come from `WindowChrome`.
- `MobileShell`: drawer → `MobileDrawerPanel` → `FileTree`; body → `EditorPane` or `TabsGridScreen`; `MobileBottomBar`; overlays `ExplorerDialogHost`, `FileActionsBottomSheet`, `EmptyAreaActionsBottomSheet`.

## Rules

- Features never call each other. Any "feature A affects feature B" behaviour goes in `WorkspaceEffects`, never inside a ViewModel. Explorer `actions` → tabs/editor, editor `dirtyChanges`/`actions` → tabs, active tab → editor + explorer follow, `workspaceViewModel.restores` → `tabsViewModel.restore`.
- `handleCommand` is the only place a `CommandId` becomes behaviour; `CommandRegistry` stays behaviour-free. `workspace.jumpToTab.*` → tabs, `file.newNote`/`file.newFolder` → explorer, `file.save` and `view.toggle*` → editor, `navigation.*` → search.
- Adding UI for a feature means editing both shells: desktop popup/overlay and mobile bottom sheet. Look for the existing overlay's name before adding a new one.
- `WorkspaceViewModel.restores` is a `MutableSharedFlow(replay = 1)` on purpose — it emits during `start()`, before `WorkspaceEffects` is composed. Dropping the replay silently breaks session restore.

No test constructs `AppRoot`, the shells, `AppModule` or `WorkspaceViewModel`.
