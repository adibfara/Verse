package com.github.adibfara.verse.core.designsystem.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset

expect fun Modifier.onSecondaryClick(key: Any?, onClick: (Offset) -> Unit): Modifier

expect fun Modifier.onTertiaryClick(key: Any?, onClick: () -> Unit): Modifier

@Composable
expect fun Modifier.itemActions(
    key: Any?,
    interactionSource: MutableInteractionSource,
    onActivate: () -> Unit,
    onContextMenu: (Offset) -> Unit,
): Modifier
