package com.github.adibfara.verse.feature.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.github.adibfara.verse.core.designsystem.component.VerseIconButton
import com.github.adibfara.verse.core.designsystem.component.VerseIcons
import com.github.adibfara.verse.core.designsystem.component.VerseText
import com.github.adibfara.verse.core.designsystem.component.VerseTextField
import com.github.adibfara.verse.core.designsystem.theme.VerseTheme

data class FindReplaceUiState(
    val query: String = "",
    val replacement: String = "",
    val showReplace: Boolean = false,
    val matchCount: Int = 0,
    val currentMatch: Int = -1,
)

@Composable
fun FindReplaceBar(
    state: FindReplaceUiState,
    onQueryChange: (String) -> Unit,
    onReplacementChange: (String) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onReplace: () -> Unit,
    onReplaceAll: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = VerseTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.sidebar)
            .padding(horizontal = VerseTheme.spacing.sm, vertical = VerseTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VerseTheme.spacing.xs),
    ) {
        VerseTextField(
            value = state.query,
            onValueChange = onQueryChange,
            placeholder = "Find",
            modifier = Modifier.weight(1f),
            selectAllOnMount = true,
        )
        if (state.showReplace) {
            VerseTextField(
                value = state.replacement,
                onValueChange = onReplacementChange,
                placeholder = "Replace with",
                modifier = Modifier.weight(1f),
            )
        }
        VerseText(
            text = if (state.matchCount == 0) "0/0" else "${state.currentMatch + 1}/${state.matchCount}",
            style = VerseTheme.typography.uiSmall,
            color = colors.textMuted,
        )
        VerseIconButton(
            icon = VerseIcons.ArrowLeft,
            contentDescription = "Previous match",
            onClick = onPrevious,
            enabled = state.matchCount > 0,
        )
        VerseIconButton(
            icon = VerseIcons.ArrowRight,
            contentDescription = "Next match",
            onClick = onNext,
            enabled = state.matchCount > 0,
        )
        if (state.showReplace) {
            VerseIconButton(
                icon = VerseIcons.Check,
                contentDescription = "Replace",
                onClick = onReplace,
                enabled = state.matchCount > 0,
            )
            VerseIconButton(
                icon = VerseIcons.Layers,
                contentDescription = "Replace all",
                onClick = onReplaceAll,
                enabled = state.matchCount > 0,
            )
        }
        VerseIconButton(
            icon = VerseIcons.Close,
            contentDescription = "Close find",
            onClick = onClose,
        )
    }
}

internal fun findMatches(text: String, query: String): List<IntRange> {
    if (query.isEmpty()) return emptyList()
    val matches = mutableListOf<IntRange>()
    var index = text.indexOf(query, 0, ignoreCase = true)
    while (index >= 0) {
        matches += index until (index + query.length)
        index = text.indexOf(query, index + 1, ignoreCase = true)
    }
    return matches
}
