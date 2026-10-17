package com.github.adibfara.verse.core.designsystem.component

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput

actual fun Modifier.onSecondaryClick(key: Any?, onClick: (Offset) -> Unit): Modifier =
    pointerInput(key) {
        detectTapGestures(onLongPress = onClick)
    }

actual fun Modifier.onBackgroundSecondaryClick(key: Any?, onClick: (Offset) -> Unit): Modifier =
    onSecondaryClick(key, onClick)

actual fun Modifier.onTertiaryClick(key: Any?, onClick: () -> Unit): Modifier = this

@Composable
actual fun Modifier.itemActions(
    key: Any?,
    interactionSource: MutableInteractionSource,
    onActivate: () -> Unit,
    onContextMenu: (Offset) -> Unit,
): Modifier = combinedClickable(
    interactionSource = interactionSource,
    indication = null,
    onLongClick = { onContextMenu(Offset.Zero) },
    onClick = onActivate,
)
