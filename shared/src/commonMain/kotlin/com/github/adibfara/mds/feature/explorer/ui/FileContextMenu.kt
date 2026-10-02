package com.github.adibfara.mds.feature.explorer.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntOffset
import com.github.adibfara.mds.core.designsystem.component.MdsContextMenu
import com.github.adibfara.mds.core.designsystem.component.MdsIcons
import com.github.adibfara.mds.core.designsystem.component.MenuEntry
import com.github.adibfara.mds.core.model.FileKind
import com.github.adibfara.mds.feature.explorer.ContextMenuTarget
import com.github.adibfara.mds.feature.explorer.ExplorerEvent

@Composable
fun FileContextMenu(
    target: ContextMenuTarget,
    onEvent: (ExplorerEvent) -> Unit,
    onCopyPath: (String) -> Unit,
    absolutePathOf: (ContextMenuTarget) -> String,
) {
    val node = target.node
    val entries = buildList {
        if (!node.isDirectory) {
            add(
                MenuEntry.Action(
                    label = "Open",
                    icon = MdsIcons.FileText,
                    onClick = { onEvent(ExplorerEvent.Activate(node)) },
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

    MdsContextMenu(
        entries = entries,
        offset = IntOffset(target.x, target.y),
        onDismiss = { onEvent(ExplorerEvent.DismissContextMenu) },
    )
}
