package com.github.adibfara.mds.feature.search.ui

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
import com.github.adibfara.mds.core.designsystem.component.MdsIcon
import com.github.adibfara.mds.core.designsystem.component.MdsIconButton
import com.github.adibfara.mds.core.designsystem.component.MdsIcons
import com.github.adibfara.mds.core.designsystem.component.MdsText
import com.github.adibfara.mds.core.designsystem.component.MdsTextField
import com.github.adibfara.mds.core.designsystem.theme.MdsTheme
import com.github.adibfara.mds.feature.search.SearchResult
import com.github.adibfara.mds.feature.search.SearchUiState

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
    val colors = MdsTheme.colors
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
                    .padding(MdsTheme.spacing.md)
                    .fillMaxWidth()
                    .widthIn(max = MdsTheme.sizes.searchPanelWidth)
                    .background(colors.surfaceRaised, MdsTheme.shapes.large)
                    .border(MdsTheme.sizes.borderWidth, colors.border, MdsTheme.shapes.large)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .padding(MdsTheme.spacing.md)
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
                verticalArrangement = Arrangement.spacedBy(MdsTheme.spacing.sm),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MdsText(
                        text = mode.title,
                        modifier = Modifier.weight(1f),
                        style = MdsTheme.typography.uiSmall,
                        color = colors.textMuted,
                    )
                    MdsIconButton(
                        icon = MdsIcons.Close,
                        contentDescription = "Close search",
                        onClick = onDismiss,
                    )
                }
                MdsTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = mode.placeholder,
                    focusRequester = focusRequester,
                )
                if (state.results.isEmpty()) {
                    MdsText(
                        text = if (state.isSearching) "Searching..." else "No matches",
                        style = MdsTheme.typography.uiSmall,
                        color = colors.textMuted,
                        modifier = Modifier.padding(MdsTheme.spacing.sm),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = MdsTheme.sizes.searchResultsHeight),
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
    val colors = MdsTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) colors.surfaceSelected else colors.surfaceRaised,
                MdsTheme.shapes.small,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = MdsTheme.spacing.sm, vertical = MdsTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (result) {
            is SearchResult.File -> Column(modifier = Modifier.weight(1f)) {
                MdsText(
                    text = result.node.path.nameWithoutExtension,
                    style = MdsTheme.typography.uiBody,
                    color = colors.textPrimary,
                    maxLines = 1,
                )
                MdsText(
                    text = result.node.path.value,
                    style = MdsTheme.typography.uiSmall,
                    color = colors.textMuted,
                    maxLines = 1,
                )
            }

            is SearchResult.Match -> Column(modifier = Modifier.weight(1f)) {
                MdsText(
                    text = result.line,
                    style = MdsTheme.typography.uiBody,
                    color = colors.textPrimary,
                    maxLines = 1,
                )
                MdsText(
                    text = "${result.path.value}:${result.lineNumber}",
                    style = MdsTheme.typography.uiSmall,
                    color = colors.textMuted,
                    maxLines = 1,
                )
            }

            is SearchResult.Action -> {
                Column(modifier = Modifier.weight(1f)) {
                    MdsText(
                        text = result.command.title,
                        style = MdsTheme.typography.uiBody,
                        color = colors.textPrimary,
                        maxLines = 1,
                    )
                    MdsText(
                        text = result.command.category.label,
                        style = MdsTheme.typography.uiSmall,
                        color = colors.textMuted,
                        maxLines = 1,
                    )
                }
                result.command.binding?.let { binding ->
                    MdsText(
                        text = binding.label,
                        style = MdsTheme.typography.uiSmall,
                        color = colors.textMuted,
                        maxLines = 1,
                    )
                }
            }

            is SearchResult.CreateNote -> {
                MdsIcon(
                    icon = MdsIcons.FilePlus,
                    contentDescription = null,
                    tint = colors.textMuted,
                )
                MdsText(
                    text = "Create note \"${result.name}\"",
                    style = MdsTheme.typography.uiBody,
                    color = colors.textPrimary,
                    maxLines = 1,
                    modifier = Modifier.padding(start = MdsTheme.spacing.sm).weight(1f),
                )
            }
        }
    }
}
