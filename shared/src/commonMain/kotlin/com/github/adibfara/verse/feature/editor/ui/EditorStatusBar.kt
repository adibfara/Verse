package com.github.adibfara.verse.feature.editor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.github.adibfara.verse.core.designsystem.component.VerseText
import com.github.adibfara.verse.core.designsystem.theme.VerseTheme
import com.github.adibfara.verse.core.markdown.DocumentStats

@Composable
fun EditorStatusBar(
    stats: DocumentStats,
    isDirty: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = VerseTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(VerseTheme.sizes.statusBarHeight)
            .padding(horizontal = VerseTheme.spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VerseTheme.spacing.lg, Alignment.End),
    ) {
        VerseText(
            text = if (isDirty) "Unsaved" else "Saved",
            style = VerseTheme.typography.uiSmall,
            color = if (isDirty) colors.accent else colors.textMuted,
        )
        VerseText(
            text = "${formatCount(stats.words)} words",
            style = VerseTheme.typography.uiSmall,
            color = colors.textMuted,
        )
        VerseText(
            text = "${formatCount(stats.characters)} characters",
            style = VerseTheme.typography.uiSmall,
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
