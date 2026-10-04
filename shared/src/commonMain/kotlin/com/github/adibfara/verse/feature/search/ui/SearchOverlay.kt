package com.github.adibfara.verse.feature.search.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.github.adibfara.verse.core.designsystem.component.VerseIcon
import com.github.adibfara.verse.core.designsystem.component.VerseIconButton
import com.github.adibfara.verse.core.designsystem.component.VerseIcons
import com.github.adibfara.verse.core.designsystem.component.VerseText
import com.github.adibfara.verse.core.designsystem.component.VerseTextField
import com.github.adibfara.verse.core.designsystem.theme.VerseTheme
import com.github.adibfara.verse.feature.search.SearchResult
import com.github.adibfara.verse.feature.search.SearchUiState

@Composable
fun SearchOverlay(
    state: SearchUiState,
    onQueryChange: (String) -> Unit,
    onMoveSelection: (Int) -> Unit,
    onActivate: (SearchResult) -> Unit,
    onActivateSelected: () -> Unit,
    onDismiss: () -> Unit,
) {
    val mode = state.mode ?: return
    val colors = VerseTheme.colors
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()

    LaunchedEffect(mode) { focusRequester.requestFocus() }
    LaunchedEffect(state.selectedIndex) {
        if (state.results.isNotEmpty()) listState.animateScrollToItem(state.selectedIndex)
    }

    Popup(
        alignment = Alignment.TopCenter,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.overlay)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                )
                .safeDrawingPadding(),
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(VerseTheme.spacing.md)
                    .fillMaxWidth()
                    .widthIn(max = VerseTheme.sizes.searchPanelWidth)
                    .background(colors.surfaceRaised, VerseTheme.shapes.large)
                    .border(VerseTheme.sizes.borderWidth, colors.border, VerseTheme.shapes.large)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .padding(VerseTheme.spacing.md)
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when (event.key) {
                            Key.DirectionDown -> {
                                onMoveSelection(1)
                                true
                            }

                            Key.DirectionUp -> {
                                onMoveSelection(-1)
                                true
                            }

                            Key.Enter -> {
                                onActivateSelected()
                                true
                            }

                            Key.Escape -> {
                                onDismiss()
                                true
                            }

                            else -> false
                        }
                    },
                verticalArrangement = Arrangement.spacedBy(VerseTheme.spacing.sm),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    VerseText(
                        text = mode.title,
                        modifier = Modifier.weight(1f),
                        style = VerseTheme.typography.uiSmall,
                        color = colors.textMuted,
                    )
                    VerseIconButton(
                        icon = VerseIcons.Close,
                        contentDescription = "Close search",
                        onClick = onDismiss,
                    )
                }
                VerseTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = mode.placeholder,
                    focusRequester = focusRequester,
                )
                if (state.results.isEmpty()) {
                    VerseText(
                        text = if (state.isSearching) "Searching..." else "No matches",
                        style = VerseTheme.typography.uiSmall,
                        color = colors.textMuted,
                        modifier = Modifier.padding(VerseTheme.spacing.sm),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = VerseTheme.sizes.searchResultsHeight),
                        state = listState,
                    ) {
                        itemsIndexed(state.results) { index, result ->
                            SearchResultRow(
                                result = result,
                                isSelected = index == state.selectedIndex,
                                onClick = { onActivate(result) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    result: SearchResult,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = VerseTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) colors.surfaceSelected else colors.surfaceRaised,
                VerseTheme.shapes.small,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = VerseTheme.spacing.sm, vertical = VerseTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (result) {
            is SearchResult.File -> Column(modifier = Modifier.weight(1f)) {
                VerseText(
                    text = result.node.path.nameWithoutExtension,
                    style = VerseTheme.typography.uiBody,
                    color = colors.textPrimary,
                    maxLines = 1,
                )
                VerseText(
                    text = result.node.path.value,
                    style = VerseTheme.typography.uiSmall,
                    color = colors.textMuted,
                    maxLines = 1,
                )
            }

            is SearchResult.Match -> Column(modifier = Modifier.weight(1f)) {
                VerseText(
                    text = result.line,
                    style = VerseTheme.typography.uiBody,
                    color = colors.textPrimary,
                    maxLines = 1,
                )
                VerseText(
                    text = "${result.path.value}:${result.lineNumber}",
                    style = VerseTheme.typography.uiSmall,
                    color = colors.textMuted,
                    maxLines = 1,
                )
            }

            is SearchResult.Action -> {
                Column(modifier = Modifier.weight(1f)) {
                    VerseText(
                        text = result.command.title,
                        style = VerseTheme.typography.uiBody,
                        color = colors.textPrimary,
                        maxLines = 1,
                    )
                    VerseText(
                        text = result.command.category.label,
                        style = VerseTheme.typography.uiSmall,
                        color = colors.textMuted,
                        maxLines = 1,
                    )
                }
                result.command.binding?.let { binding ->
                    VerseText(
                        text = binding.label,
                        style = VerseTheme.typography.uiSmall,
                        color = colors.textMuted,
                        maxLines = 1,
                    )
                }
            }

            is SearchResult.CreateNote -> {
                VerseIcon(
                    icon = VerseIcons.FilePlus,
                    contentDescription = null,
                    tint = colors.textMuted,
                )
                VerseText(
                    text = "Create note \"${result.name}\"",
                    style = VerseTheme.typography.uiBody,
                    color = colors.textPrimary,
                    maxLines = 1,
                    modifier = Modifier.padding(start = VerseTheme.spacing.sm).weight(1f),
                )
            }
        }
    }
}
