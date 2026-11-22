# core/platform

`expect` declarations live here. The `actual`s keep the `core.*` package and live under
`shared/src/jvmMain/.../platform`, `shared/src/androidMain/.../platform` or `shared/src/jvmSharedMain/.../platform`.

| expect (file) | jvmMain | androidMain | jvmSharedMain |
| --- | --- | --- | --- |
| `createSystemOpener`, `createAppPaths`, `createLocationPicker`, `createStoragePermission`, `createSpeechRecognition`, `currentPlatformKind`, `PlatformBackHandler`, `PlatformStatusBarStyle`, `PlatformFolderPickerOverlay` (`PlatformServices.kt`) | `PlatformServices.jvm.kt` | `PlatformServices.android.kt`, `FolderPicker.android.kt` | — |
| `createLocationResolver` (`LocationResolver.kt`) | — | — | `LocationResolver.kt` |
| `decodeImageBitmap` (`ImageDecoder.kt`) | `ImageDecoder.jvm.kt` (Skia) | `ImageDecoder.android.kt` (BitmapFactory) | — |
| `ListAwareImeInput`, `ImeBufferMirror` (`ImeInput.kt`) | `ImeInput.jvm.kt` (pass-through) | `ImeInput.android.kt` (wraps the field's `InputConnection` for list editing) | — |
| `platformDispatchers`, `currentTimeMillis` (`core/common`), `createFileStorage` (`core/persistence`), `createSpaceFileSystemFactory` (`core/filesystem`), `onSecondaryClick`/`onTertiaryClick`/`itemActions` (`core/designsystem/component`) | own files | own files | — |

- `jvmSharedMain` is a real source set (`shared/build.gradle.kts`): both `jvmMain` and `androidMain` depend on it, which is why the NIO location resolver and `JvmSpaceFileSystem` need no per-platform actual.
- Desktop: `JvmSpaceFileSystem` (`java.nio` + `WatchService`, atomic temp-file writes). Android: `AndroidSpaceFileSystem(root)`, a `JvmSpaceFileSystem` subclass that polls every 1500 ms and caches one instance per space location. `InMemorySpaceFileSystem` is a test/demo fake, not the Android backing store.
- Desktop entry (`desktopApp/.../main.kt`) owns the undecorated window, `WindowChrome`, the AWT `DropTarget` and `args[0]`.
- Android entry must call `AndroidPlatform.install(context)` / `installActivity(...)` and register the bridges *before* `initializeDependencies()`; `AndroidPlatform.require()` throws otherwise.
- `PlatformFolderPickerOverlay()` must be composed exactly once — it is, in `AppRoot`.

## Gotchas

- `AndroidLocationPicker.pickFile` returns null; only directory picking is implemented on Android.
- `createAppPaths()` is what both `SettingsStore` and `WorkspaceStore` use as the fallback directory when a space is ephemeral.
