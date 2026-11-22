package com.github.adibfara.verse.feature.editor.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Dp
import com.github.adibfara.verse.core.markdown.MdRange
import kotlinx.coroutines.delay

private const val BreatheDelayMillis = 140L
private const val BreatheInMillis = 420
private const val BreatheOutMillis = 1100
private const val BreathePeakAlpha = 0.11f

internal class RevealHighlight {
    val strength = Animatable(0f)
    var range by mutableStateOf<MdRange?>(null)
        private set

    suspend fun breathe(target: MdRange) {
        range = target
        strength.snapTo(0f)
        delay(BreatheDelayMillis)
        strength.animateTo(1f, tween(BreatheInMillis, easing = FastOutSlowInEasing))
        strength.animateTo(0f, tween(BreatheOutMillis, easing = FastOutSlowInEasing))
        range = null
    }
}

internal fun Modifier.drawRevealHighlight(
    highlight: RevealHighlight,
    layoutOf: () -> TextLayoutResult?,
    toTransformed: (Int) -> Int,
    color: Color,
    padding: Dp,
    radius: Dp,
): Modifier = drawBehind {
    val target = highlight.range ?: return@drawBehind
    val amount = highlight.strength.value
    if (amount <= 0f) return@drawBehind
    val layout = layoutOf() ?: return@drawBehind
    val length = layout.layoutInput.text.length
    val start = toTransformed(target.start).coerceIn(0, length)
    val end = toTransformed(target.end).coerceIn(start, length)
    val firstLine = layout.getLineForOffset(start)
    val lastLine = layout.getLineForOffset(maxOf(start, end - 1))
    val left = (firstLine..lastLine).minOf { layout.getLineLeft(it) }
    val right = (firstLine..lastLine).maxOf { layout.getLineRight(it) }
    val top = layout.getLineTop(firstLine)
    val bottom = layout.getLineBottom(lastLine)
    val pad = padding.toPx()
    drawRoundRect(
        color = color.copy(alpha = BreathePeakAlpha * amount),
        topLeft = Offset(left - pad, top),
        size = Size(right - left + pad * 2, bottom - top),
        cornerRadius = CornerRadius(radius.toPx()),
    )
}
