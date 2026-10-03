package com.github.adibfara.mds.feature.editor.ui

import androidx.compose.foundation.background
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
import com.github.adibfara.mds.core.designsystem.component.MdsTextField
import com.github.adibfara.mds.core.designsystem.theme.MdsTheme

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
    val colors = MdsTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.sidebar)
            .padding(horizontal = MdsTheme.spacing.sm, vertical = MdsTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MdsTheme.spacing.xs),
    ) {
        MdsTextField(
            value = state.query,
            onValueChange = onQueryChange,
            placeholder = "Find",
            modifier = Modifier.weight(1f),
            selectAllOnMount = true,
        )
        if (state.showReplace) {
            MdsTextField(
                value = state.replacement,
                onValueChange = onReplacementChange,
                placeholder = "Replace with",
                modifier = Modifier.weight(1f),
            )
        }
        MdsText(
            text = if (state.matchCount == 0) "0/0" else "${state.currentMatch + 1}/${state.matchCount}",
            style = MdsTheme.typography.uiSmall,
            color = colors.textMuted,
        )
        MdsIconButton(
            icon = MdsIcons.ArrowLeft,
            contentDescription = "Previous match",
            onClick = onPrevious,
            enabled = state.matchCount > 0,
        )
        MdsIconButton(
            icon = MdsIcons.ArrowRight,
            contentDescription = "Next match",
            onClick = onNext,
            enabled = state.matchCount > 0,
        )
        if (state.showReplace) {
            MdsIconButton(
                icon = MdsIcons.Check,
                contentDescription = "Replace",
                onClick = onReplace,
                enabled = state.matchCount > 0,
            )
            MdsIconButton(
                icon = MdsIcons.Layers,
                contentDescription = "Replace all",
                onClick = onReplaceAll,
                enabled = state.matchCount > 0,
            )
        }
        MdsIconButton(
            icon = MdsIcons.Close,
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
