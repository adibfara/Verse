package com.github.adibfara.mds.feature.explorer.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntOffset
import com.github.adibfara.mds.core.designsystem.component.MdsBottomSheet
import com.github.adibfara.mds.core.designsystem.component.MdsContextMenu
import com.github.adibfara.mds.core.designsystem.component.MdsMenuSheetList
import com.github.adibfara.mds.core.designsystem.component.MdsIcons
import com.github.adibfara.mds.core.designsystem.component.MenuEntry
import com.github.adibfara.mds.core.model.FileKind
import com.github.adibfara.mds.feature.explorer.ContextMenuTarget
import com.github.adibfara.mds.feature.explorer.ExplorerEvent

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
                    icon = MdsIcons.FileText,
                    onClick = { onEvent(ExplorerEvent.Activate(node)) },
                ),
            )
            add(
                MenuEntry.Action(
                    label = if (isFavorite) "Remove from favorites" else "Add to favorites",
                    icon = if (isFavorite) MdsIcons.HeartFilled else MdsIcons.Heart,
                    onClick = { onEvent(ExplorerEvent.ToggleFavorite(node)) },
                ),
            )
        }
        if (node.isDirectory) {
            add(
                MenuEntry.Action(
                    label = "New note here",
                    icon = MdsIcons.FilePlus,
                    onClick = { onEvent(ExplorerEvent.RequestNewNote(node.path)) },
                ),
            )
            add(
                MenuEntry.Action(
                    label = "New folder here",
                    icon = MdsIcons.FolderPlus,
                    onClick = { onEvent(ExplorerEvent.RequestNewFolder(node.path)) },
                ),
            )
        }
        add(MenuEntry.Separator)
        add(
            MenuEntry.Action(
                label = "Make a copy",
                icon = MdsIcons.Copy,
                onClick = { onEvent(ExplorerEvent.Duplicate(node)) },
            ),
        )
        add(
            MenuEntry.Action(
                label = "Move to...",
                icon = MdsIcons.MoveTo,
                onClick = { onEvent(ExplorerEvent.RequestMove(node)) },
            ),
        )
        add(
            MenuEntry.Action(
                label = "Copy path",
                icon = MdsIcons.Copy,
                onClick = { onCopyPath(absolutePathOf(target)) },
            ),
        )
        add(MenuEntry.Separator)
        if (!node.isDirectory && node.kind != FileKind.Other) {
            add(
                MenuEntry.Action(
                    label = "Open in default app",
                    icon = MdsIcons.ExternalLink,
                    onClick = { onEvent(ExplorerEvent.OpenInDefaultApplication(node)) },
                ),
            )
        }
        add(
            MenuEntry.Action(
                label = "Show in system explorer",
                icon = MdsIcons.ExternalLink,
                onClick = { onEvent(ExplorerEvent.RevealInFileManager(node)) },
            ),
        )
        add(MenuEntry.Separator)
        add(
            MenuEntry.Action(
                label = "Rename...",
                icon = MdsIcons.Pencil,
                onClick = { onEvent(ExplorerEvent.RequestRename(node)) },
            ),
        )
        add(
            MenuEntry.Action(
                label = "Delete",
                icon = MdsIcons.Trash,
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
    MdsContextMenu(
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
    MdsBottomSheet(onDismiss = { onEvent(ExplorerEvent.DismissContextMenu) }) {
        MdsMenuSheetList(
            entries = fileContextMenuEntries(target, onEvent, onCopyPath, absolutePathOf, isFavorite),
            onEntryClicked = { onEvent(ExplorerEvent.DismissContextMenu) },
        )
    }
}
