package com.github.adibfara.verse.feature.editor.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import com.github.adibfara.verse.core.designsystem.component.VerseIcons
import com.github.adibfara.verse.core.designsystem.theme.VerseTheme
import com.github.adibfara.verse.core.markdown.FormatAction
import com.github.adibfara.verse.core.markdown.LineMarkerKind
import com.github.adibfara.verse.core.markdown.MaskedDocument
import com.github.adibfara.verse.core.markdown.MdCodeBlock
import com.github.adibfara.verse.core.markdown.MdDocument
import com.github.adibfara.verse.core.markdown.MdLink
import com.github.adibfara.verse.core.markdown.MdRange
import com.github.adibfara.verse.core.markdown.MdTableGrid
import com.github.adibfara.verse.core.markdown.MdTask
import com.github.adibfara.verse.core.markdown.QuoteIndentSpaces
import com.github.adibfara.verse.core.markdown.SpecialSpanKind
import com.github.adibfara.verse.core.markdown.StyleKind
import com.github.adibfara.verse.core.markdown.TaskBoxPlaceholder
import com.github.adibfara.verse.core.markdown.headingLevelAt
import com.github.adibfara.verse.core.markdown.inlineFormatsCovering
import com.github.adibfara.verse.core.markdown.inlineLinkAt
import com.github.adibfara.verse.core.markdown.lineMarkerKindAt
import com.github.adibfara.verse.core.markdown.lineRangeAt
import com.github.adibfara.verse.core.markdown.maskDocument
import com.github.adibfara.verse.core.markdown.maskEditsFor
import com.github.adibfara.verse.core.markdown.specialSpanAt

private const val TapPadding = 3f
private const val TaskTapPadding = 9f

@Composable
fun RenderedMarkdownField(
    value: TextFieldValue,
    document: MdDocument,
    onValueChange: (TextFieldValue) -> Unit,
    onToggleTask: (MdRange, Boolean) -> Unit,
    onOpenLink: (String) -> Unit,
    onFormat: (FormatAction) -> Unit,
    focusRequester: FocusRequester? = null,
    showFormatPopup: Boolean = true,
    readOnly: Boolean = false,
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
        focusRequester = focusRequester,
        showFormatPopup = showFormatPopup,
        readOnly = readOnly,
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
    focusRequester: FocusRequester? = null,
    showFormatPopup: Boolean = true,
    readOnly: Boolean = false,
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
        focusRequester = focusRequester,
        showFormatPopup = showFormatPopup,
        readOnly = readOnly,
        modifier = modifier,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MaskedMarkdownField(
    value: TextFieldValue,
    document: MdDocument,
    revealedLine: MdRange?,
    onValueChange: (TextFieldValue) -> Unit,
    onToggleTask: (MdRange, Boolean) -> Unit,
    onOpenLink: (String) -> Unit,
    onFormat: (FormatAction) -> Unit,
    focusRequester: FocusRequester? = null,
    showFormatPopup: Boolean = true,
    readOnly: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val styles = rememberMarkdownStyles()
    val colors = VerseTheme.colors
    val clipboard = LocalClipboardManager.current
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
    val currentValue = rememberUpdatedState(value)
    val currentValueChange = rememberUpdatedState(onValueChange)
    val currentToggleTask = rememberUpdatedState(onToggleTask)
    val currentOpenLink = rememberUpdatedState(onOpenLink)
    val overTarget = remember { mutableStateOf(false) }
    val taps = remember(column, readOnly) {
        Modifier
            .markdownTaps(
                maskedOf = { currentMasked.value },
                layoutOf = { layout.value },
                column = column,
                toggleOnText = readOnly,
                onToggleTask = { range, checked -> currentToggleTask.value(range, checked) },
                onOpenLink = { target -> currentOpenLink.value(target) },
                onCopyCode = { code -> clipboard.setText(AnnotatedString(code)) },
            )
            .markdownHover(
                maskedOf = { currentMasked.value },
                layoutOf = { layout.value },
                toggleOnText = readOnly,
                onOverTarget = { overTarget.value = it },
            )
    }
    val measurer = rememberTextMeasurer()
    val copyPainter = rememberVectorPainter(VerseIcons.Copy)
    val labelStyle = VerseTheme.typography.uiSmall
    val decorations = remember(colors, measurer, copyPainter, labelStyle) {
        Modifier.drawBlockDecorations(
            maskedOf = { currentMasked.value },
            layoutOf = { layout.value },
            measurer = measurer,
            copyPainter = copyPainter,
            labelStyle = labelStyle,
            codeBackground = colors.codeBackground,
            codeBorder = colors.border,
            labelColor = colors.textMuted,
            outline = colors.textSecondary,
            fill = colors.accent,
            check = colors.textOnAccent,
            rule = colors.borderStrong,
            quoteBar = colors.quoteBar,
            tableLine = colors.border,
            tableHeader = colors.surfaceHover,
        )
    }
    val caretKeys = remember(column) {
        Modifier.verticalCaretKeys(
            valueOf = { currentValue.value },
            maskedOf = { currentMasked.value },
            layoutOf = { layout.value },
            column = column,
            onValueChange = { moved -> currentValueChange.value(moved) },
        )
    }
    val editorBody = VerseTheme.typography.editorBody
    val textStyle = remember(colors, editorBody) {
        editorBody.copy(color = colors.textPrimary)
    }
    val cursorBrush = remember(colors) { SolidColor(colors.caret) }

    val density = LocalDensity.current
    val caretMargin = with(density) { VerseTheme.spacing.xl.toPx() }
    val keyboardInset = WindowInsets.ime.getBottom(density)
    val bringCaretIntoView = remember { BringIntoViewRequester() }
    val lastBroughtCaret = remember { mutableStateOf<Pair<Int, Int>?>(null) }
    LaunchedEffect(value.selection.start, layout.value, keyboardInset) {
        val result = layout.value ?: return@LaunchedEffect
        val key = value.selection.start to keyboardInset
        if (lastBroughtCaret.value == key) return@LaunchedEffect
        lastBroughtCaret.value = key
        bringCaretIntoView.bringIntoView(
            caretBoundsOf(masked, result, value.selection.start, caretMargin),
        )
    }

    Box(modifier = modifier) {
        BasicTextField(
            value = value,
            onValueChange = { next ->
                if (next.text != value.text) column.x = null
                onValueChange(next)
            },
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewRequester(bringCaretIntoView)
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .then(taps)
                .pointerHoverIcon(
                    icon = if (overTarget.value) PointerIcon.Hand else PointerIcon.Default,
                    overrideDescendants = overTarget.value,
                )
                .then(decorations)
                .then(caretKeys),
            textStyle = textStyle,
            cursorBrush = cursorBrush,
            visualTransformation = transformation,
            onTextLayout = { layout.value = it },
            readOnly = readOnly,
        )

        MarkdownOverlays(
            masked = masked,
            value = value,
            layout = layout,
            showFormatPopup = showFormatPopup,
            onFormat = onFormat,
        )
    }
}

@Composable
private fun MarkdownOverlays(
    masked: MaskedDocument,
    value: TextFieldValue,
    layout: State<TextLayoutResult?>,
    showFormatPopup: Boolean,
    onFormat: (FormatAction) -> Unit,
) {
    val result = layout.value
    val popup = remember(masked, value.selection, result, showFormatPopup) {
        if (showFormatPopup) popupStateFor(masked, value, result) else null
    }
    popup?.let { state -> FormatPopup(state = state, onAction = onFormat) }
}

private fun popupStateFor(
    masked: MaskedDocument,
    value: TextFieldValue,
    layout: TextLayoutResult?,
): FormatPopupState? {
    if (layout == null) return null
    val text = masked.mask.originalText
    if (text != value.text) return null
    listMarkerPopupState(masked, text, value.selection.start, layout)?.let { return it }
    selectionPopupState(masked, value, layout)?.let { return it }
    return caretInlinePopupState(masked, text, value.selection.start, layout)
}

private fun caretInlinePopupState(
    masked: MaskedDocument,
    text: String,
    caretOffset: Int,
    layout: TextLayoutResult,
): FormatPopupState? {
    val span = specialSpanAt(masked.styles, text, caretOffset) ?: return null
    if (span.kind != SpecialSpanKind.Inline) return null
    return inlinePopupState(masked, span.range, layout)
}

private fun listMarkerPopupState(
    masked: MaskedDocument,
    text: String,
    caretOffset: Int,
    layout: TextLayoutResult,
): FormatPopupState? {
    val caretLine = lineRangeAt(text, caretOffset)
    if (lineMarkerKindAt(text, caretLine.start) == LineMarkerKind.None) return null

    var blockStart = caretLine.start
    while (blockStart > 0) {
        val previousLine = lineRangeAt(text, blockStart - 1)
        if (lineMarkerKindAt(text, previousLine.start) == LineMarkerKind.None) break
        blockStart = previousLine.start
    }
    val firstLine = lineRangeAt(text, blockStart)
    val length = layout.layoutInput.text.length
    val glyph = markerGlyphRange(masked, firstLine) ?: return null
    val box = boundingBoxOf(layout, masked.transformedRangeOf(glyph), length) ?: return null
    return FormatPopupState(
        kind = FormatPopupKind.LineMarker,
        range = caretLine,
        anchor = box,
        formats = emptySet(),
        headingLevel = 0,
        lineMarker = lineMarkerKindAt(text, caretLine.start),
    )
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
    val text = masked.mask.originalText
    val start = range.start.coerceIn(0, text.length)
    val end = range.end.coerceIn(start, text.length)
    return FormatPopupState(
        kind = FormatPopupKind.Inline,
        range = range,
        anchor = box,
        formats = inlineFormatsCovering(masked.styles, range),
        headingLevel = headingLevelAt(masked.styles, range.start),
        lineMarker = LineMarkerKind.None,
        sourceText = text.substring(start, end),
        isLink = inlineLinkAt(text, start) != null,
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RawMarkdownField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    focusRequester: FocusRequester? = null,
    readOnly: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colors = VerseTheme.colors
    val layout = remember { mutableStateOf<TextLayoutResult?>(null) }
    val density = LocalDensity.current
    val caretMargin = with(density) { VerseTheme.spacing.xl.toPx() }
    val keyboardInset = WindowInsets.ime.getBottom(density)
    val bringCaretIntoView = remember { BringIntoViewRequester() }
    LaunchedEffect(value.selection.start, layout.value, keyboardInset) {
        val result = layout.value ?: return@LaunchedEffect
        bringCaretIntoView.bringIntoView(
            plainCaretBoundsOf(result, value.selection.start, caretMargin),
        )
    }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .bringIntoViewRequester(bringCaretIntoView)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
        textStyle = VerseTheme.typography.editorRaw.copy(color = colors.textPrimary),
        cursorBrush = SolidColor(colors.caret),
        onTextLayout = { layout.value = it },
        readOnly = readOnly,
    )
}

private class CaretColumn {
    var x: Float? = null
}

private sealed interface TapTarget {
    data class Task(val task: MdTask) : TapTarget
    data class Link(val link: MdLink) : TapTarget
    data class CopyCode(val block: MdCodeBlock) : TapTarget
}

private fun Modifier.markdownTaps(
    maskedOf: () -> MaskedDocument,
    layoutOf: () -> TextLayoutResult?,
    column: CaretColumn,
    toggleOnText: Boolean,
    onToggleTask: (MdRange, Boolean) -> Unit,
    onOpenLink: (String) -> Unit,
    onCopyCode: (String) -> Unit,
): Modifier = pointerInput(toggleOnText) {
    val slop = viewConfiguration.touchSlop
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        column.x = null
        val layout = layoutOf() ?: return@awaitEachGesture
        val masked = maskedOf()
        val target = targetAt(masked, layout, down.position, toggleOnText) ?: return@awaitEachGesture
        down.consume()
        var last = down.position
        var travelled = 0f
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == down.id } ?: return@awaitEachGesture
            travelled += (change.position - last).getDistance()
            last = change.position
            if (travelled > slop) return@awaitEachGesture
            change.consume()
            if (!change.pressed) break
        }
        if (targetAt(masked, layout, last, toggleOnText) != target) return@awaitEachGesture
        when (target) {
            is TapTarget.Task -> onToggleTask(target.task.range, !target.task.checked)
            is TapTarget.Link -> onOpenLink(target.link.target)
            is TapTarget.CopyCode -> onCopyCode(target.block.code)
        }
    }
}

private fun Modifier.markdownHover(
    maskedOf: () -> MaskedDocument,
    layoutOf: () -> TextLayoutResult?,
    toggleOnText: Boolean,
    onOverTarget: (Boolean) -> Unit,
): Modifier = pointerInput(toggleOnText) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.type == PointerEventType.Exit) {
                onOverTarget(false)
                continue
            }
            val position = event.changes.lastOrNull()?.position
            val layout = layoutOf()
            if (position == null || layout == null) {
                onOverTarget(false)
                continue
            }
            val masked = maskedOf()
            onOverTarget(targetAt(masked, layout, position, toggleOnText) != null)
        }
    }
}

private fun targetAt(
    masked: MaskedDocument,
    layout: TextLayoutResult,
    position: Offset,
    toggleOnText: Boolean,
): TapTarget? {
    val length = layout.layoutInput.text.length
    if (length == 0) return null
    if (position.y < 0f || position.y > layout.size.height) return null

    masked.codeBlocks.forEach { block ->
        val headerLine = codeHeaderLineOf(masked, layout, block) ?: return@forEach
        if (codeCopyRect(layout, headerLine).inflate(TapPadding).contains(position)) {
            return TapTarget.CopyCode(block)
        }
    }

    val line = layout.getLineForVerticalPosition(position.y)
    if (!toggleOnText) {
        if (position.x < layout.getLineLeft(line) - TaskTapPadding) return null
        if (position.x > layout.getLineRight(line) + TaskTapPadding) return null
    }

    val offset = layout.getOffsetForPosition(position)

    masked.tasks.forEach { task ->
        val box = boundingBoxOf(layout, masked.transformedRangeOf(task.range), length)
        if (box != null && box.inflate(TaskTapPadding).contains(position)) {
            return TapTarget.Task(task)
        }
    }

    masked.links.forEach { link ->
        val range = masked.transformedRangeOf(link.range)
        if (offset >= range.start && offset < range.end) return TapTarget.Link(link)
    }

    if (toggleOnText) {
        val originalOffset = masked.mask.transformedToOriginal(offset)
        val sourceLine = lineRangeAt(masked.mask.originalText, originalOffset)
        masked.tasks.forEach { task ->
            if (task.range.start >= sourceLine.start && task.range.start <= sourceLine.end) {
                return TapTarget.Task(task)
            }
        }
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
    val firstLine = layout.getLineForOffset(start)
    val lastLine = layout.getLineForOffset(end - 1)
    var left: Float
    var right: Float
    if (firstLine == lastLine) {
        val head = layout.getBoundingBox(start)
        val tail = layout.getBoundingBox(end - 1)
        left = minOf(head.left, tail.left)
        right = maxOf(head.right, tail.right)
    } else {
        left = layout.getLineLeft(firstLine)
        right = layout.getLineRight(firstLine)
        for (line in firstLine + 1..lastLine) {
            left = minOf(left, layout.getLineLeft(line))
            right = maxOf(right, layout.getLineRight(line))
        }
    }
    return Rect(
        left = left,
        top = layout.getLineTop(firstLine),
        right = right,
        bottom = layout.getLineBottom(lastLine),
    )
}

private fun caretBoundsOf(
    masked: MaskedDocument,
    layout: TextLayoutResult,
    caretOffset: Int,
    margin: Float,
): Rect = plainCaretBoundsOf(
    layout = layout,
    caretOffset = masked.mask.originalToTransformed(caretOffset),
    margin = margin,
)

private fun plainCaretBoundsOf(
    layout: TextLayoutResult,
    caretOffset: Int,
    margin: Float,
): Rect {
    val length = layout.layoutInput.text.length
    val offset = caretOffset.coerceIn(0, length)
    val line = layout.getLineForOffset(offset)
    val x = layout.getHorizontalPosition(offset, usePrimaryDirection = true)
    return Rect(
        left = x,
        top = layout.getLineTop(line) - margin,
        right = x,
        bottom = layout.getLineBottom(line) + margin,
    )
}

private const val BoxSideRatio = 1.3f
private const val BoxCornerRatio = 0.3f
private const val BoxStrokeRatio = 0.11f
private const val BoxBaselineRatio = 0.34f
private const val CheckStrokeRatio = 0.15f
private const val CodeCopyRatio = 0.62f
private const val CodeGutterRatio = 0.3f
private const val CodeCornerRatio = 0.35f
private const val RuleStrokeRatio = 0.07f
private const val QuoteBarRatio = 0.13f
private const val TableStrokeRatio = 0.05f

private fun codeHeaderLineOf(
    masked: MaskedDocument,
    layout: TextLayoutResult,
    block: MdCodeBlock,
): Int? {
    val length = layout.layoutInput.text.length
    if (length == 0) return null
    val start = masked.mask.originalToTransformed(block.range.start).coerceIn(0, length)
    return layout.getLineForOffset(start)
}

private fun codeCopyRect(layout: TextLayoutResult, line: Int): Rect {
    val top = layout.getLineTop(line)
    val bottom = layout.getLineBottom(line)
    val height = bottom - top
    val side = height * CodeCopyRatio
    val right = layout.size.width.toFloat() - height * CodeGutterRatio
    return Rect(right - side, top + (height - side) / 2f, right, top + (height + side) / 2f)
}

private fun Modifier.drawBlockDecorations(
    maskedOf: () -> MaskedDocument,
    layoutOf: () -> TextLayoutResult?,
    measurer: TextMeasurer,
    copyPainter: Painter,
    labelStyle: TextStyle,
    codeBackground: Color,
    codeBorder: Color,
    labelColor: Color,
    outline: Color,
    fill: Color,
    check: Color,
    rule: Color,
    quoteBar: Color,
    tableLine: Color,
    tableHeader: Color,
): Modifier = drawWithContent {
    val layout = layoutOf()
    if (layout == null) {
        drawContent()
        return@drawWithContent
    }
    val masked = maskedOf()
    val transformed = masked.mask.transformedText
    val grids = masked.tables.mapNotNull { table -> tableGeometryOf(masked, layout, table) }

    grids.forEach { grid ->
        grid.headerBounds?.let { header ->
            drawRect(
                color = tableHeader,
                topLeft = Offset(grid.left, header.first),
                size = Size(grid.right - grid.left, header.second - header.first),
            )
        }
    }

    masked.codeBlocks.forEach { block ->
        val headerLine = codeHeaderLineOf(masked, layout, block) ?: return@forEach
        val limit = layout.layoutInput.text.length
        val end = masked.mask.originalToTransformed(block.range.end).coerceIn(0, limit)
        val lastLine = layout.getLineForOffset(end)
        val top = layout.getLineTop(headerLine)
        val bottom = layout.getLineBottom(lastLine)
        val corner = CornerRadius((bottom - top).coerceAtMost(layout.getLineBottom(headerLine) - top) * CodeCornerRatio)
        drawRoundRect(
            color = codeBackground,
            topLeft = Offset(0f, top),
            size = Size(size.width, bottom - top),
            cornerRadius = corner,
        )
        drawRoundRect(
            color = codeBorder,
            topLeft = Offset(0f, top),
            size = Size(size.width, bottom - top),
            cornerRadius = corner,
            style = Stroke(width = 1f),
        )
        val headerBottom = layout.getLineBottom(headerLine)
        drawLine(
            color = codeBorder,
            start = Offset(0f, headerBottom),
            end = Offset(size.width, headerBottom),
            strokeWidth = 1f,
        )
    }

    drawContent()

    masked.codeBlocks.forEach { block ->
        val headerLine = codeHeaderLineOf(masked, layout, block) ?: return@forEach
        val top = layout.getLineTop(headerLine)
        val height = layout.getLineBottom(headerLine) - top
        val label = block.language.ifEmpty { "text" }
        val measured = measurer.measure(label, labelStyle)
        drawText(
            textLayoutResult = measured,
            color = labelColor,
            topLeft = Offset(
                x = height * CodeGutterRatio,
                y = top + (height - measured.size.height) / 2f,
            ),
        )
        val copy = codeCopyRect(layout, headerLine)
        translate(left = copy.left, top = copy.top) {
            with(copyPainter) {
                draw(size = Size(copy.width, copy.height), colorFilter = ColorFilter.tint(labelColor))
            }
        }
    }

    masked.tasks.forEach { task ->
        val range = masked.transformedRangeOf(task.range)
        val boxOffset = range.start + range.length / 2
        if (transformed.getOrNull(boxOffset) != TaskBoxPlaceholder) return@forEach
        val glyph = boundingBoxOf(layout, MdRange(boxOffset, boxOffset + 1), transformed.length)
            ?: return@forEach
        val baseline = layout.getLineBaseline(layout.getLineForOffset(boxOffset))
        drawTaskBox(glyph, baseline, task.checked, outline, fill, check)
    }

    masked.rules.forEach { range ->
        val collapsed = masked.transformedRangeOf(range)
        if (collapsed.length != 0) return@forEach
        val line = layout.getLineForOffset(collapsed.start.coerceIn(0, transformed.length))
        val top = layout.getLineTop(line)
        val height = layout.getLineBottom(line) - top
        val y = top + height / 2f
        drawLine(
            color = rule,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = (height * RuleStrokeRatio).coerceAtLeast(1f),
        )
    }

    masked.quoteMarkers.forEach { marker ->
        val range = masked.transformedRangeOf(marker)
        if (range.length != QuoteIndentSpaces) return@forEach
        val indent = boundingBoxOf(layout, range, transformed.length) ?: return@forEach
        val line = layout.getLineForOffset(range.start.coerceIn(0, transformed.length))
        val top = layout.getLineTop(line)
        val bottom = layout.getLineBottom(line)
        val width = ((bottom - top) * QuoteBarRatio).coerceAtLeast(1f)
        val x = indent.left + width
        drawLine(
            color = quoteBar,
            start = Offset(x, top),
            end = Offset(x, bottom),
            strokeWidth = width,
        )
    }

    grids.forEach { grid -> drawTableGrid(grid, tableLine) }
}

private class TableGeometry(
    val left: Float,
    val right: Float,
    val top: Float,
    val bottom: Float,
    val columns: List<Float>,
    val rowBottoms: List<Float>,
    val headerBounds: Pair<Float, Float>?,
    val stroke: Float,
)

private fun tableGeometryOf(
    masked: MaskedDocument,
    layout: TextLayoutResult,
    table: MdTableGrid,
): TableGeometry? {
    val length = layout.layoutInput.text.length
    if (length == 0) return null
    val delimiter = table.delimiterRange
    if (delimiter != null && masked.transformedRangeOf(delimiter).length != 0) return null

    val columns = table.dividers.mapNotNull { divider ->
        if (masked.transformedRangeOf(divider.range).length != divider.replacementLength) {
            return@mapNotNull null
        }
        val start = masked.mask.originalToTransformed(divider.range.start) + divider.leadingSpaces
        val gutter = MdRange(start, start + divider.gutterSpaces)
        boundingBoxOf(layout, gutter, length)?.let { (it.left + it.right) / 2f }
    }
    if (columns.size < 2) return null

    val rows = table.rowRanges.mapNotNull { rowBoundsOf(masked, layout, it, length) }
    if (rows.isEmpty()) return null
    val first = rows.first()
    return TableGeometry(
        left = columns.first(),
        right = columns.last(),
        top = first.first,
        bottom = rows.last().second,
        columns = columns,
        rowBottoms = rows.map { it.second },
        headerBounds = table.headerRange?.let { rowBoundsOf(masked, layout, it, length) },
        stroke = ((first.second - first.first) * TableStrokeRatio).coerceAtLeast(1f),
    )
}

private fun rowBoundsOf(
    masked: MaskedDocument,
    layout: TextLayoutResult,
    range: MdRange,
    length: Int,
): Pair<Float, Float>? {
    val start = masked.mask.originalToTransformed(range.start).coerceIn(0, length)
    val end = masked.mask.originalToTransformed(range.end).coerceIn(start, length)
    val firstLine = layout.getLineForOffset(start)
    val lastLine = layout.getLineForOffset(end)
    if (lastLine < firstLine) return null
    return layout.getLineTop(firstLine) to layout.getLineBottom(lastLine)
}

private fun DrawScope.drawTableGrid(grid: TableGeometry, color: Color) {
    grid.columns.forEach { x ->
        drawLine(
            color = color,
            start = Offset(x, grid.top),
            end = Offset(x, grid.bottom),
            strokeWidth = grid.stroke,
        )
    }
    (listOf(grid.top) + grid.rowBottoms).forEach { y ->
        drawLine(
            color = color,
            start = Offset(grid.left, y),
            end = Offset(grid.right, y),
            strokeWidth = grid.stroke,
        )
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
    valueOf: () -> TextFieldValue,
    maskedOf: () -> MaskedDocument,
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
    val moved = caretByLine(valueOf(), maskedOf(), layout, downwards, column)
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
