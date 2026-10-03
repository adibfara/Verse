package com.github.adibfara.mds.feature.editor.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import com.github.adibfara.mds.core.designsystem.component.MdsContextMenu
import com.github.adibfara.mds.core.designsystem.component.MdsIconButton
import com.github.adibfara.mds.core.designsystem.component.MdsIcons
import com.github.adibfara.mds.core.designsystem.component.MenuEntry

sealed interface EditorOverflowAction {
    data object Find : EditorOverflowAction
    data object Replace : EditorOverflowAction
    data object Rename : EditorOverflowAction
    data object Move : EditorOverflowAction
    data object CopyPath : EditorOverflowAction
    data object Share : EditorOverflowAction
    data object Delete : EditorOverflowAction
}

@Composable
fun EditorOverflowButton(
    onAction: (EditorOverflowAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        MdsIconButton(
            icon = MdsIcons.MoreVertical,
            contentDescription = "More actions",
            onClick = { expanded = true },
        )
        if (expanded) {
            fun act(action: EditorOverflowAction) {
                expanded = false
                onAction(action)
            }
            MdsContextMenu(
                entries = listOf(
                    MenuEntry.Action(
                        label = "Find",
                        icon = MdsIcons.Search,
                        onClick = { act(EditorOverflowAction.Find) },
                    ),
                    MenuEntry.Action(
                        label = "Replace",
                        icon = MdsIcons.Search,
                        onClick = { act(EditorOverflowAction.Replace) },
                    ),
                    MenuEntry.Separator,
                    MenuEntry.Action(
                        label = "Rename...",
                        icon = MdsIcons.Pencil,
                        onClick = { act(EditorOverflowAction.Rename) },
                    ),
                    MenuEntry.Action(
                        label = "Move to...",
                        icon = MdsIcons.MoveTo,
                        onClick = { act(EditorOverflowAction.Move) },
                    ),
                    MenuEntry.Action(
                        label = "Copy path",
                        icon = MdsIcons.Copy,
                        onClick = { act(EditorOverflowAction.CopyPath) },
                    ),
                    MenuEntry.Action(
                        label = "Share",
                        icon = MdsIcons.Share,
                        onClick = { act(EditorOverflowAction.Share) },
                    ),
                    MenuEntry.Separator,
                    MenuEntry.Action(
                        label = "Delete",
                        icon = MdsIcons.Trash,
                        danger = true,
                        onClick = { act(EditorOverflowAction.Delete) },
                    ),
                ),
                offset = IntOffset.Zero,
                onDismiss = { expanded = false },
            )
        }
    }
}
