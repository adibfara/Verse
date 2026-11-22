package com.github.adibfara.verse.feature.editor.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import com.github.adibfara.verse.core.designsystem.component.VerseContextMenu
import com.github.adibfara.verse.core.designsystem.component.VerseIconButton
import com.github.adibfara.verse.core.designsystem.component.VerseIcons
import com.github.adibfara.verse.core.designsystem.component.MenuEntry
import com.github.adibfara.verse.core.persistence.EditorFontSizeStep
import com.github.adibfara.verse.core.persistence.MaxEditorFontSize
import com.github.adibfara.verse.core.persistence.MinEditorFontSize
import com.github.adibfara.verse.core.persistence.formatEditorFontSize

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
    leadingEntries: List<MenuEntry> = emptyList(),
    alignment: Alignment = Alignment.TopStart,
    fontSize: Float? = null,
    onAdjustFontSize: ((Float) -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        VerseIconButton(
            icon = VerseIcons.MoreVertical,
            contentDescription = "More actions",
            onClick = { expanded = true },
        )
        if (expanded) {
            fun act(action: EditorOverflowAction) {
                expanded = false
                onAction(action)
            }
            val trailingEntries = listOf(
                MenuEntry.Action(
                    label = "Find",
                    icon = VerseIcons.Search,
                    onClick = { act(EditorOverflowAction.Find) },
                ),
                MenuEntry.Action(
                    label = "Replace",
                    icon = VerseIcons.Search,
                    onClick = { act(EditorOverflowAction.Replace) },
                ),
                MenuEntry.Separator,
                MenuEntry.Action(
                    label = "Rename...",
                    icon = VerseIcons.Pencil,
                    onClick = { act(EditorOverflowAction.Rename) },
                ),
                MenuEntry.Action(
                    label = "Move to...",
                    icon = VerseIcons.MoveTo,
                    onClick = { act(EditorOverflowAction.Move) },
                ),
                MenuEntry.Action(
                    label = "Copy path",
                    icon = VerseIcons.Copy,
                    onClick = { act(EditorOverflowAction.CopyPath) },
                ),
                MenuEntry.Action(
                    label = "Share",
                    icon = VerseIcons.Share,
                    onClick = { act(EditorOverflowAction.Share) },
                ),
                MenuEntry.Separator,
                MenuEntry.Action(
                    label = "Delete",
                    icon = VerseIcons.Trash,
                    danger = true,
                    onClick = { act(EditorOverflowAction.Delete) },
                ),
            )
            val entries = buildList {
                if (fontSize != null && onAdjustFontSize != null) {
                    add(
                        MenuEntry.Stepper(
                            label = "Font size",
                            value = formatEditorFontSize(fontSize),
                            onDecrease = { onAdjustFontSize(-EditorFontSizeStep) },
                            onIncrease = { onAdjustFontSize(EditorFontSizeStep) },
                            decreaseEnabled = fontSize > MinEditorFontSize,
                            increaseEnabled = fontSize < MaxEditorFontSize,
                        ),
                    )
                    add(MenuEntry.Separator)
                }
                if (leadingEntries.isNotEmpty()) {
                    addAll(leadingEntries)
                    add(MenuEntry.Separator)
                }
                addAll(trailingEntries)
            }
            VerseContextMenu(
                entries = entries,
                offset = IntOffset.Zero,
                onDismiss = { expanded = false },
                alignment = alignment,
            )
        }
    }
}
