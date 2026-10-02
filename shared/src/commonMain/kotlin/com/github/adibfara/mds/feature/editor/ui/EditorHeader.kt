package com.github.adibfara.mds.feature.editor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.github.adibfara.mds.core.designsystem.component.MdsIconButton
import com.github.adibfara.mds.core.designsystem.component.MdsIcons
import com.github.adibfara.mds.core.designsystem.component.MdsText
import com.github.adibfara.mds.core.designsystem.theme.MdsTheme
import com.github.adibfara.mds.core.model.EditorMode

@Composable
fun EditorHeader(
    breadcrumb: List<String>,
    mode: EditorMode,
    isDirty: Boolean,
    onModeChange: (EditorMode) -> Unit,
    onRevealInFileManager: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MdsTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(MdsTheme.sizes.toolbarHeight)
            .padding(horizontal = MdsTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MdsTheme.spacing.xs),
    ) {
        Breadcrumb(segments = breadcrumb, isDirty = isDirty, modifier = Modifier.weight(1f))

        MdsIconButton(
            icon = MdsIcons.Pencil,
            contentDescription = "Live editing mode",
            onClick = { onModeChange(EditorMode.Live) },
            selected = mode == EditorMode.Live,
        )
        MdsIconButton(
            icon = MdsIcons.Code,
            contentDescription = "Raw markdown mode",
            onClick = { onModeChange(EditorMode.Raw) },
            selected = mode == EditorMode.Raw,
        )
        MdsIconButton(
            icon = MdsIcons.Eye,
            contentDescription = "Preview mode",
            onClick = { onModeChange(EditorMode.Preview) },
            selected = mode == EditorMode.Preview,
        )
        MdsIconButton(
            icon = MdsIcons.ExternalLink,
            contentDescription = "Show in system explorer",
            onClick = onRevealInFileManager,
        )
    }
}

@Composable
private fun Breadcrumb(segments: List<String>, isDirty: Boolean, modifier: Modifier = Modifier) {
    val colors = MdsTheme.colors
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MdsTheme.spacing.xs),
    ) {
        segments.forEachIndexed { index, segment ->
            val isLast = index == segments.lastIndex
            MdsText(
                text = segment,
                style = MdsTheme.typography.uiLabel,
                color = if (isLast) colors.textPrimary else colors.textMuted,
                maxLines = 1,
            )
            if (!isLast) {
                MdsText(
                    text = "/",
                    style = MdsTheme.typography.uiLabel,
                    color = colors.textMuted,
                )
            }
        }
        if (isDirty) {
            MdsText(
                text = "•",
                style = MdsTheme.typography.uiLabel,
                color = colors.accent,
            )
        }
    }
}
