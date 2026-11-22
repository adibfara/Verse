# feature/viewer

## Files

- `ui/FileViewers.kt` — `ImageViewer`, `UnsupportedFileCard`, `EmptyEditorPlaceholder`.
- `ui/SpaceImageLoader.kt` — `fun interface SpaceImageLoader` and `LocalSpaceImageLoader`.

## Seam

- No ViewModel, no state, no events. `feature/editor/ui/EditorPane.kt` branches on `EditorUiState.kind` and renders exactly one of the three composables in place of the text field.
- `ImageViewer(target)` takes an absolute path string and reads `LocalSpaceImageLoader`; the loader is provided in `app/AppRoot.kt` by `rememberSpaceImageLoader(...)` from `WorkspaceHost.kt`, which resolves through the space filesystem and `decodeImageBitmap`. With no provider it renders nothing, which is how tests and previews end up empty.
- Images never render inline inside the markdown text field. A text field cannot host widgets; a block-based editor would be needed, and that is a deliberate non-goal. Do not treat it as a bug.

No tests.
