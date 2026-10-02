package com.github.adibfara.mds.feature.editor.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.github.adibfara.mds.core.designsystem.theme.MdsTheme
import com.github.adibfara.mds.core.markdown.FormatAction
import com.github.adibfara.mds.core.markdown.LineMarkerKind
import com.github.adibfara.mds.core.markdown.MaskedDocument
import com.github.adibfara.mds.core.markdown.MdDocument
import com.github.adibfara.mds.core.markdown.MdLink
import com.github.adibfara.mds.core.markdown.MdRange
import com.github.adibfara.mds.core.markdown.MdTask
import com.github.adibfara.mds.core.markdown.SpecialSpanKind
import com.github.adibfara.mds.core.markdown.StyleKind
import com.github.adibfara.mds.core.markdown.TaskBoxPlaceholder
import com.github.adibfara.mds.core.markdown.headingLevelAt
import com.github.adibfara.mds.core.markdown.inlineFormatsCovering
import com.github.adibfara.mds.core.markdown.lineMarkerKindAt
import com.github.adibfara.mds.core.markdown.lineRangeAt
import com.github.adibfara.mds.core.markdown.maskDocument
import com.github.adibfara.mds.core.markdown.maskEditsFor
import com.github.adibfara.mds.core.markdown.specialSpanAt
import kotlinx.coroutines.delay

private const val TapPadding = 3f
private const val PopupLingerMillis = 700L

@Composable
fun RenderedMarkdownField(
    value: TextFieldValue,
    document: MdDocument,
    onValueChange: (TextFieldValue) -> Unit,
    onToggleTask: (MdRange, Boolean) -> Unit,
    onOpenLink: (String) -> Unit,
    onFormat: (FormatAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    MaskedMarkdownField(
        value = value,
        document = document,
        revealedLine = null,
        onValueChange = onValueChange,
        onToggleTask = onToggleTask,
        onOpenLink = onOpenLink,
        onFormat = onFormat,
        modifier = modifier,
    )
}

@Composable
fun LiveMarkdownField(
    value: TextFieldValue,
    document: MdDocument,
    onValueChange: (TextFieldValue) -> Unit,
    onToggleTask: (MdRange, Boolean) -> Unit,
    onOpenLink: (String) -> Unit,
    onFormat: (FormatAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val revealedLine = remember(value.text, value.selection.start) {
        lineRangeAt(value.text, value.selection.start)
    }
    MaskedMarkdownField(
        value = value,
        document = document,
        revealedLine = revealedLine,
        onValueChange = onValueChange,
        onToggleTask = onToggleTask,
        onOpenLink = onOpenLink,
        onFormat = onFormat,
        modifier = modifier,
    )
}

@Composable
private fun MaskedMarkdownField(
    value: TextFieldValue,
    document: MdDocument,
    revealedLine: MdRange?,
    onValueChange: (TextFieldValue) -> Unit,
    onToggleTask: (MdRange, Boolean) -> Unit,
    onOpenLink: (String) -> Unit,
    onFormat: (FormatAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val styles = rememberMarkdownStyles()
    val colors = MdsTheme.colors
    val edits = remember(document) { maskEditsFor(document) }
    val masked = remember(document, edits, value.text, revealedLine) {
        maskDocument(document, edits, value.text, revealedLine)
    }
    val transformation = remember(masked, styles) {
        MarkdownVisualTransformation(masked = masked, styles = styles)
    }

    val layout = remember { mutableStateOf<TextLayoutResult?>(null) }
    val column = remember { CaretColumn() }
    val currentMasked = rememberUpdatedState(masked)
    val currentToggleTask = rememberUpdatedState(onToggleTask)
    val currentOpenLink = rememberUpdatedState(onOpenLink)
    val overTarget = remember { mutableStateOf(false) }
    val hoveredAnchor = remember { mutableStateOf<PopupAnchor?>(null) }
    val popupHovered = remember { mutableStateOf(false) }
    val lingeringAnchor = remember { mutableStateOf<PopupAnchor?>(null) }
    val taps = remember(column) {
        Modifier
            .markdownTaps(
                maskedOf = { currentMasked.value },
                layoutOf = { layout.value },
                column = column,
                onToggleTask = { range, checked -> currentToggleTask.value(range, checked) },
                onOpenLink = { target -> currentOpenLink.value(target) },
            )
            .markdownHover(
                maskedOf = { currentMasked.value },
                layoutOf = { layout.value },
                onOverTarget = { overTarget.value = it },
                onAnchor = { hoveredAnchor.value = it },
            )
    }

    LaunchedEffect(hoveredAnchor.value, popupHovered.value) {
        val anchor = hoveredAnchor.value
        if (anchor != null) {
            lingeringAnchor.value = anchor
            return@LaunchedEffect
        }
        if (popupHovered.value) return@LaunchedEffect
        delay(PopupLingerMillis)
        lingeringAnchor.value = null
    }

    val popup = popupStateFor(
        anchor = lingeringAnchor.value,
        masked = masked,
        value = value,
        layout = layout.value,
    )

    Box(modifier = modifier) {
        BasicTextField(
            value = value,
            onValueChange = { next ->
                if (next.text != value.text) column.x = null
                onValueChange(next)
            },
            modifier = Modifier
                .fillMaxWidth()
                .then(taps)
                .pointerHoverIcon(
                    icon = if (overTarget.value) PointerIcon.Hand else PointerIcon.Default,
                    overrideDescendants = overTarget.value,
                )
                .drawTaskBoxes(
                    maskedOf = { masked },
                    layoutOf = { layout.value },
                    outline = colors.textSecondary,
                    fill = colors.accent,
                    check = colors.textOnAccent,
                )
                .verticalCaretKeys(
                    value = value,
                    masked = masked,
                    layoutOf = { layout.value },
                    column = column,
                    onValueChange = onValueChange,
                ),
            textStyle = MdsTheme.typography.editorBody.copy(color = colors.textPrimary),
            cursorBrush = SolidColor(colors.caret),
            visualTransformation = transformation,
            onTextLayout = { layout.value = it },
        )

        popup?.let { state ->
            FormatPopup(
                state = state,
                onHoverChange = { popupHovered.value = it },
                onAction = onFormat,
            )
        }
    }
}

private data class PopupAnchor(val kind: FormatPopupKind, val offset: Int)

private fun popupStateFor(
    anchor: PopupAnchor?,
    masked: MaskedDocument,
    value: TextFieldValue,
    layout: TextLayoutResult?,
): FormatPopupState? {
    if (layout == null) return null
    val text = masked.mask.originalText
    if (text != value.text) return null
    anchor?.let { stateForAnchor(it, masked, text, layout) }?.let { return it }
    return selectionPopupState(masked, value, layout)
}

private fun stateForAnchor(
    anchor: PopupAnchor,
    masked: MaskedDocument,
    text: String,
    layout: TextLayoutResult,
): FormatPopupState? {
    val length = layout.layoutInput.text.length
    return when (anchor.kind) {
        FormatPopupKind.LineMarker -> {
            val line = lineRangeAt(text, anchor.offset)
            val glyph = markerGlyphRange(masked, line) ?: return null
            val box = boundingBoxOf(layout, masked.transformedRangeOf(glyph), length) ?: return null
            FormatPopupState(
                kind = FormatPopupKind.LineMarker,
                range = line,
                anchor = box,
                formats = emptySet(),
                headingLevel = 0,
                lineMarker = lineMarkerKindAt(text, anchor.offset),
            )
        }

        FormatPopupKind.Inline -> {
            val span = specialSpanAt(masked.styles, text, anchor.offset) ?: return null
            if (span.kind != SpecialSpanKind.Inline) return null
            inlinePopupState(masked, span.range, layout)
        }
    }
}

private fun selectionPopupState(
    masked: MaskedDocument,
    value: TextFieldValue,
    layout: TextLayoutResult,
): FormatPopupState? {
    val selection = value.selection
    if (selection.collapsed) return null
    return inlinePopupState(masked, MdRange(selection.min, selection.max), layout)
}

private fun inlinePopupState(
    masked: MaskedDocument,
    range: MdRange,
    layout: TextLayoutResult,
): FormatPopupState? {
    val length = layout.layoutInput.text.length
    val box = boundingBoxOf(layout, masked.transformedRangeOf(range), length) ?: return null
    return FormatPopupState(
        kind = FormatPopupKind.Inline,
        range = range,
        anchor = box,
        formats = inlineFormatsCovering(masked.styles, range),
        headingLevel = headingLevelAt(masked.styles, range.start),
        lineMarker = LineMarkerKind.None,
    )
}

private fun markerGlyphRange(masked: MaskedDocument, line: MdRange): MdRange? {
    masked.tasks
        .firstOrNull { it.range.start >= line.start && it.range.end <= line.end }
        ?.let { return it.range }
    return masked.styles
        .firstOrNull {
            it.kind == StyleKind.ListMarker &&
                it.range.start >= line.start &&
                it.range.end <= line.end
        }
        ?.range
}

@Composable
fun RawMarkdownField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MdsTheme.colors
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = MdsTheme.typography.editorRaw.copy(color = colors.textPrimary),
        cursorBrush = SolidColor(colors.caret),
    )
}

private class CaretColumn {
    var x: Float? = null
}

private sealed interface TapTarget {
    data class Task(val task: MdTask) : TapTarget
    data class Link(val link: MdLink) : TapTarget
}

private fun Modifier.markdownTaps(
    maskedOf: () -> MaskedDocument,
    layoutOf: () -> TextLayoutResult?,
    column: CaretColumn,
    onToggleTask: (MdRange, Boolean) -> Unit,
    onOpenLink: (String) -> Unit,
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        column.x = null
        val layout = layoutOf() ?: return@awaitEachGesture
        val masked = maskedOf()
        val target = targetAt(masked, layout, down.position) ?: return@awaitEachGesture
        down.consume()
        val up = waitForUpOrCancellation(PointerEventPass.Initial) ?: return@awaitEachGesture
        up.consume()
        if (targetAt(masked, layout, up.position) != target) return@awaitEachGesture
        when (target) {
            is TapTarget.Task -> onToggleTask(target.task.range, !target.task.checked)
            is TapTarget.Link -> onOpenLink(target.link.target)
        }
    }
}

private fun Modifier.markdownHover(
    maskedOf: () -> MaskedDocument,
    layoutOf: () -> TextLayoutResult?,
    onOverTarget: (Boolean) -> Unit,
    onAnchor: (PopupAnchor?) -> Unit,
): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.type == PointerEventType.Exit) {
                onOverTarget(false)
                onAnchor(null)
                continue
            }
            val position = event.changes.lastOrNull()?.position
            val layout = layoutOf()
            if (position == null || layout == null) {
                onOverTarget(false)
                onAnchor(null)
                continue
            }
            val masked = maskedOf()
            onOverTarget(targetAt(masked, layout, position) != null)
            onAnchor(popupAnchorAt(masked, layout, position))
        }
    }
}

private fun popupAnchorAt(
    masked: MaskedDocument,
    layout: TextLayoutResult,
    position: Offset,
): PopupAnchor? {
    val length = layout.layoutInput.text.length
    if (length == 0) return null
    if (position.y < 0f || position.y > layout.size.height) return null
    val line = layout.getLineForVerticalPosition(position.y)
    if (position.x < layout.getLineLeft(line) - TapPadding) return null
    if (position.x > layout.getLineRight(line) + TapPadding) return null

    masked.tasks.forEach { task ->
        val box = boundingBoxOf(layout, masked.transformedRangeOf(task.range), length)
        if (box != null && box.inflate(TapPadding).contains(position)) {
            return PopupAnchor(FormatPopupKind.LineMarker, task.range.start)
        }
    }

    val text = masked.mask.originalText
    val offset = masked.mask.transformedToOriginal(layout.getOffsetForPosition(position))
    val span = specialSpanAt(masked.styles, text, offset) ?: return null
    return when (span.kind) {
        SpecialSpanKind.LineMarker -> PopupAnchor(FormatPopupKind.LineMarker, span.range.start)
        SpecialSpanKind.Inline -> PopupAnchor(FormatPopupKind.Inline, span.range.start)
    }
}

private fun targetAt(
    masked: MaskedDocument,
    layout: TextLayoutResult,
    position: Offset,
): TapTarget? {
    val length = layout.layoutInput.text.length
    if (length == 0) return null
    if (position.y < 0f || position.y > layout.size.height) return null
    val line = layout.getLineForVerticalPosition(position.y)
    if (position.x < layout.getLineLeft(line) - TapPadding) return null
    if (position.x > layout.getLineRight(line) + TapPadding) return null

    masked.tasks.forEach { task ->
        val box = boundingBoxOf(layout, masked.transformedRangeOf(task.range), length)
        if (box != null && box.inflate(TapPadding).contains(position)) {
            return TapTarget.Task(task)
        }
    }

    val offset = layout.getOffsetForPosition(position)
    masked.links.forEach { link ->
        val range = masked.transformedRangeOf(link.range)
        if (offset >= range.start && offset < range.end) return TapTarget.Link(link)
    }
    return null
}

private fun boundingBoxOf(
    layout: TextLayoutResult,
    range: MdRange,
    length: Int,
): Rect? {
    val start = range.start.coerceIn(0, length)
    val end = range.end.coerceIn(start, length)
    if (end <= start) return null
    var box = layout.getBoundingBox(start)
    for (index in start + 1 until end) {
        val next = layout.getBoundingBox(index)
        box = Rect(
            left = minOf(box.left, next.left),
            top = minOf(box.top, next.top),
            right = maxOf(box.right, next.right),
            bottom = maxOf(box.bottom, next.bottom),
        )
    }
    return box
}

private const val BoxSideRatio = 0.86f
private const val BoxCornerRatio = 0.3f
private const val BoxStrokeRatio = 0.12f
private const val BoxBaselineRatio = 0.34f
private const val CheckStrokeRatio = 0.16f

private fun Modifier.drawTaskBoxes(
    maskedOf: () -> MaskedDocument,
    layoutOf: () -> TextLayoutResult?,
    outline: Color,
    fill: Color,
    check: Color,
): Modifier = drawWithContent {
    drawContent()
    val layout = layoutOf() ?: return@drawWithContent
    val masked = maskedOf()
    val transformed = masked.mask.transformedText
    masked.tasks.forEach { task ->
        val range = masked.transformedRangeOf(task.range)
        if (range.length != 1) return@forEach
        if (transformed.getOrNull(range.start) != TaskBoxPlaceholder) return@forEach
        val glyph = boundingBoxOf(layout, range, transformed.length) ?: return@forEach
        val baseline = layout.getLineBaseline(layout.getLineForOffset(range.start))
        drawTaskBox(glyph, baseline, task.checked, outline, fill, check)
    }
}

private fun DrawScope.drawTaskBox(
    glyph: Rect,
    baseline: Float,
    checked: Boolean,
    outline: Color,
    fill: Color,
    check: Color,
) {
    val side = minOf(glyph.width, glyph.height) * BoxSideRatio
    if (side <= 0f) return
    val centerY = baseline - glyph.width * BoxBaselineRatio
    val topLeft = Offset(
        x = glyph.left + (glyph.width - side) / 2f,
        y = centerY - side / 2f,
    )
    val size = Size(side, side)
    val corner = CornerRadius(side * BoxCornerRatio)

    if (checked) {
        drawRoundRect(color = fill, topLeft = topLeft, size = size, cornerRadius = corner)
        val path = Path().apply {
            moveTo(topLeft.x + side * 0.26f, topLeft.y + side * 0.52f)
            lineTo(topLeft.x + side * 0.44f, topLeft.y + side * 0.70f)
            lineTo(topLeft.x + side * 0.75f, topLeft.y + side * 0.30f)
        }
        drawPath(
            path = path,
            color = check,
            style = Stroke(
                width = side * CheckStrokeRatio,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    } else {
        val stroke = side * BoxStrokeRatio
        drawRoundRect(
            color = outline,
            topLeft = Offset(topLeft.x + stroke / 2f, topLeft.y + stroke / 2f),
            size = Size(side - stroke, side - stroke),
            cornerRadius = corner,
            style = Stroke(width = stroke),
        )
    }
}

private fun Modifier.verticalCaretKeys(
    value: TextFieldValue,
    masked: MaskedDocument,
    layoutOf: () -> TextLayoutResult?,
    column: CaretColumn,
    onValueChange: (TextFieldValue) -> Unit,
): Modifier = onPreviewKeyEvent { event ->
    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
    val downwards = when (event.key) {
        Key.DirectionDown -> true
        Key.DirectionUp -> false
        else -> {
            column.x = null
            return@onPreviewKeyEvent false
        }
    }
    if (event.isShiftPressed || event.isCtrlPressed || event.isAltPressed || event.isMetaPressed) {
        column.x = null
        return@onPreviewKeyEvent false
    }
    val layout = layoutOf() ?: return@onPreviewKeyEvent false
    val moved = caretByLine(value, masked, layout, downwards, column)
    onValueChange(moved)
    true
}

private fun caretByLine(
    value: TextFieldValue,
    masked: MaskedDocument,
    layout: TextLayoutResult,
    downwards: Boolean,
    column: CaretColumn,
): TextFieldValue {
    val mask = masked.mask
    val caret = mask.originalToTransformed(value.selection.start)
    val line = layout.getLineForOffset(caret)
    val targetLine = line + if (downwards) 1 else -1
    if (targetLine < 0) {
        column.x = null
        return value.copy(selection = TextRange(0))
    }
    if (targetLine >= layout.lineCount) {
        column.x = null
        return value.copy(selection = TextRange(value.text.length))
    }
    val x = column.x ?: layout.getHorizontalPosition(caret, usePrimaryDirection = true)
    column.x = x
    val y = (layout.getLineTop(targetLine) + layout.getLineBottom(targetLine)) / 2f
    val target = mask.transformedToOriginal(layout.getOffsetForPosition(Offset(x, y)))
    return value.copy(selection = TextRange(target.coerceIn(0, value.text.length)))
}
