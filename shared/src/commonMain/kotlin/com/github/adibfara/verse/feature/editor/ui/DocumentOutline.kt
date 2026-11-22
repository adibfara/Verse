package com.github.adibfara.verse.feature.editor.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import com.github.adibfara.verse.core.designsystem.component.VerseIcon
import com.github.adibfara.verse.core.designsystem.component.VerseIcons
import com.github.adibfara.verse.core.designsystem.component.VerseText
import com.github.adibfara.verse.core.designsystem.theme.VerseTheme
import com.github.adibfara.verse.core.markdown.OutlineEntry
import kotlinx.coroutines.delay

private const val OutlineLingerMillis = 220L
private const val OutlineMorphMillis = 220
private const val OutlineFadeMillis = 160
private const val ClosedAlpha = 0.85f

@Composable
fun DocumentOutline(
    entries: List<OutlineEntry>,
    onJump: (OutlineEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val source = remember { MutableInteractionSource() }
    val hovering by source.collectIsHoveredAsState()
    var pinned by remember { mutableStateOf(false) }
    var hoverOpen by remember { mutableStateOf(false) }
    LaunchedEffect(hovering) {
        if (hovering) {
            hoverOpen = true
        } else {
            delay(OutlineLingerMillis)
            hoverOpen = false
        }
    }
    val open = pinned || hoverOpen
    val colors = VerseTheme.colors
    val currentJump = rememberUpdatedState(onJump)
    val minLevel = remember(entries) { entries.minOf { it.level } }
    val width by animateDpAsState(
        targetValue = if (open) VerseTheme.sizes.outlinePanelWidth else VerseTheme.sizes.iconButton,
        animationSpec = tween(durationMillis = OutlineMorphMillis, easing = FastOutSlowInEasing),
    )
    val background by animateColorAsState(
        targetValue = when {
            open -> colors.surfaceRaised
            hovering -> colors.surfaceHover
            else -> colors.surfaceRaised.copy(alpha = ClosedAlpha)
        },
        animationSpec = tween(durationMillis = OutlineMorphMillis),
    )
    val borderColor by animateColorAsState(
        targetValue = if (open) colors.border else Color.Transparent,
        animationSpec = tween(durationMillis = OutlineMorphMillis),
    )
    val labelAlpha by animateFloatAsState(
        targetValue = if (open) 1f else 0f,
        animationSpec = tween(durationMillis = OutlineFadeMillis),
    )
    val shape = VerseTheme.shapes.medium

    Column(
        modifier = modifier
            .width(width)
            .clip(shape)
            .background(background, shape)
            .border(VerseTheme.sizes.borderWidth, borderColor, shape)
            .hoverable(source),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(VerseTheme.sizes.iconButton)
                .pointerInput(Unit) { detectTapGestures(onTap = { pinned = !pinned }) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VerseText(
                text = "Overview",
                style = VerseTheme.typography.uiLabel,
                color = colors.textSecondary,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = VerseTheme.spacing.md)
                    .graphicsLayer { alpha = labelAlpha },
            )
            Box(
                modifier = Modifier.size(VerseTheme.sizes.iconButton),
                contentAlignment = Alignment.Center,
            ) {
                VerseIcon(
                    icon = VerseIcons.Outline,
                    contentDescription = "Document overview",
                    tint = if (open) colors.accent else colors.textSecondary,
                    size = VerseTheme.sizes.iconSmall,
                )
            }
        }
        AnimatedVisibility(
            visible = open,
            enter = expandVertically(
                animationSpec = tween(durationMillis = OutlineMorphMillis, easing = FastOutSlowInEasing),
                expandFrom = Alignment.Top,
            ) + fadeIn(tween(durationMillis = OutlineFadeMillis, delayMillis = OutlineFadeMillis / 2)),
            exit = shrinkVertically(
                animationSpec = tween(durationMillis = OutlineMorphMillis, easing = FastOutSlowInEasing),
                shrinkTowards = Alignment.Top,
            ) + fadeOut(tween(durationMillis = OutlineFadeMillis)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = VerseTheme.sizes.outlinePanelMaxHeight)
                    .verticalScroll(rememberScrollState())
                    .padding(start = VerseTheme.spacing.xxs, end = VerseTheme.spacing.xxs, bottom = VerseTheme.spacing.xxs),
            ) {
                entries.forEach { entry ->
                    OutlineRow(
                        entry = entry,
                        indentLevels = entry.level - minLevel,
                        onClick = {
                            pinned = false
                            hoverOpen = false
                            currentJump.value(entry)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun OutlineRow(entry: OutlineEntry, indentLevels: Int, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val colors = VerseTheme.colors
    val top = indentLevels == 0
    VerseText(
        text = entry.title,
        style = VerseTheme.typography.uiSmall.copy(
            fontWeight = if (top) FontWeight.SemiBold else FontWeight.Normal,
        ),
        color = if (top) colors.textPrimary else colors.textSecondary,
        maxLines = 1,
        modifier = Modifier
            .fillMaxWidth()
            .background(if (hovered) colors.surfaceHover else Color.Transparent, VerseTheme.shapes.small)
            .hoverable(source)
            .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) }
            .padding(
                start = VerseTheme.spacing.sm + VerseTheme.sizes.indentStep * indentLevels,
                end = VerseTheme.spacing.sm,
                top = VerseTheme.spacing.xs,
                bottom = VerseTheme.spacing.xs,
            ),
    )
}
