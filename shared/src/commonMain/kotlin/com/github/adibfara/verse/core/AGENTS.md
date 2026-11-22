# core

Feature-free building blocks. Nothing in `core/` may import a `feature/*` package.

| Package | What it is |
| --- | --- |
| `model` | `FilePath` (value class, `/`-separated, `isRoot`/`parent`/`isDescendantOf`), `FileNode`, `FileKind`, `SortOrder`, `Space`, `OpenTab`, `EditorMode` |
| `filesystem` | `SpaceFileSystem` — the only interface that touches storage — plus `InMemorySpaceFileSystem` (tests/demo) and `uniqueName` |
| `markdown` | parser, marker mask and formatting edits. The root `AGENTS.md` section "Markdown pipeline" is authoritative; read it before touching anything here |
| `designsystem` | theme tokens and hand-built components — see `designsystem/AGENTS.md` |
| `command` | `Commands` catalog, `CommandRegistry`, `ShortcutBridge` |
| `persistence` | settings and per-space session files — see `persistence/AGENTS.md` |
| `platform` | the expect/actual seam — see `platform/AGENTS.md` |
| `common` | `DispatcherProvider`, `Debouncer`, `currentTimeMillis`, flow helpers |

Rules that hold everywhere in `core/`:

- Storage is reached only through `SpaceFileSystem` (or `FileStorage` in `persistence`). Nothing above them sees platform path syntax; `FilePath` stays `/`-separated.
- `expect`/`actual` pairs are declared here and implemented in `shared/src/jvmMain/.../platform`, `shared/src/androidMain/.../platform` and `shared/src/jvmSharedMain/.../platform` — those files keep the `core.*` package despite the `platform` folder.
- `CommandRegistry` is behaviour-free: a `CommandId` only becomes behaviour in `app/AppRoot.kt`'s `handleCommand`. Adding a command means one `Commands` entry plus one branch there.
- `HouseStyleTest` (in `shared/src/jvmTest`) enforces the mechanical rules: no `//` comments in Kotlin, no colour literals outside `designsystem/theme`, `PointerButton` only in `jvmMain`, and no Compose UI or material3 import in a ViewModel, UiState or use case.
