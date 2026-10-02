package com.github.adibfara.mds.feature.viewer.ui

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
import com.github.adibfara.mds.core.designsystem.component.MdsButton
import com.github.adibfara.mds.core.designsystem.component.MdsIcon
import com.github.adibfara.mds.core.designsystem.component.MdsIcons
import com.github.adibfara.mds.core.designsystem.component.MdsText
import com.github.adibfara.mds.core.designsystem.theme.MdsTheme

@Composable
fun ImageViewer(target: String, modifier: Modifier = Modifier) {
    val loader = LocalSpaceImageLoader.current
    var bitmap by remember(target) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(target, loader) {
        bitmap = loader.load(target)
    }

    Box(
        modifier = modifier.fillMaxSize().padding(MdsTheme.spacing.lg),
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
            MdsText(
                text = "Could not display this image",
                style = MdsTheme.typography.uiBody,
                color = MdsTheme.colors.textMuted,
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
    val colors = MdsTheme.colors
    Column(
        modifier = modifier.fillMaxSize().padding(MdsTheme.spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(MdsTheme.spacing.md, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MdsIcon(
            icon = MdsIcons.FileText,
            contentDescription = null,
            tint = colors.textMuted,
            size = MdsTheme.sizes.iconLarge,
        )
        MdsText(
            text = fileName,
            style = MdsTheme.typography.uiTitle,
            color = colors.textPrimary,
        )
        MdsText(
            text = "MDs does not render this file type yet.",
            style = MdsTheme.typography.uiBody,
            color = colors.textMuted,
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(MdsTheme.spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MdsButton(
                label = "Open in default app",
                onClick = onOpenInDefaultApplication,
                primary = true,
            )
            MdsButton(label = "Show in system explorer", onClick = onRevealInFileManager)
        }
    }
}

@Composable
fun EmptyEditorPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(MdsTheme.spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(MdsTheme.spacing.sm, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MdsText(
            text = "No file open",
            style = MdsTheme.typography.uiTitle,
            color = MdsTheme.colors.textMuted,
        )
        MdsText(
            text = "Pick a file from the sidebar, or press Ctrl+O to search by name.",
            style = MdsTheme.typography.uiBody,
            color = MdsTheme.colors.textMuted,
            modifier = Modifier.padding(horizontal = MdsTheme.spacing.lg),
        )
    }
}
