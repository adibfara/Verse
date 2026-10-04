package com.github.adibfara.verse.feature.tabs.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.dp
import com.github.adibfara.verse.core.designsystem.component.VerseIcon
import com.github.adibfara.verse.core.designsystem.component.VerseIcons
import com.github.adibfara.verse.core.designsystem.component.VerseText
import com.github.adibfara.verse.core.designsystem.component.onTertiaryClick
import com.github.adibfara.verse.core.designsystem.theme.VerseTheme
import com.github.adibfara.verse.core.model.OpenTab

@Composable
fun TabItem(
    tab: OpenTab,
    isActive: Boolean,
    onActivate: () -> Unit,
    onClose: () -> Unit,
    onBoundsChanged: (left: Float, right: Float) -> Unit,
    onDragTo: (pointerX: Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val colors = VerseTheme.colors
    var origin by remember { mutableStateOf(Offset.Zero) }

    val background = when {
        isActive -> colors.surfaceRaised
        hovered -> colors.surfaceHover
        else -> Color.Transparent
    }

    Row(
        modifier = modifier
            .height(VerseTheme.sizes.tabHeight)
            .widthIn(min = 110.dp, max = 220.dp)
            .background(background, VerseTheme.shapes.small)
            .border(
                width = VerseTheme.sizes.borderWidth,
                color = if (isActive) colors.accent else Color.Transparent,
                shape = VerseTheme.shapes.small,
            )
            .onGloballyPositioned { coordinates ->
                origin = coordinates.positionInParent()
                onBoundsChanged(origin.x, origin.x + coordinates.size.width)
            }
            .onTertiaryClick(tab.id, onClose)
            .pointerInput(tab.id) {
                detectDragGestures(
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragEnd,
                ) { change, _ ->
                    change.consume()
                    onDragTo(origin.x + change.position.x)
                }
            }
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onActivate)
            .padding(horizontal = VerseTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VerseText(
            text = tab.title,
            modifier = Modifier.weight(1f),
            style = VerseTheme.typography.tabTitle,
            color = if (isActive) colors.textPrimary else colors.textSecondary,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.size(VerseTheme.spacing.xs))
        Box(
            modifier = Modifier.size(16.dp).clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            when {
                tab.isDirty && !hovered -> Box(
                    modifier = Modifier.size(7.dp).background(colors.accent, VerseTheme.shapes.round),
                )

                else -> VerseIcon(
                    icon = VerseIcons.Close,
                    contentDescription = "Close tab",
                    tint = if (hovered) colors.textPrimary else colors.textMuted,
                    size = 12.dp,
                )
            }
        }
    }
}
