package com.github.adibfara.mds.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

@Immutable
class WindowChrome(
    val isMaximized: Boolean,
    val onMinimize: () -> Unit,
    val onToggleMaximize: () -> Unit,
    val onClose: () -> Unit,
    val draggableArea: @Composable (content: @Composable () -> Unit) -> Unit,
)
