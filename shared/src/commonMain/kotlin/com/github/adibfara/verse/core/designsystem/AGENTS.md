# core/designsystem

## Layout

- `theme/` — `VerseTheme.colors` / `.typography` / `.spacing` / `.shapes` / `.sizes`, plus `ThemeDefinition.kt` (theme registry) and `VerseColors.kt` (light/dark).
- `component/VersePrimitives.kt` — `VerseText`, `VerseIcon`, `VerseIconButton`, `VerseListRow`, `VerseFloatingBar`, `VerseDivider`.
- `component/VerseControls.kt` — `VerseTextField`, `VerseButton`, `VerseDialog`, `VerseStepper`, `VerseSplitter`.
- `component/VerseMenu.kt` — `MenuEntry` (`Action` / `Stepper` / `Separator`) and `VerseContextMenu`; a `Stepper` row keeps the menu open while its `-` / `+` are pressed.
- `component/VerseBottomSheet.kt` — `VerseBottomSheet` and `VerseMenuSheetList` (the mobile equivalent of a context menu).
- `component/VerseIcons.kt` — every icon in the app, as hand-built Lucide-style stroke paths.
- `component/PointerInteractions.kt` — `onSecondaryClick`, `onBackgroundSecondaryClick`, `onTertiaryClick`, `itemActions` (expect/actual).

## Rules

- Zero hard-coded colours or dimensions outside `theme/`; read `VerseTheme.*`. That is what makes a new theme free. `HouseStyleTest` fails a colour literal anywhere else.
- `material-icons-extended` does not exist for Compose MP 1.12 (it stopped at 1.7.3). Add a path to `VerseIcons` instead; `ImageVector.Builder.path` needs `import androidx.compose.ui.graphics.vector.path`.
- Never use `PointerButton` or `PointerEvent.button` — desktop-only, breaks the Android build. Use `onSecondaryClick` on the target itself, and `onBackgroundSecondaryClick` on a container whose children handle their own secondary clicks: it skips events a descendant already consumed, so a row menu cannot also open the container menu.
- Menus are hand-built, never Material `DropdownMenu` — that API is used nowhere in the repo. Desktop: `VerseContextMenu`; mobile: `VerseMenuSheetList` inside `VerseBottomSheet`.
- `MenuEntry.Action` rows are plain `clickable` on purpose (no `VerseIconButton`), so opening a menu never steals focus.
