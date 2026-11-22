# feature/settings

## Files

- `ui/SettingsPanel.kt` — the entire options UI. No ViewModel, no state of its own.

## Seam

- `SettingsPanel(settings, sortOrder, followEnabled, onSettingsChange, onSortOrderChange, onToggleFollow, onAddSpace, onCloseSpace, onDismiss)`.
- It is a pure projection of `AppSettings` plus two pieces of explorer state. `onSettingsChange` is `workspaceViewModel::updateSettings`; sort order and follow-selection are callbacks into `ExplorerViewModel` and are persisted by that VM, not by `AppSettings`.
- Rendered by `app/AppRoot.kt` when its local `settingsVisible` flag is set. Closing the panel is `onDismiss`, so a new option needs a row here and an `AppSettings` field — no VM change.
- The Keybindings section edits `AppSettings.keyBindingOverrides` (command id → `KeyBinding`, or `null` to unbind) by listing `Commands.all` and each command's effective binding. Recording a shortcut captures the next key event through `KeyEvent.toBinding()` and clears any command that already owned it. `app/AppRoot.kt` pushes those overrides into the shared `CommandRegistry` with `setOverrides`, so a rebind takes effect without re-creating the registry.

No tests.
