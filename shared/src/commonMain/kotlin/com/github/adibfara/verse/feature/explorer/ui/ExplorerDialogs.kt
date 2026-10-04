package com.github.adibfara.verse.feature.explorer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import com.github.adibfara.verse.core.designsystem.component.VerseButton
import com.github.adibfara.verse.core.designsystem.component.VerseDialog
import com.github.adibfara.verse.core.designsystem.component.VerseText
import com.github.adibfara.verse.core.designsystem.component.VerseTextField
import com.github.adibfara.verse.core.designsystem.theme.VerseTheme
import com.github.adibfara.verse.core.model.FileNode
import com.github.adibfara.verse.core.model.FilePath
import com.github.adibfara.verse.feature.explorer.ExplorerDialog
import com.github.adibfara.verse.feature.explorer.ExplorerEvent

@Composable
fun ExplorerDialogHost(
    dialog: ExplorerDialog,
    onEvent: (ExplorerEvent) -> Unit,
    onChooseFolder: () -> Unit,
) {
    when (dialog) {
        is ExplorerDialog.NewNote -> NamedDialog(
            title = "New note",
            label = "Name",
            value = dialog.name,
            location = dialog.parent,
            errorMessage = dialog.errorMessage,
            confirmLabel = "Create",
            onEvent = onEvent,
            onChooseFolder = onChooseFolder,
        )

        is ExplorerDialog.NewFolder -> NamedDialog(
            title = "New folder",
            label = "Name",
            value = dialog.name,
            location = dialog.parent,
            errorMessage = dialog.errorMessage,
            confirmLabel = "Create",
            onEvent = onEvent,
            onChooseFolder = onChooseFolder,
        )

        is ExplorerDialog.Rename -> NamedDialog(
            title = "Rename",
            label = "New name",
            value = dialog.name,
            location = dialog.target.path.parent ?: FilePath.Root,
            errorMessage = dialog.errorMessage,
            confirmLabel = "Rename",
            onEvent = onEvent,
            onChooseFolder = null,
        )

        is ExplorerDialog.Move -> MoveDialog(dialog = dialog, onEvent = onEvent)

        is ExplorerDialog.ConfirmDelete -> DeleteDialog(node = dialog.target, onEvent = onEvent)
    }
}

@Composable
private fun NamedDialog(
    title: String,
    label: String,
    value: String,
    location: FilePath,
    errorMessage: String?,
    confirmLabel: String,
    onEvent: (ExplorerEvent) -> Unit,
    onChooseFolder: (() -> Unit)?,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    VerseDialog(
        title = title,
        onDismiss = { onEvent(ExplorerEvent.DismissDialog) },
        actions = {
            VerseButton(label = "Cancel", onClick = { onEvent(ExplorerEvent.DismissDialog) })
            VerseButton(
                label = confirmLabel,
                onClick = { onEvent(ExplorerEvent.ConfirmDialog) },
                primary = true,
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(VerseTheme.spacing.sm)) {
            VerseText(
                text = label,
                style = VerseTheme.typography.uiLabel,
                color = VerseTheme.colors.textSecondary,
            )
            VerseTextField(
                value = value,
                onValueChange = { onEvent(ExplorerEvent.DialogNameChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                focusRequester = focusRequester,
                onSubmit = { onEvent(ExplorerEvent.ConfirmDialog) },
                selectAllOnMount = true,
            )
            VerseText(
                text = "Location",
                style = VerseTheme.typography.uiLabel,
                color = VerseTheme.colors.textSecondary,
            )
            LocationRow(location = location, onChooseFolder = onChooseFolder)
            errorMessage?.let {
                VerseText(
                    text = it,
                    style = VerseTheme.typography.uiSmall,
                    color = VerseTheme.colors.danger,
                )
            }
        }
    }
}

@Composable
private fun LocationRow(location: FilePath, onChooseFolder: (() -> Unit)?) {
    val colors = VerseTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceHover, VerseTheme.shapes.small)
            .padding(VerseTheme.spacing.sm),
    ) {
        VerseText(
            text = if (location.isRoot) "Space root" else location.value,
            style = VerseTheme.typography.uiBody,
            color = colors.textPrimary,
            maxLines = 1,
        )
    }
    onChooseFolder?.let {
        VerseButton(label = "Choose folder...", onClick = it)
    }
}

@Composable
private fun MoveDialog(dialog: ExplorerDialog.Move, onEvent: (ExplorerEvent) -> Unit) {
    val colors = VerseTheme.colors
    val filtered = remember(dialog.query, dialog.destinations) {
        if (dialog.query.isBlank()) {
            dialog.destinations
        } else {
            dialog.destinations.filter { it.path.value.contains(dialog.query, ignoreCase = true) }
        }
    }

    VerseDialog(
        title = "Move \"${dialog.target.name}\" to",
        onDismiss = { onEvent(ExplorerEvent.DismissDialog) },
        actions = {
            VerseButton(label = "Cancel", onClick = { onEvent(ExplorerEvent.DismissDialog) })
            VerseButton(
                label = "Move",
                onClick = { onEvent(ExplorerEvent.ConfirmDialog) },
                primary = true,
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(VerseTheme.spacing.sm)) {
            VerseTextField(
                value = dialog.query,
                onValueChange = { onEvent(ExplorerEvent.MoveQueryChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = "Search folders",
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
                    .border(VerseTheme.sizes.borderWidth, colors.border, VerseTheme.shapes.small),
            ) {
                items(items = filtered, key = { it.path.value }) { folder ->
                    FolderOption(
                        folder = folder,
                        isSelected = folder.path == dialog.selected,
                        onClick = { onEvent(ExplorerEvent.MoveDestinationSelected(folder.path)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FolderOption(folder: FileNode, isSelected: Boolean, onClick: () -> Unit) {
    val colors = VerseTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) colors.surfaceSelected else colors.surfaceRaised)
            .clickable(onClick = onClick)
            .padding(VerseTheme.spacing.sm),
    ) {
        VerseText(
            text = if (folder.path.isRoot) folder.name else folder.path.value,
            style = VerseTheme.typography.uiBody,
            color = colors.textPrimary,
            maxLines = 1,
        )
    }
}

@Composable
private fun DeleteDialog(node: FileNode, onEvent: (ExplorerEvent) -> Unit) {
    VerseDialog(
        title = "Delete \"${node.name}\"?",
        onDismiss = { onEvent(ExplorerEvent.DismissDialog) },
        actions = {
            VerseButton(label = "Cancel", onClick = { onEvent(ExplorerEvent.DismissDialog) })
            VerseButton(
                label = "Delete",
                onClick = { onEvent(ExplorerEvent.ConfirmDialog) },
                primary = true,
                danger = true,
            )
        },
    ) {
        VerseText(
            text = if (node.isDirectory) {
                "This folder and everything inside it will be deleted from disk."
            } else {
                "This file will be deleted from disk."
            },
            style = VerseTheme.typography.uiBody,
            color = VerseTheme.colors.textSecondary,
        )
    }
}
