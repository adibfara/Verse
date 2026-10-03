package com.github.adibfara.mds.feature.explorer.ui

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
import com.github.adibfara.mds.core.designsystem.component.MdsButton
import com.github.adibfara.mds.core.designsystem.component.MdsDialog
import com.github.adibfara.mds.core.designsystem.component.MdsText
import com.github.adibfara.mds.core.designsystem.component.MdsTextField
import com.github.adibfara.mds.core.designsystem.theme.MdsTheme
import com.github.adibfara.mds.core.model.FileNode
import com.github.adibfara.mds.core.model.FilePath
import com.github.adibfara.mds.feature.explorer.ExplorerDialog
import com.github.adibfara.mds.feature.explorer.ExplorerEvent

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

    MdsDialog(
        title = title,
        onDismiss = { onEvent(ExplorerEvent.DismissDialog) },
        actions = {
            MdsButton(label = "Cancel", onClick = { onEvent(ExplorerEvent.DismissDialog) })
            MdsButton(
                label = confirmLabel,
                onClick = { onEvent(ExplorerEvent.ConfirmDialog) },
                primary = true,
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MdsTheme.spacing.sm)) {
            MdsText(
                text = label,
                style = MdsTheme.typography.uiLabel,
                color = MdsTheme.colors.textSecondary,
            )
            MdsTextField(
                value = value,
                onValueChange = { onEvent(ExplorerEvent.DialogNameChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                focusRequester = focusRequester,
                onSubmit = { onEvent(ExplorerEvent.ConfirmDialog) },
                selectAllOnMount = true,
            )
            MdsText(
                text = "Location",
                style = MdsTheme.typography.uiLabel,
                color = MdsTheme.colors.textSecondary,
            )
            LocationRow(location = location, onChooseFolder = onChooseFolder)
            errorMessage?.let {
                MdsText(
                    text = it,
                    style = MdsTheme.typography.uiSmall,
                    color = MdsTheme.colors.danger,
                )
            }
        }
    }
}

@Composable
private fun LocationRow(location: FilePath, onChooseFolder: (() -> Unit)?) {
    val colors = MdsTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceHover, MdsTheme.shapes.small)
            .padding(MdsTheme.spacing.sm),
    ) {
        MdsText(
            text = if (location.isRoot) "Space root" else location.value,
            style = MdsTheme.typography.uiBody,
            color = colors.textPrimary,
            maxLines = 1,
        )
    }
    onChooseFolder?.let {
        MdsButton(label = "Choose folder...", onClick = it)
    }
}

@Composable
private fun MoveDialog(dialog: ExplorerDialog.Move, onEvent: (ExplorerEvent) -> Unit) {
    val colors = MdsTheme.colors
    val filtered = remember(dialog.query, dialog.destinations) {
        if (dialog.query.isBlank()) {
            dialog.destinations
        } else {
            dialog.destinations.filter { it.path.value.contains(dialog.query, ignoreCase = true) }
        }
    }

    MdsDialog(
        title = "Move \"${dialog.target.name}\" to",
        onDismiss = { onEvent(ExplorerEvent.DismissDialog) },
        actions = {
            MdsButton(label = "Cancel", onClick = { onEvent(ExplorerEvent.DismissDialog) })
            MdsButton(
                label = "Move",
                onClick = { onEvent(ExplorerEvent.ConfirmDialog) },
                primary = true,
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MdsTheme.spacing.sm)) {
            MdsTextField(
                value = dialog.query,
                onValueChange = { onEvent(ExplorerEvent.MoveQueryChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = "Search folders",
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
                    .border(MdsTheme.sizes.borderWidth, colors.border, MdsTheme.shapes.small),
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
    val colors = MdsTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) colors.surfaceSelected else colors.surfaceRaised)
            .clickable(onClick = onClick)
            .padding(MdsTheme.spacing.sm),
    ) {
        MdsText(
            text = if (folder.path.isRoot) folder.name else folder.path.value,
            style = MdsTheme.typography.uiBody,
            color = colors.textPrimary,
            maxLines = 1,
        )
    }
}

@Composable
private fun DeleteDialog(node: FileNode, onEvent: (ExplorerEvent) -> Unit) {
    MdsDialog(
        title = "Delete \"${node.name}\"?",
        onDismiss = { onEvent(ExplorerEvent.DismissDialog) },
        actions = {
            MdsButton(label = "Cancel", onClick = { onEvent(ExplorerEvent.DismissDialog) })
            MdsButton(
                label = "Delete",
                onClick = { onEvent(ExplorerEvent.ConfirmDialog) },
                primary = true,
                danger = true,
            )
        },
    ) {
        MdsText(
            text = if (node.isDirectory) {
                "This folder and everything inside it will be deleted from disk."
            } else {
                "This file will be deleted from disk."
            },
            style = MdsTheme.typography.uiBody,
            color = MdsTheme.colors.textSecondary,
        )
    }
}
