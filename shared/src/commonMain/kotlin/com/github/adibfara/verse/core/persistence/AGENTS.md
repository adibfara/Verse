# core/persistence

| File | Contents |
| --- | --- |
| `AppSettings.kt` | `AppSettings`, `StartupBehavior`, `LockBehavior`, editor font-size bounds, `withAdjustedEditorFontSize` and `formatEditorFontSize` |
| `SettingsStore.kt` | `settings: StateFlow<AppSettings>`, `load` / `update` / `rememberSpace` / `forgetSpace` / `flush`, shared `VerseJson`, BOM stripping |
| `WorkspaceState.kt` | `WorkspaceState` (tabs, active tab index, expanded folders, sidebar, follow selection, layout mode, favourites filter), `WorkspaceTab`, `ExplorerLayoutMode` |
| `WorkspaceStore.kt` | reads/writes that session file |
| `WorkspaceSessionRepository.kt` | the in-memory session owner: `session: StateFlow<WorkspaceState>`, `attach` / `detach` / `update` / `flush` |
| `FileStorage.kt` | the only file-access interface here: `read`, `write`, `exists`, `join` |

- Written files: app settings → `<appDataDirectory>/settings.json`. The session → `<space>/.verse/workspace.json`, falling back to `<appDataDirectory>/workspaces/<sanitised-space-id>.json` when the space is ephemeral or its location is not a real directory. A hand-edited file saved by a Windows editor must keep working, hence the BOM strip on both readers.
- Writes are atomic and debounced 500 ms off the main thread; `flush()` cancels the debounce and writes now — call it on shutdown and on space close.
- `WorkspaceSessionRepository` is the single in-memory owner of session state. `WorkspaceViewModel`, `TabsViewModel` and `ExplorerViewModel` all read and write it (via `update`, which skips no-op transforms) and never talk to each other. Put a new piece of session state here, not in a ViewModel.
- `sortOrder` lives in `AppSettings` but is written by `ExplorerViewModel`, not by the settings panel.
- `keyBindingOverrides` lives in `AppSettings` and is pushed into the shared `CommandRegistry` by `app/AppRoot.kt`; a `null` value means the command is unbound.
