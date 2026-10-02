package com.github.adibfara.mds.feature.editor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.github.adibfara.mds.core.designsystem.component.MdsText
import com.github.adibfara.mds.core.designsystem.theme.MdsTheme
import com.github.adibfara.mds.core.markdown.DocumentStats

@Composable
fun EditorStatusBar(
    stats: DocumentStats,
    isDirty: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MdsTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(MdsTheme.sizes.statusBarHeight)
            .padding(horizontal = MdsTheme.spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MdsTheme.spacing.lg, Alignment.End),
    ) {
        MdsText(
            text = if (isDirty) "Unsaved" else "Saved",
            style = MdsTheme.typography.uiSmall,
            color = if (isDirty) colors.accent else colors.textMuted,
        )
        MdsText(
            text = "${formatCount(stats.words)} words",
            style = MdsTheme.typography.uiSmall,
            color = colors.textMuted,
        )
        MdsText(
            text = "${formatCount(stats.characters)} characters",
            style = MdsTheme.typography.uiSmall,
            color = colors.textMuted,
        )
    }
}

private fun formatCount(value: Int): String {
    val text = value.toString()
    if (text.length <= 3) return text
    return text.reversed()
        .chunked(3)
        .joinToString(",")
        .reversed()
}
