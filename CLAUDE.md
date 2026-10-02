# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

MDs is an Obsidian-like markdown app built on Compose Multiplatform (Kotlin 2.4.20, Compose MP 1.12.1, AGP 9.1.1). Targets are `jvm` (desktop, the primary focus) and `android`. The defining goal is **speed**: instant startup, instant file open, instant tab switching. Nothing may block the main thread.

## Commands

```bash
./gradlew :desktopApp:run                       # run the desktop app
./gradlew :desktopApp:run --args="C:\path\to\note.md"   # single-file / "Open with" mode
./gradlew :desktopApp:hotRun --auto             # desktop with hot reload
./gradlew :androidApp:assembleDebug             # Android APK
./gradlew :desktopApp:packageMsi                # installer (registers .md file association)

./gradlew :shared:jvmTest                       # all shared tests (commonTest runs here)
./gradlew :shared:jvmTest --tests "*MarkerMaskTest*"            # one test class
./gradlew :shared:jvmTest --tests "*MarkerMaskTest.maskHidesEmphasisMarkers"   # one test
./gradlew :shared:testAndroidHostTest           # Android host tests
```

Gradle `-q` hides test stdout. To read `println` output from a test, parse `shared/build/test-results/jvmTest/TEST-*.xml` for its `<system-out>` block. Add `--rerun-tasks` when a test is UP-TO-DATE but you changed non-source inputs.

## House style

- **No comments in any Kotlin file.** This is a deliberate project rule, not an oversight — `grep -rn "^\s*//" --include=*.kt` must stay at 0. Name things so comments aren't needed.
- Single `:shared` module organised by package (`core/*`, `feature/*`), not Gradle feature modules. Keep it that way.
- UI is split into small composables; screens compose components, components take state + lambdas only.
- **Zero hard-coded colors or dimensions outside `core/designsystem/theme`.** This is what makes new themes free. Use `MdsTheme.colors/typography/spacing/shapes/sizes`.

## Architecture

MVVM with a thin domain layer. Nothing in a ViewModel, repository, or use case imports `androidx.compose.ui` or `material3`.

```
UI (composables) -> ViewModel (StateFlow<UiState> + onEvent) -> UseCase/Repository -> SpaceFileSystem / Stores
```

Every ViewModel exposes `val state: StateFlow<XUiState>` and `fun onEvent(event: XEvent)`. ViewModels hold a single `MutableStateFlow<UiState>` and mutate it directly rather than `combine`-ing many flows.

### How the features are wired together

Features do not call each other. `app/ui/WorkspaceHost.kt` (`WorkspaceEffects`) is the single place where cross-feature wiring lives — explorer actions open tabs, editor dirty-changes mark tabs, the active tab drives the editor, restores populate tabs. If you need feature A to affect feature B, add it there, not inside a ViewModel.

State ownership:
- `SpaceRepository` — the currently open space + its `SpaceFileSystem`. Everything reactive hangs off `spaceRepository.current`.
- `WorkspaceSessionRepository` — in-memory `WorkspaceState` (tabs, active index, expanded folders, sidebar) backed by `WorkspaceStore`. Both `TabsViewModel` and `ExplorerViewModel` read/write it; this is how they stay consistent without talking to each other.
- `EditorViewModel` holds `Map<FilePath, EditorSession>`, one VM for all tabs (not one VM per tab). Switching tabs preserves text, caret, mode and dirty state with no re-read and no re-parse.

`WorkspaceViewModel.restores` is a `MutableSharedFlow(replay = 1)` **on purpose**: it emits during `start()`, before `WorkspaceScreen` (and therefore its collector) is composed. Dropping the replay silently breaks session restore.

### The I/O seam

`core/filesystem/SpaceFileSystem` is the only interface that touches storage; nothing else in the app performs I/O. `FilePath` is a value class holding a space-relative `/`-separated path, so no code above this interface sees platform path syntax.

- Desktop: `JvmSpaceFileSystem` (java.nio + `WatchService`), atomic writes via temp file + move.
- Android: `InMemorySpaceFileSystem` seeded with demo content. Real Android storage (SAF document trees) is a deliberate later round — do not assume file paths work there.

Directories are listed **only when expanded**; there is never a recursive walk at startup. `watch()` invalidates just the affected directory.

### Markdown pipeline (the hardest part)

`MarkdownParser` wraps `org.jetbrains:markdown` (GFM flavour) and produces one `MdDocument` that feeds all three editor modes:

- `blocks` — our own `MdBlock`/`MdInline` tree with source offsets, used by read-only Preview.
- `styles` — flat `StyledRange`s (headings, bold, link, …) applied as `SpanStyle`s.
- `markers` — syntax characters to hide (`#`, `**`, backticks, `>`).
- `decorations` — `MaskEdit` *replacements* (`- ` → `• `, `[ ]` → `☐`, `[x]` → `☑`).

`buildMaskedText(text, edits, revealedLine)` in `MarkerMask.kt` applies hides and replacements and builds two `IntArray`s for `OffsetMapping`. **This is the crash-prone core of the app.** Its invariants (monotonic, in-bounds, round-tripping for non-replaced offsets) are locked down by property tests in `MarkerMaskTest` / `MaskReplacementTest` that sweep every caret position over a document corpus. Do not weaken those tests to make a change pass.

Markers on the caret's current line are revealed (Obsidian behaviour), so the transformation is a pure function of `(document, caretLine)` and is cached on that key.

Editor modes: `Live` (default — editable **and** styled; this is the product's whole point), `Raw` (plain monospace, no styling at all, by explicit user requirement), `Preview` (read-only composables with clickable checkboxes and images).

Known limit: a single text field cannot host interactive widgets, so in `Live` mode checkboxes are glyphs rather than clickable, and inline images do not render. Closing that gap requires a block-based editor (one composable per block), which is a deliberate non-goal of the current design.

Task markers are detected by **scanning the source line** (`findTaskMarkerInLine`), not by trusting the parser's `CHECK_BOX` token — the upstream parser classifies `[x]` at the start of some list items as a shortcut reference link instead. Relatedly, link punctuation is only hidden for links that actually have a `LINK_DESTINATION`; reference-style links render as plain text so an undefined `[x]` can never be mangled.

### Commands and shortcuts

`core/command/Commands` is the single catalog; `CommandRegistry` resolves bindings and powers both the shortcut dispatcher and the command palette. `KeyBinding`/`ShortcutKey` are our own types so the catalog stays Compose-free and testable; `ShortcutBridge.kt` maps Compose `KeyEvent` to them. Adding a feature means registering one `Command` and handling its id in `handleCommand` in `AppRoot.kt`.

### Persistence

- `%APPDATA%/MDs/settings.json` — theme, recent spaces, startup behaviour, font size, sort order.
- `<space>/.mds/workspace.json` — tabs, active tab, expanded folders, sidebar width. Living inside the space means the session travels with the folder. Ephemeral/in-memory spaces fall back to the app data dir.

Both readers strip a leading BOM before decoding; a hand-edited file saved by a Windows editor would otherwise silently reset to defaults. Writes are atomic and debounced off the main thread.

### Platform seam

`expect`/`actual` pairs live in `core/platform` + `core/common`, implemented under `shared/src/jvmMain/.../platform` and `shared/src/androidMain/.../platform` (note: those files keep the `core.*` package despite the `platform` folder). Covers dispatchers, app paths, system opener, location picker/resolver, image decoding, back handling, and the filesystem factory.

Desktop entry (`desktopApp/src/main/kotlin/.../main.kt`) owns window concerns: the window is **undecorated** with an in-app title bar, so it passes a `WindowChrome` (minimise/maximise/close + `WindowDraggableArea`) down into `AppRoot`. It also installs the AWT `DropTarget` for drag-and-drop and reads `args[0]` for "Open with".

## Platform gotchas that will cost you time

- **`material-icons-extended` does not exist for Compose MP 1.12** (it stopped at 1.7.3). All icons are hand-built Lucide-style stroke paths in `core/designsystem/component/MdsIcons.kt`. `ImageVector.Builder.path` is a top-level extension needing `import androidx.compose.ui.graphics.vector.path`.
- **`PointerButton` and `PointerEvent.button` are desktop-only** and will break the Android build. Use `Modifier.onSecondaryClick` / `onTertiaryClick` from `core/designsystem/component/PointerInteractions.kt` (right-click on desktop, long-press on Android).
- The editor deliberately uses the **legacy** `BasicTextField(value: TextFieldValue, visualTransformation = …)`. It is the API that supports span styling *and* an `OffsetMapping` for hidden characters; the newer `TextFieldState` API does not. Re-verify before "modernising" it.
- Writing Kotlin through bash heredocs in this environment **eats backslashes** — `'\n'` becomes a literal newline and produces "Incorrect character literal". Use the Write/Edit tools for Kotlin containing escapes.
- When screenshotting the running desktop app on Windows, the capturing process must call `SetProcessDpiAwarenessContext(-4)` first. Without it, a DPI-unaware capture silently returns a clipped, virtualised image and you will chase layout bugs that do not exist.
