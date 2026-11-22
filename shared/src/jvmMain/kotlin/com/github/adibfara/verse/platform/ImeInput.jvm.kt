package com.github.adibfara.verse.core.platform

import androidx.compose.runtime.Composable

@Composable
actual fun ListAwareImeInput(
    mirror: ImeBufferMirror,
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    content()
}
