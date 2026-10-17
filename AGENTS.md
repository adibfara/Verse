# AGENTS.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Verse is an Obsidian-like markdown app built on Compose Multiplatform (Kotlin 2.4.20, Compose MP 1.12.1, AGP 9.1.1). Targets are `jvm` (desktop, the primary focus) and `android`. The defining goal is **speed**: instant startup, instant file open, instant tab switching. Nothing may block the main thread.

## Where to look first

Every package documents its own seam in a nearby `AGENTS.md` — read that before grepping. They cover the files, the
state/event API, who renders it, and the traps:

`app/AGENTS.md`, `core/AGENTS.md`, `core/designsystem/AGENTS.md`, `core/platform/AGENTS.md`,
`core/persistence/AGENTS.md`, and one per feature:
`feature/{editor,explorer,tabs,space,favorites,references,locks,search,settings,viewer}/AGENTS.md`.

Rules with a mechanical definition are enforced by `HouseStyleTest`, which runs with the normal test task
(`./gradlew :shared:jvmTest`): a `//` comment in any Kotlin file, a hard-coded colour literal outside
`core/designsystem/theme`, `PointerButton` outside `shared/src/jvmMain`, `androidx.compose.material.icons` anywhere, or
a Compose UI / material3 import in a ViewModel, UiState or use case all fail the build.

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
./gradlew :shared:jvmTest --tests "*HouseStyleTest*"            # mechanical house-style rules
./gradlew :shared:testAndroidHostTest           # Android host tests
```

Gradle `-q` hides test stdout. To read `println` output from a test, parse `shared/build/test-results/jvmTest/TEST-*.xml` for its `<system-out>` block. Add `--rerun-tasks` when a test is UP-TO-DATE but you changed non-source inputs.

## Verifying your work

**Do not run the app to check a change.** No `:desktopApp:run`, no clicking through the UI, no screenshotting a live
window, no "let me just make sure it launches". This is a headless session: a desktop window you spawn cannot be seen or
driven, so any such attempt is wasted time and a stuck process, not a verification.

Verify the way the project already does — with tests:

- `./gradlew :shared:jvmTest` for anything in `:shared` (compiles the whole module, so it is also your build check).
- `./gradlew :shared:testAndroidHostTest` when the change touches code the Android target also compiles.
- `./gradlew :androidApp:assembleDebug` only when you actually need the Android build to type-check.

Then **tell the user what to check by hand**: the exact screen, the exact action, and the exact thing they should see.
Be specific and short ("open a space, hover `**bold**` in Live mode, the toolbar should appear after a pause"). If a
change cannot be pinned by a test, say that plainly and describe the manual steps instead of trying to exercise the UI
yourself. Handing verification to the user is the expected workflow here, not a failure on your part.

## House style

- **No comments in any Kotlin file.** This is a deliberate project rule, not an oversight — `grep -rn "^\s*//" --include=*.kt` must stay at 0. Name things so comments aren't needed.
- Single `:shared` module organised by package (`core/*`, `feature/*`), not Gradle feature modules. Keep it that way.
- UI is split into small composables; screens compose components, components take state + lambdas only.
- **Zero hard-coded colors or dimensions outside `core/designsystem/theme`.** This is what makes new themes free. Use `VerseTheme.colors/typography/spacing/shapes/sizes`.

## Architecture

MVVM with a thin domain layer. Nothing in a ViewModel, repository, or use case imports `material3` or Compose UI — the
single exception is `feature/editor`, which needs `androidx.compose.ui.text.TextFieldValue` and `TextRange` to hold a
text field's value. `HouseStyleTest` enforces this.

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
- Android: `AndroidSpaceFileSystem`, a `JvmSpaceFileSystem` subclass that polls for changes every 1500 ms and caches one
  instance per space location. `InMemorySpaceFileSystem` is only a test/demo fake now. Real Android storage (SAF
  document trees) is a deliberate later round — do not assume file paths work there.

Directories are listed **only when expanded**; there is never a recursive walk at startup. `watch()` invalidates just the affected directory.

### Markdown pipeline (the hardest part)

`MarkdownParser` wraps `org.jetbrains:markdown` (GFM flavour) and produces one `MdDocument` that feeds all three editor modes:

- `blocks` — our own `MdBlock`/`MdInline` tree with source offsets. No UI consumes it today (the read-only renderers were deleted when Preview became editable); it stays because it is the parser's structural output, is covered by `MarkdownParserTest`, and is what a future block-based editor would read.
- `styles` — flat `StyledRange`s (headings, bold, link, …) applied as `SpanStyle`s.
- `markers` — syntax characters to hide (`#`, `**`, backticks, `>`).
- `decorations` — `MaskEdit` *replacements*: `- ` → `• `, and a checkbox → `TaskBoxPlaceholder` (U+2003 EM SPACE). A task item's bullet is **hidden rather than bulleted**, so `- [ ] x` renders as one box then `x`, not `• ☐ x`.
- `tasks` / `links` — source ranges of checkbox markers and of inline links with their targets. These are what make the glyphs and links clickable in an editable field; links nested inside an `IMAGE` node are excluded, or every image would also register as a link.

`buildMaskedText(text, edits, revealedLine)` in `MarkerMask.kt` applies hides and replacements and builds two `IntArray`s for `OffsetMapping`. **This is the crash-prone core of the app.** Its invariants (monotonic, in-bounds, round-tripping for non-replaced offsets) are locked down by property tests in `MarkerMaskTest` / `MaskReplacementTest` / `RenderedMaskTest` that sweep every caret position over a document corpus. Do not weaken those tests to make a change pass.

`maskEditsFor(document)` in `MaskEdits.kt` builds the edit list and is where the **empty-construct rule** lives: a marker is only hidden when its construct actually has visible content. `# Hi` renders as `Hi`; backspace to `# ` and the `# ` comes back, so a half-typed construct is never invisible. Content is measured by walking the construct's style range and skipping its own markers.

Editor modes (all three are editable):

- `Preview` — the default, and first in the toolbar. Markers are always hidden, so it reads as rendered markdown you can type into.
- `Live` — same, except markers on the caret's current line are revealed (Obsidian behaviour), so the transformation is a pure function of `(document, caretLine)` and is cached on that key.
- `Raw` — plain monospace, no styling at all, by explicit user requirement.

Parsing is debounced 80 ms, so the document usually lags the text field by a keystroke or two. `MarkdownVisualTransformation` therefore projects the stale document forward through `textRevisionBetween(before, after)` (`TextRevision.kt`): marker edits `shift` (dropped when the edit touches them) and styles `stretch` (grown when the edit lands inside). Without this the whole document's markers flash back on every keystroke. Do not replace it with an identity fallback.

Checkboxes are **drawn, not typed**. `[ ]` and `[x]` are both replaced by the same blank em-space placeholder, and `drawTaskBoxes` paints a rounded box into that glyph's bounding box (centred on the line's baseline, not the line box, or it rides high). Do not go back to `☐`/`☑` glyphs: their advance widths differ, so toggling reflowed the row, and Windows re-resolved the font fallback for the whole run, which visibly changed *other* lines' checkmarks. One placeholder for both states is what makes a toggle cost zero layout — `openAndDoneTasksReserveIdenticalWidth` pins it.

Checkboxes and links **are** clickable, even though the editor is one text field: `MarkdownFields.kt` hit-tests taps against the transformed `TextLayoutResult` on `PointerEventPass.Initial` and consumes the gesture before the field places a caret. A checkbox is matched by the glyph's bounding box, a link by offset range. Everything else falls through to normal caret placement. The same hit-test drives `pointerHoverIcon`, which needs `overrideDescendants = true` to beat the field's own I-beam — and must go back to false off-target, or the whole editor loses the I-beam.

A link target is opened by `rememberLinkOpener` in `WorkspaceHost.kt`: `http(s)`/`mailto` go to `SystemOpener.openUrl`, **not** `openInDefaultApplication` — that one stats the string as a `File` and silently returns for a URL. Anything else resolves against the space (relative to the active note first, then from the root, trying `target` then `target.md`) and opens as a tab. A relative target that resolves to nothing does nothing at all today.

Toggling a checkbox goes through `documentWithTaskToggled`, which rewrites the parsed document in place rather than waiting for the debounced re-parse. `[ ]` → `[x]` is length-preserving, so every other range stays valid and only the task's own styles change. Without it the edit lands *inside* the checkbox's own decoration range, the projection drops that decoration as touched, and the literal `[x]` flashes on screen for 80 ms. `togglingATaskMatchesAFullReparse` asserts the shortcut equals a real parse.

The **hover format toolbar** (`FormatPopup.kt`) is the third thing the hit-test drives. Hovering special text — emphasis, inline code, a link, a heading, a bullet or a checkbox — reports a `PopupAnchor(kind, offset)`, never a snapshot of the popup's contents: everything shown is re-derived from the *current* `MaskedDocument` plus text each composition (`popupStateFor` in `MarkdownFields.kt`). That is what makes the toolbar self-healing — after you click one of its buttons the text has changed, and the popup simply recomputes which formats are active and where its anchor glyph now sits instead of displaying a stale row. Plain text reports nothing, so the toolbar is only ever offered where there is something to toggle. A non-collapsed selection is the fallback anchor, which is how selecting text opens the same toolbar.

Its buttons are tapped through `detectTapGestures`, **not** `Modifier.clickable`: clickable is focusable and requests focus on press, which pulled focus out of the editor on every toggle. Nothing in the toolbar is a focus target, and the `Popup` is `focusable = false`, so the caret never leaves the text field. Do not "fix" these back to `clickable`/`VerseIconButton`.

Its timing is one `LaunchedEffect` keyed on (hovered anchor, popup-hovered): a fresh appearance waits `PopupShowDelayMillis` and fades in, moving to another special span while the toolbar is already up swaps it over **immediately and without a fade** (the fade lives in a `remember`ed `Animatable` that only re-runs when the popup leaves composition), and leaving either the span or the toolbar lingers `PopupLingerMillis` so the pointer can cross the gap between them. The toolbar reports its own hover back up, or that gap would dismiss it.

A format action **parses synchronously** before publishing (`applyFormat` in `EditorViewModel`, which then calls `textChanged(reparse = false)`). This is the one place the 80 ms parse debounce is deliberately bypassed, and it is not an optimisation — it is the only way the published `value.text` and `document.text` can be the *same* text in the same frame. Publish them out of step and the inserted `# `, `**` or `- [ ] ` is text no marker edit covers yet, so it renders literally for one debounce before snapping to the hidden form. In Preview there is no middle ground: raw markers must never be on screen. `EditorFormattingTest` pins this by masking the published state and asserting no marker character survives; every one of its cases fails if the synchronous parse is removed. (This is the same disease `documentWithTaskToggled` cures for checkbox clicks — that one gets away with an in-place document rewrite only because `[ ]` → `[x]` is length-preserving.)

Its edits go through `core/markdown/MarkdownFormatting.kt` — pure `String` → `TextEdit` functions behind one `FormatAction`, so there is no Compose in the formatting logic and it is all unit-testable (`MarkdownFormattingTest`). Every action describes itself as a list of non-overlapping `Splice`s rather than returning a finished string, so one `mapOffset` maps the **caret the user already had** through the edit. Formatting must never impose a selection or move the caret off its character: a collapsed caret stays collapsed, an existing selection keeps covering the same words. Insertions use selection affinity — the selection start is pushed right and its end held left — which is what keeps a wrap from swallowing its own closing marker; a caret inside a prefix that gets deleted lands at the content start. Inline toggles match delimiter **run length** at the boundary rather than a prefix, which is why toggling italic on `**x**` nests to `***x***` instead of eating one of bold's stars. Line markers (`SetLineMarker`) rewrite only the line's prefix, preserving indentation, an ordered marker, and the checked state, so checkbox/bullet/none round-trips without touching content.

Up/Down arrows are also handled there (`verticalCaretKeys`) instead of by Compose. The built-in vertical movement was reported stuck on heading lines — pressing Down did nothing — which is the shape of trouble you get driving it through a lossy `OffsetMapping` and mixed font sizes. Rather than depend on its internals, ours maps the caret into transformed space, picks the neighbouring line off the layout, and maps the hit offset back, keeping a goal column so repeated presses do not drift when crossing a large heading. Shift/Ctrl/Alt arrows still fall through to the default.

Known limit: a text field still cannot host real widgets, so inline images do not render in any mode. Closing that gap requires a block-based editor (one composable per block), which is a deliberate non-goal of the current design.

Task markers are detected by **scanning the source line** (`findTaskMarkerInLine`), not by trusting the parser's `CHECK_BOX` token — the upstream parser classifies `[x]` at the start of some list items as a shortcut reference link instead. Relatedly, link punctuation is only hidden for links that actually have a `LINK_DESTINATION`; reference-style links render as plain text so an undefined `[x]` can never be mangled.

### Commands and shortcuts

`core/command/Commands` is the single catalog; `CommandRegistry` resolves bindings and powers both the shortcut dispatcher and the command palette. `KeyBinding`/`ShortcutKey` are our own types so the catalog stays Compose-free and testable; `ShortcutBridge.kt` maps Compose `KeyEvent` to them. Adding a feature means registering one `Command` and handling its id in `handleCommand` in `AppRoot.kt`.

### Persistence

- `%APPDATA%/Verse/settings.json` — theme, recent spaces, startup behaviour, font size, sort order.
- `<space>/.verse/workspace.json` — tabs, active tab, expanded folders, sidebar width. Living inside the space means the session travels with the folder. Ephemeral/in-memory spaces fall back to the app data dir.

Both readers strip a leading BOM before decoding; a hand-edited file saved by a Windows editor would otherwise silently reset to defaults. Writes are atomic and debounced off the main thread.

### Platform seam

`expect`/`actual` pairs live in `core/platform` + `core/common`, implemented under `shared/src/jvmMain/.../platform` and `shared/src/androidMain/.../platform` (note: those files keep the `core.*` package despite the `platform` folder). Covers dispatchers, app paths, system opener, location picker/resolver, image decoding, back handling, and the filesystem factory.

Desktop entry (`desktopApp/src/main/kotlin/.../main.kt`) owns window concerns: the window is **undecorated** with an in-app title bar, so it passes a `WindowChrome` (minimise/maximise/close + `WindowDraggableArea`) down into `AppRoot`. It also installs the AWT `DropTarget` for drag-and-drop and reads `args[0]` for "Open with".

## Platform gotchas that will cost you time

- **`material-icons-extended` does not exist for Compose MP 1.12** (it stopped at 1.7.3). All icons are hand-built Lucide-style stroke paths in `core/designsystem/component/VerseIcons.kt`. `ImageVector.Builder.path` is a top-level extension needing `import androidx.compose.ui.graphics.vector.path`.
- **`PointerButton` and `PointerEvent.button` are desktop-only** and will break the Android build. Use `Modifier.onSecondaryClick` / `onTertiaryClick` from `core/designsystem/component/PointerInteractions.kt` (right-click on desktop, long-press on Android).
- The editor deliberately uses the **legacy** `BasicTextField(value: TextFieldValue, visualTransformation = …)`. It is the API that supports span styling *and* an `OffsetMapping` for hidden characters; the newer `TextFieldState` API does not. Re-verify before "modernising" it.
- Writing Kotlin through bash heredocs in this environment **eats backslashes** — `'\n'` becomes a literal newline and produces "Incorrect character literal". Use the Write/Edit tools for Kotlin containing escapes.
- When screenshotting the running desktop app on Windows, the capturing process must call
  `SetProcessDpiAwarenessContext(-4)` first. Without it, a DPI-unaware capture silently returns a clipped, virtualised
  image and you will chase layout bugs that do not exist. (This is for captures the user asks for, not a verification
  step — see "Verifying your work".)
