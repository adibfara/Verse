package com.github.adibfara.mds.core.designsystem.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset

expect fun Modifier.onSecondaryClick(key: Any?, onClick: (Offset) -> Unit): Modifier

expect fun Modifier.onTertiaryClick(key: Any?, onClick: () -> Unit): Modifier
