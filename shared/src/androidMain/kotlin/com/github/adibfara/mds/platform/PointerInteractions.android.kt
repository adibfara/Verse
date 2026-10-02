package com.github.adibfara.mds.core.designsystem.component

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput

actual fun Modifier.onSecondaryClick(key: Any?, onClick: (Offset) -> Unit): Modifier =
    pointerInput(key) {
        detectTapGestures(onLongPress = onClick)
    }

actual fun Modifier.onTertiaryClick(key: Any?, onClick: () -> Unit): Modifier = this
