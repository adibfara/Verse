package com.github.adibfara.verse.feature.explorer.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntOffset
import com.github.adibfara.verse.core.designsystem.component.VerseBottomSheet
import com.github.adibfara.verse.core.designsystem.component.VerseContextMenu
import com.github.adibfara.verse.core.designsystem.component.VerseMenuSheetList
import com.github.adibfara.verse.core.designsystem.component.VerseIcons
import com.github.adibfara.verse.core.designsystem.component.MenuEntry
import com.github.adibfara.verse.core.model.FileKind
import com.github.adibfara.verse.feature.explorer.ContextMenuTarget
import com.github.adibfara.verse.feature.explorer.ExplorerEvent

fun fileContextMenuEntries(
    target: ContextMenuTarget,
    onEvent: (ExplorerEvent) -> Unit,
    onCopyPath: (String) -> Unit,
    absolutePathOf: (ContextMenuTarget) -> String,
    isFavorite: Boolean = false,
): List<MenuEntry> {
    val node = target.node
    return buildList {
        if (!node.isDirectory) {
            add(
                MenuEntry.Action(
                    label = "Open",
                    icon = VerseIcons.FileText,
                    onClick = { onEvent(ExplorerEvent.Activate(node)) },
                ),
            )
            add(
                MenuEntry.Action(
                    label = if (isFavorite) "Remove from favorites" else "Add to favorites",
                    icon = if (isFavorite) VerseIcons.HeartFilled else VerseIcons.Heart,
                    onClick = { onEvent(ExplorerEvent.ToggleFavorite(node)) },
                ),
            )
        }
        if (node.isDirectory) {
            add(
                MenuEntry.Action(
                    label = "New note here",
                    icon = VerseIcons.FilePlus,
                    onClick = { onEvent(ExplorerEvent.RequestNewNote(node.path)) },
                ),
            )
            add(
                MenuEntry.Action(
                    label = "New folder here",
                    icon = VerseIcons.FolderPlus,
                    onClick = { onEvent(ExplorerEvent.RequestNewFolder(node.path)) },
                ),
            )
        }
        add(MenuEntry.Separator)
        add(
            MenuEntry.Action(
                label = "Make a copy",
                icon = VerseIcons.Copy,
                onClick = { onEvent(ExplorerEvent.Duplicate(node)) },
            ),
        )
        add(
            MenuEntry.Action(
                label = "Move to...",
                icon = VerseIcons.MoveTo,
                onClick = { onEvent(ExplorerEvent.RequestMove(node)) },
            ),
        )
        add(
            MenuEntry.Action(
                label = "Copy path",
                icon = VerseIcons.Copy,
                onClick = { onCopyPath(absolutePathOf(target)) },
            ),
        )
        add(MenuEntry.Separator)
        if (!node.isDirectory && node.kind != FileKind.Other) {
            add(
                MenuEntry.Action(
                    label = "Open in default app",
                    icon = VerseIcons.ExternalLink,
                    onClick = { onEvent(ExplorerEvent.OpenInDefaultApplication(node)) },
                ),
            )
        }
        add(
            MenuEntry.Action(
                label = "Show in system explorer",
                icon = VerseIcons.ExternalLink,
                onClick = { onEvent(ExplorerEvent.RevealInFileManager(node)) },
            ),
        )
        add(MenuEntry.Separator)
        add(
            MenuEntry.Action(
                label = "Rename...",
                icon = VerseIcons.Pencil,
                onClick = { onEvent(ExplorerEvent.RequestRename(node)) },
            ),
        )
        add(
            MenuEntry.Action(
                label = "Delete",
                icon = VerseIcons.Trash,
                danger = true,
                onClick = { onEvent(ExplorerEvent.RequestDelete(node)) },
            ),
        )
    }
}

@Composable
fun FileContextMenu(
    target: ContextMenuTarget,
    onEvent: (ExplorerEvent) -> Unit,
    onCopyPath: (String) -> Unit,
    absolutePathOf: (ContextMenuTarget) -> String,
    isFavorite: Boolean = false,
) {
    VerseContextMenu(
        entries = fileContextMenuEntries(target, onEvent, onCopyPath, absolutePathOf, isFavorite),
        offset = IntOffset(target.x, target.y),
        onDismiss = { onEvent(ExplorerEvent.DismissContextMenu) },
    )
}

@Composable
fun FileActionsBottomSheet(
    target: ContextMenuTarget,
    onEvent: (ExplorerEvent) -> Unit,
    onCopyPath: (String) -> Unit,
    absolutePathOf: (ContextMenuTarget) -> String,
    isFavorite: Boolean = false,
) {
    VerseBottomSheet(onDismiss = { onEvent(ExplorerEvent.DismissContextMenu) }) {
        VerseMenuSheetList(
            entries = fileContextMenuEntries(target, onEvent, onCopyPath, absolutePathOf, isFavorite),
            onEntryClicked = { onEvent(ExplorerEvent.DismissContextMenu) },
        )
    }
}
