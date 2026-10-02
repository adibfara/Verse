package com.github.adibfara.mds.core.markdown

enum class InlineFormat(val marker: String) {
    Bold("**"),
    Italic("*"),
    Strikethrough("~~"),
    Code("`"),
}

enum class LineMarkerKind {
    None,
    Bullet,
    Task,
    Ordered,
}

data class TextEdit(val text: String, val selection: MdRange)

sealed interface FormatAction {
    data class ToggleInline(val range: MdRange, val format: InlineFormat) : FormatAction

    data class SetHeading(val offset: Int, val level: Int) : FormatAction

    data class SetLineMarker(val offset: Int, val kind: LineMarkerKind) : FormatAction

    data object ContinueList : FormatAction
}

fun applyFormatAction(text: String, selection: MdRange, action: FormatAction): TextEdit {
    val splices = when (action) {
        is FormatAction.ToggleInline -> inlineSplices(text, action.range, action.format)
        is FormatAction.SetHeading -> headingSplices(text, action.offset, action.level)
        is FormatAction.SetLineMarker -> lineMarkerSplices(text, action.offset, action.kind)
        FormatAction.ContinueList -> if (selection.start == selection.end) {
            continuationSplices(text, selection.start)
        } else {
            emptyList()
        }
    }
    return applySplices(text, splices, selection)
}

fun hasListContinuation(text: String, caretOffset: Int): Boolean =
    continuationSplices(text, caretOffset).isNotEmpty()

private fun inlineSplices(text: String, range: MdRange, format: InlineFormat): List<Splice> {
    val start = range.start.coerceIn(0, text.length)
    val end = range.end.coerceIn(start, text.length)
    if (end == start) return emptyList()
    val marker = format.marker
    val wrapped = wrappedRangeOf(text, MdRange(start, end), marker)
    if (wrapped != null) {
        return listOf(
            Splice(wrapped.start, wrapped.start + marker.length, ""),
            Splice(wrapped.end - marker.length, wrapped.end, ""),
        )
    }
    return listOf(Splice(start, start, marker), Splice(end, end, marker))
}

private fun headingSplices(text: String, offset: Int, level: Int): List<Splice> {
    val line = lineRangeAt(text, offset)
    val indentEnd = indentEndOf(text, line)
    var hashEnd = indentEnd
    while (hashEnd < line.end && text[hashEnd] == '#') hashEnd++
    var contentStart = hashEnd
    if (hashEnd > indentEnd) {
        while (contentStart < line.end && text[contentStart] == ' ') contentStart++
    }
    val prefix = if (level <= 0) "" else "#".repeat(level.coerceAtMost(6)) + " "
    return listOf(Splice(indentEnd, contentStart, prefix))
}

private fun lineMarkerSplices(text: String, offset: Int, kind: LineMarkerKind): List<Splice> {
    val line = lineRangeAt(text, offset)
    val scan = scanLinePrefix(text, line)
    val bullet = scan.marker.ifEmpty { "-" }
    val checkBox = scan.checkBox ?: ' '
    val prefix = when (kind) {
        LineMarkerKind.None -> ""
        LineMarkerKind.Bullet -> "$bullet "
        LineMarkerKind.Task -> "$bullet [$checkBox] "
        LineMarkerKind.Ordered -> orderedMarkerPrefix(text, line, scan)
    }
    return listOf(Splice(scan.indentEnd, scan.contentStart, prefix))
}

private fun isOrderedMarker(marker: String): Boolean = marker.isNotEmpty() && marker.first().isDigit()

private fun orderedMarkerPrefix(text: String, line: MdRange, scan: LinePrefixScan): String {
    val number: Int
    val separator: Char
    if (isOrderedMarker(scan.marker)) {
        number = scan.marker.takeWhile { it.isDigit() }.toIntOrNull() ?: 1
        separator = scan.marker.last()
    } else {
        val indent = scan.indentEnd - line.start
        number = (previousOrderedSiblingNumber(text, line, indent) ?: 0) + 1
        separator = '.'
    }
    val checkBox = scan.checkBox
    return if (checkBox != null) "$number$separator [$checkBox] " else "$number$separator "
}

private fun previousOrderedSiblingNumber(text: String, line: MdRange, indent: Int): Int? {
    var cursor = line.start - 1
    while (cursor >= 0) {
        val priorLine = lineRangeAt(text, cursor)
        if (priorLine.start >= priorLine.end) return null
        val priorScan = scanLinePrefix(text, priorLine)
        val priorIndent = priorScan.indentEnd - priorLine.start
        when {
            priorIndent < indent -> return null
            priorIndent == indent -> {
                return if (isOrderedMarker(priorScan.marker)) {
                    priorScan.marker.takeWhile { it.isDigit() }.toIntOrNull()
                } else {
                    null
                }
            }

            else -> cursor = priorLine.start - 1
        }
    }
    return null
}

fun lineMarkerKindAt(text: String, offset: Int): LineMarkerKind {
    val scan = scanLinePrefix(text, lineRangeAt(text, offset))
    return when {
        scan.checkBox != null -> LineMarkerKind.Task
        isOrderedMarker(scan.marker) -> LineMarkerKind.Ordered
        scan.marker.isNotEmpty() -> LineMarkerKind.Bullet
        else -> LineMarkerKind.None
    }
}

private fun continuationSplices(text: String, caretOffset: Int): List<Splice> {
    val line = lineRangeAt(text, caretOffset)
    val scan = scanLinePrefix(text, line)
    val kind = when {
        scan.checkBox != null -> LineMarkerKind.Task
        isOrderedMarker(scan.marker) -> LineMarkerKind.Ordered
        scan.marker.isNotEmpty() -> LineMarkerKind.Bullet
        else -> LineMarkerKind.None
    }
    if (kind == LineMarkerKind.None) return emptyList()

    val isEmptyItem = text.substring(scan.contentStart, line.end).isBlank()
    if (isEmptyItem) {
        return listOf(Splice(scan.indentEnd, line.end, ""))
    }

    val indentText = text.substring(line.start, scan.indentEnd)
    val newMarker = when (kind) {
        LineMarkerKind.Bullet -> "${scan.marker.ifEmpty { "-" }} "
        LineMarkerKind.Task -> "${scan.marker.ifEmpty { "-" }} [ ] "
        LineMarkerKind.Ordered -> {
            val number = (scan.marker.takeWhile { it.isDigit() }.toIntOrNull() ?: 1) + 1
            val separator = scan.marker.lastOrNull { !it.isDigit() } ?: '.'
            "$number$separator "
        }
        LineMarkerKind.None -> ""
    }
    return listOf(Splice(caretOffset, caretOffset, "\n" + indentText + newMarker))
}

fun lineContentRange(text: String, offset: Int): MdRange {
    val line = lineRangeAt(text, offset)
    var index = indentEndOf(text, line)
    while (index < line.end && text[index] == '#') index++
    while (index < line.end && text[index] == ' ') index++
    return MdRange(index, line.end)
}

private val FormatsByKind = mapOf(
    StyleKind.Bold to InlineFormat.Bold,
    StyleKind.Italic to InlineFormat.Italic,
    StyleKind.Strikethrough to InlineFormat.Strikethrough,
    StyleKind.InlineCode to InlineFormat.Code,
)

fun inlineFormatsCovering(styles: List<StyledRange>, range: MdRange): Set<InlineFormat> {
    val formats = mutableSetOf<InlineFormat>()
    styles.forEach { styled ->
        val format = FormatsByKind[styled.kind] ?: return@forEach
        if (styled.range.start <= range.start && styled.range.end >= range.end) formats.add(format)
    }
    return formats
}

fun headingLevelAt(styles: List<StyledRange>, offset: Int): Int {
    var level = 0
    styles.forEach { styled ->
        if (styled.kind.isHeading && styled.range.contains(offset)) {
            level = styled.kind.headingLevel
        }
    }
    return level
}

enum class SpecialSpanKind {
    Inline,
    LineMarker,
}

data class SpecialSpan(val kind: SpecialSpanKind, val range: MdRange)

fun specialSpanAt(styles: List<StyledRange>, text: String, offset: Int): SpecialSpan? {
    var innermost: MdRange? = null
    var heading = false
    styles.forEach { styled ->
        if (!styled.range.contains(offset)) return@forEach
        when (styled.kind) {
            StyleKind.ListMarker,
            StyleKind.TaskMarker,
            StyleKind.TaskMarkerDone,
            -> return SpecialSpan(SpecialSpanKind.LineMarker, styled.range)

            StyleKind.Bold,
            StyleKind.Italic,
            StyleKind.Strikethrough,
            StyleKind.InlineCode,
            StyleKind.Link,
            -> {
                val current = innermost
                if (current == null || styled.range.length < current.length) innermost = styled.range
            }

            else -> if (styled.kind.isHeading) heading = true
        }
    }
    innermost?.let { return SpecialSpan(SpecialSpanKind.Inline, it) }
    if (heading) return SpecialSpan(SpecialSpanKind.Inline, lineContentRange(text, offset))
    return null
}

private class LinePrefixScan(
    val indentEnd: Int,
    val contentStart: Int,
    val marker: String,
    val checkBox: Char?,
)

private fun scanLinePrefix(text: String, line: MdRange): LinePrefixScan {
    val indentEnd = indentEndOf(text, line)
    var index = indentEnd
    var marker = ""
    if (index < line.end && (text[index] == '-' || text[index] == '*' || text[index] == '+')) {
        marker = text.substring(index, index + 1)
        index++
    } else {
        var digits = index
        while (digits < line.end && text[digits].isDigit()) digits++
        if (digits > index && digits < line.end && (text[digits] == '.' || text[digits] == ')')) {
            marker = text.substring(index, digits + 1)
            index = digits + 1
        }
    }
    if (marker.isEmpty()) return LinePrefixScan(indentEnd, indentEnd, "", null)
    while (index < line.end && text[index] == ' ') index++

    var checkBox: Char? = null
    if (index + 3 <= line.end && text[index] == '[' && text[index + 2] == ']') {
        val inner = text[index + 1]
        if (inner == ' ' || inner == 'x' || inner == 'X') {
            checkBox = if (inner == ' ') ' ' else 'x'
            index += 3
            while (index < line.end && text[index] == ' ') index++
        }
    }
    return LinePrefixScan(indentEnd, index, marker, checkBox)
}

private fun indentEndOf(text: String, line: MdRange): Int {
    var index = line.start
    while (index < line.end && (text[index] == ' ' || text[index] == '\t')) index++
    return index
}

private class Splice(val start: Int, val end: Int, val replacement: String)

private fun applySplices(text: String, splices: List<Splice>, selection: MdRange): TextEdit {
    if (splices.isEmpty()) return TextEdit(text, selection)
    var updated = text
    splices.asReversed().forEach { splice ->
        updated = updated.replaceRange(splice.start, splice.end, splice.replacement)
    }
    val start = mapOffset(selection.start, splices).coerceIn(0, updated.length)
    val end = if (selection.length == 0) {
        start
    } else {
        maxOf(start, mapOffset(selection.end, splices, keepLeft = true)).coerceIn(0, updated.length)
    }
    return TextEdit(updated, MdRange(start, end))
}

private fun mapOffset(offset: Int, splices: List<Splice>, keepLeft: Boolean = false): Int {
    var shift = 0
    splices.forEach { splice ->
        val delta = splice.replacement.length - (splice.end - splice.start)
        val isInsertion = splice.end == splice.start
        val isAfter = if (isInsertion && keepLeft) {
            offset > splice.start
        } else {
            offset >= splice.end
        }
        when {
            isAfter -> shift += delta
            offset > splice.start -> return splice.start + shift + splice.replacement.length
        }
    }
    return offset + shift
}

private fun wrappedRangeOf(text: String, range: MdRange, marker: String): MdRange? {
    val character = marker[0]
    val width = marker.length
    if (range.length >= width * 2 &&
        runForward(text, range.start, character) == width &&
        runBackward(text, range.end, character) == width
    ) {
        return range
    }
    if (range.start >= width && range.end + width <= text.length &&
        runBackward(text, range.start, character) == width &&
        runForward(text, range.end, character) == width
    ) {
        return MdRange(range.start - width, range.end + width)
    }
    return null
}

private fun runForward(text: String, from: Int, character: Char): Int {
    var index = from
    while (index < text.length && text[index] == character) index++
    return index - from
}

private fun runBackward(text: String, from: Int, character: Char): Int {
    var index = from
    while (index > 0 && text[index - 1] == character) index--
    return from - index
}
