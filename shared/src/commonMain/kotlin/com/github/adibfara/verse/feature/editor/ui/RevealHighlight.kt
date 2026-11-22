package com.github.adibfara.verse.feature.editor.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.text.TextLayoutResult
import com.github.adibfara.verse.core.markdown.MdRange
import kotlinx.coroutines.delay

private const val BreatheDelayMillis = 140L
private const val BreatheInMillis = 420
private const val BreatheOutMillis = 1100
private const val BreathePeak = 0.85f

internal class RevealHighlight {
    val strength = Animatable(0f)
    var range by mutableStateOf<MdRange?>(null)
        private set

    suspend fun breathe(target: MdRange) {
        range = target
        strength.snapTo(0f)
        delay(BreatheDelayMillis)
        strength.animateTo(BreathePeak, tween(BreatheInMillis, easing = FastOutSlowInEasing))
        strength.animateTo(0f, tween(BreatheOutMillis, easing = FastOutSlowInEasing))
        range = null
    }
}

internal fun Modifier.drawRevealHighlight(
    highlight: RevealHighlight,
    layoutOf: () -> TextLayoutResult?,
    toTransformed: (Int) -> Int,
    color: Color,
): Modifier = drawWithContent {
    drawContent()
    val target = highlight.range ?: return@drawWithContent
    val amount = highlight.strength.value
    if (amount <= 0f) return@drawWithContent
    val layout = layoutOf() ?: return@drawWithContent
    val length = layout.layoutInput.text.length
    val start = toTransformed(target.start).coerceIn(0, length)
    val end = toTransformed(target.end).coerceIn(start, length)
    val firstLine = layout.getLineForOffset(start)
    val lastLine = layout.getLineForOffset(maxOf(start, end - 1))
    val bounds = Rect(
        left = (firstLine..lastLine).minOf { layout.getLineLeft(it) },
        top = layout.getLineTop(firstLine),
        right = (firstLine..lastLine).maxOf { layout.getLineRight(it) },
        bottom = layout.getLineBottom(lastLine),
    )
    clipRect(bounds.left, bounds.top, bounds.right, bounds.bottom) {
        drawIntoCanvas { canvas ->
            val paint = Paint().apply {
                colorFilter = ColorFilter.tint(color.copy(alpha = amount), BlendMode.SrcIn)
            }
            canvas.saveLayer(bounds, paint)
            layout.multiParagraph.paint(canvas)
            canvas.restore()
        }
    }
}
