package com.github.adibfara.mds.feature.editor.ui.preview

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.github.adibfara.mds.core.designsystem.component.MdsText
import com.github.adibfara.mds.core.designsystem.theme.MdsTheme

fun interface MarkdownImageLoader {
    suspend fun load(target: String): ImageBitmap?
}

val LocalMarkdownImageLoader = staticCompositionLocalOf<MarkdownImageLoader> {
    MarkdownImageLoader { null }
}

@Composable
fun MarkdownImage(
    target: String,
    alt: String,
    onOpenImage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val loader = LocalMarkdownImageLoader.current
    var bitmap by remember(target) { mutableStateOf<ImageBitmap?>(null) }
    var failed by remember(target) { mutableStateOf(false) }

    LaunchedEffect(target, loader) {
        val loaded = loader.load(target)
        bitmap = loaded
        failed = loaded == null
    }

    val colors = MdsTheme.colors
    val current = bitmap
    when {
        current != null -> Image(
            bitmap = current,
            contentDescription = alt.ifEmpty { target },
            modifier = modifier
                .fillMaxWidth()
                .heightIn(max = 480.dp)
                .clickable { onOpenImage(target) },
            contentScale = ContentScale.Fit,
            alignment = Alignment.CenterStart,
        )

        failed -> Box(
            modifier = modifier
                .fillMaxWidth()
                .background(colors.surfaceHover, MdsTheme.shapes.medium)
                .border(MdsTheme.sizes.borderWidth, colors.border, MdsTheme.shapes.medium)
                .padding(MdsTheme.spacing.lg),
        ) {
            MdsText(
                text = alt.ifEmpty { target }.ifEmpty { "Image not found" },
                style = MdsTheme.typography.uiSmall,
                color = colors.textMuted,
            )
        }

        else -> Box(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp)
                .background(colors.surfaceHover, MdsTheme.shapes.medium),
        )
    }
}
