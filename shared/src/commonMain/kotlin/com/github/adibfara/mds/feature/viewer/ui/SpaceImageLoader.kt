package com.github.adibfara.mds.feature.viewer.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap

fun interface SpaceImageLoader {
    suspend fun load(target: String): ImageBitmap?
}

val LocalSpaceImageLoader = staticCompositionLocalOf<SpaceImageLoader> {
    SpaceImageLoader { null }
}
