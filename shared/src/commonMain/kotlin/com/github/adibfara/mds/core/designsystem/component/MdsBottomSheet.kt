package com.github.adibfara.mds.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.adibfara.mds.core.designsystem.theme.MdsTheme

@Composable
fun MdsBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = MdsTheme.colors
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.overlay)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .background(colors.surfaceRaised, MdsTheme.shapes.large)
                    .padding(vertical = MdsTheme.spacing.md),
            ) {
                content()
            }
        }
    }
}

@Composable
fun MdsMenuSheetList(
    entries: List<MenuEntry>,
    onEntryClicked: () -> Unit,
) {
    entries.forEach { entry ->
        when (entry) {
            is MenuEntry.Separator -> MenuSeparator()
            is MenuEntry.Action -> MenuActionRow(entry = entry, onDismiss = onEntryClicked)
        }
    }
}
