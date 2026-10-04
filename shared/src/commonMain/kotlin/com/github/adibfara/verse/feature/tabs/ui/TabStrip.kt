package com.github.adibfara.verse.feature.tabs.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.github.adibfara.verse.core.designsystem.theme.VerseTheme
import com.github.adibfara.verse.core.model.TabId
import com.github.adibfara.verse.feature.tabs.TabsEvent
import com.github.adibfara.verse.feature.tabs.TabsUiState

private class TabBounds {
    private val lefts = mutableMapOf<TabId, Float>()
    private val rights = mutableMapOf<TabId, Float>()

    fun update(id: TabId, left: Float, right: Float) {
        lefts[id] = left
        rights[id] = right
    }

    fun indexAt(order: List<TabId>, pointerX: Float): Int? {
        order.forEachIndexed { index, id ->
            val left = lefts[id] ?: return@forEachIndexed
            val right = rights[id] ?: return@forEachIndexed
            if (pointerX >= left && pointerX <= right) return index
        }
        return null
    }
}

@Composable
fun TabStrip(
    state: TabsUiState,
    onEvent: (TabsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = VerseTheme.colors
    val bounds = remember { TabBounds() }
    val scrollState = rememberScrollState()
    val draggingIndex = remember { mutableListOf<Int>() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(VerseTheme.sizes.tabHeight + VerseTheme.spacing.sm)
            .background(colors.window)
            .horizontalScroll(scrollState)
            .padding(horizontal = VerseTheme.spacing.sm, vertical = VerseTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VerseTheme.spacing.xs),
    ) {
        state.tabs.forEachIndexed { index, tab ->
            TabItem(
                tab = tab,
                isActive = tab.id == state.activeTabId,
                onActivate = { onEvent(TabsEvent.Activate(tab.id)) },
                onClose = { onEvent(TabsEvent.Close(tab.id)) },
                onBoundsChanged = { left, right -> bounds.update(tab.id, left, right) },
                onDragTo = { pointerX ->
                    val from = draggingIndex.lastOrNull() ?: index
                    val target = bounds.indexAt(state.tabs.map { it.id }, pointerX)
                    if (target != null && target != from) {
                        draggingIndex.clear()
                        draggingIndex.add(target)
                        onEvent(TabsEvent.Move(from, target))
                    }
                },
                onDragEnd = { draggingIndex.clear() },
            )
        }
    }
}
