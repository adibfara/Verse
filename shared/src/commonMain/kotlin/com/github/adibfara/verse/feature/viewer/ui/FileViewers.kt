package com.github.adibfara.verse.feature.viewer.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import com.github.adibfara.verse.core.designsystem.component.VerseButton
import com.github.adibfara.verse.core.designsystem.component.VerseIcon
import com.github.adibfara.verse.core.designsystem.component.VerseIcons
import com.github.adibfara.verse.core.designsystem.component.VerseText
import com.github.adibfara.verse.core.designsystem.theme.VerseTheme

@Composable
fun ImageViewer(target: String, modifier: Modifier = Modifier) {
    val loader = LocalSpaceImageLoader.current
    var bitmap by remember(target) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(target, loader) {
        bitmap = loader.load(target)
    }

    Box(
        modifier = modifier.fillMaxSize().padding(VerseTheme.spacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        val current = bitmap
        if (current != null) {
            Image(
                bitmap = current,
                contentDescription = target,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        } else {
            VerseText(
                text = "Could not display this image",
                style = VerseTheme.typography.uiBody,
                color = VerseTheme.colors.textMuted,
            )
        }
    }
}

@Composable
fun UnsupportedFileCard(
    fileName: String,
    onOpenInDefaultApplication: () -> Unit,
    onRevealInFileManager: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = VerseTheme.colors
    Column(
        modifier = modifier.fillMaxSize().padding(VerseTheme.spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(VerseTheme.spacing.md, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        VerseIcon(
            icon = VerseIcons.FileText,
            contentDescription = null,
            tint = colors.textMuted,
            size = VerseTheme.sizes.iconLarge,
        )
        VerseText(
            text = fileName,
            style = VerseTheme.typography.uiTitle,
            color = colors.textPrimary,
        )
        VerseText(
            text = "MDs does not render this file type yet.",
            style = VerseTheme.typography.uiBody,
            color = colors.textMuted,
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(VerseTheme.spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            VerseButton(
                label = "Open in default app",
                onClick = onOpenInDefaultApplication,
                primary = true,
            )
            VerseButton(label = "Show in system explorer", onClick = onRevealInFileManager)
        }
    }
}

@Composable
fun EmptyEditorPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(VerseTheme.spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(VerseTheme.spacing.sm, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        VerseText(
            text = "No file open",
            style = VerseTheme.typography.uiTitle,
            color = VerseTheme.colors.textMuted,
        )
        VerseText(
            text = "Pick a file from the sidebar, or press Ctrl+O to search by name.",
            style = VerseTheme.typography.uiBody,
            color = VerseTheme.colors.textMuted,
            modifier = Modifier.padding(horizontal = VerseTheme.spacing.lg),
        )
    }
}
