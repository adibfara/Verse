package com.github.adibfara.verse.core.markdown

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

    data class SetLineMarker(val kind: LineMarkerKind) : FormatAction

    data class ToggleLink(val range: MdRange) : FormatAction

    data class ContinueList(val caretOffset: Int) : FormatAction

    data class DeleteTaskMarker(val caretOffset: Int) : FormatAction
}

fun applyFormatAction(text: String, selection: MdRange, action: FormatAction): TextEdit {
    if (action is FormatAction.ToggleInline && action.range.length == 0) {
        return collapsedInlineEdit(text, action.range.start, action.format)
    }
    if (action is FormatAction.ToggleLink) {
        return linkEdit(text, action.range)
    }
    if (action is FormatAction.ContinueList) {
        if (selection.start != selection.end) return TextEdit(text, selection)
        return enterEdit(text, action.caretOffset, selection)
    }
    if (action is FormatAction.DeleteTaskMarker) {
        return taskMarkerDeletionEdit(text, action.caretOffset, selection)
    }
    val splices = when (action) {
        is FormatAction.ToggleInline -> inlineSplices(text, action.range, action.format)
        is FormatAction.SetHeading -> headingSplices(text, action.offset, action.level)
        is FormatAction.SetLineMarker -> lineMarkerSplices(text, selection, action.kind)
        is FormatAction.ToggleLink,
        is FormatAction.ContinueList,
        is FormatAction.DeleteTaskMarker,
        -> emptyList()
    }
    return applySplices(text, splices, selection)
}

fun continueListAfterNewline(text: String, newlineOffset: Int, selection: MdRange): TextEdit =
    continuationEdit(text, newlineOffset, selection)

fun insertedNewlineOffset(before: String, after: String): Int? {
    if (before.isEmpty() && after.isEmpty()) return null
    val limit = minOf(before.length, after.length)
    var prefix = 0
    while (prefix < limit && before[prefix] == after[prefix]) prefix++
    var suffix = 0
    while (suffix < limit - prefix &&
        before[before.length - 1 - suffix] == after[after.length - 1 - suffix]
    ) {
        suffix++
    }
    val inserted = after.substring(prefix, after.length - suffix)
    if (inserted.isEmpty()) return null
    val newline = inserted.indexOf('\n')
    if (newline < 0) return null
    if (inserted.indexOf('\n', startIndex = newline + 1) >= 0) return null
    return prefix + newline
}

fun taskMarkerBackspaceEdit(before: String, after: String, selection: MdRange): TextEdit? {
    if (before == after || selection.start != selection.end) return null
    val removed = removedRange(before, after) ?: return null
    val scan = scanLinePrefix(before, lineRangeAt(before, removed.start))
    if (scan.checkBox == null) return null
    val boxStart = scan.checkBoxEnd - 3
    if (removed.start < boxStart || removed.start >= scan.checkBoxEnd) return null
    if (removed.end > scan.contentStart) return null
    return applySplices(
        text = after,
        splices = listOf(Splice(scan.indentEnd, scan.contentStart - removed.length, "")),
        selection = selection,
    )
}

private fun removedRange(before: String, after: String): MdRange? {
    val removed = before.length - after.length
    if (removed <= 0) return null
    val limit = minOf(before.length, after.length)
    var prefix = 0
    while (prefix < limit && before[prefix] == after[prefix]) prefix++
    var suffix = 0
    while (suffix < limit - prefix &&
        before[before.length - 1 - suffix] == after[after.length - 1 - suffix]
    ) {
        suffix++
    }
    val end = before.length - suffix
    if (end - prefix != removed) return null
    return MdRange(prefix, end)
}


fun compositionThroughEdit(before: String, after: String, composition: MdRange): MdRange? {
    if (before == after) return composition
    val limit = minOf(before.length, after.length)
    var prefix = 0
    while (prefix < limit && before[prefix] == after[prefix]) prefix++
    var suffix = 0
    while (suffix < limit - prefix &&
        before[before.length - 1 - suffix] == after[after.length - 1 - suffix]
    ) {
        suffix++
    }
    val change = MdRange(prefix, before.length - suffix)
    val delta = after.length - before.length
    return when {
        composition.end <= change.start -> composition
        composition.start >= change.end -> MdRange(composition.start + delta, composition.end + delta)
        else -> null
    }
}

fun inlineLinkAt(text: String, offset: Int): MdRange? {
    val caret = offset.coerceIn(0, text.length)
    var open = -1
    var index = caret
    while (index > 0) {
        index--
        if (text[index] == '\n') return null
        if (text[index] == '[') {
            open = index
            break
        }
    }
    if (open < 0) return null
    val close = text.indexOf("](", startIndex = open)
    if (close < 0 || close >= text.length) return null
    val end = text.indexOf(')', startIndex = close + 2)
    if (end < 0) return null
    if (text.substring(open, end).contains('\n')) return null
    if (caret > end + 1) return null
    return MdRange(open, end + 1)
}

private fun linkEdit(text: String, range: MdRange): TextEdit {
    val start = range.start.coerceIn(0, text.length)
    val end = range.end.coerceIn(start, text.length)

    val existing = inlineLinkAt(text, start)
    if (existing != null) {
        val labelEnd = text.indexOf("](", startIndex = existing.start)
        val label = text.substring(existing.start + 1, labelEnd)
        return TextEdit(
            text.replaceRange(existing.start, existing.end, label),
            MdRange(existing.start + label.length, existing.start + label.length),
        )
    }

    val label = text.substring(start, end)
    val replacement = "[$label]()"
    val caret = if (label.isEmpty()) start + 1 else start + replacement.length - 1
    return TextEdit(text.replaceRange(start, end, replacement), MdRange(caret, caret))
}

private fun collapsedInlineEdit(text: String, offset: Int, format: InlineFormat): TextEdit {
    val caret = offset.coerceIn(0, text.length)
    val marker = format.marker
    val character = marker[0]
    val width = marker.length
    val opening = runBackward(text, caret, character)
    val closing = runForward(text, caret, character)

    if (opening == width && closing == width) {
        val landing = caret - width
        return TextEdit(text.removeRange(landing, caret + width), MdRange(landing, landing))
    }
    if (closing == width) {
        val landing = caret + width
        return TextEdit(text, MdRange(landing, landing))
    }
    val landing = caret + width
    return TextEdit(text.replaceRange(caret, caret, marker + marker), MdRange(landing, landing))
}

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
    val currentLevel = hashEnd - indentEnd
    val target = if (level in 1..6 && currentLevel == level) 0 else level
    val prefix = if (target <= 0) "" else "#".repeat(target.coerceAtMost(6)) + " "
    return listOf(Splice(indentEnd, contentStart, prefix))
}

private fun lineMarkerSplices(text: String, selection: MdRange, requested: LineMarkerKind): List<Splice> {
    if (selection.start != selection.end) return selectionSplices(text, selection, requested)
    val line = lineRangeAt(text, selection.start)
    val current = lineMarkerKindAt(text, line.start)
    val kind = if (requested != LineMarkerKind.None && current == requested) {
        LineMarkerKind.None
    } else {
        requested
    }
    if (current == LineMarkerKind.None) {
        return listOf(lineMarkerSplice(text, line, kind, orderedNumber = null, orderedSeparator = '.'))
    }

    val block = listBlockRange(text, line)
    val blockLines = buildList {
        var cursor = block.start
        while (cursor <= block.end) {
            val currentLine = lineRangeAt(text, cursor)
            add(currentLine)
            cursor = currentLine.end + 1
        }
    }

    if (kind == LineMarkerKind.Ordered) {
        val alreadyAllOrdered = blockLines.all { lineMarkerKindAt(text, it.start) == LineMarkerKind.Ordered }
        if (alreadyAllOrdered) return emptyList()
        val separator = blockLines
            .map { scanLinePrefix(text, it) }
            .firstOrNull { isOrderedMarker(it.marker) }
            ?.marker?.lastOrNull() ?: '.'
        return blockLines.mapIndexed { index, currentLine ->
            lineMarkerSplice(text, currentLine, kind, orderedNumber = index + 1, orderedSeparator = separator)
        }
    }

    return blockLines.map { currentLine ->
        lineMarkerSplice(text, currentLine, kind, orderedNumber = null, orderedSeparator = '.')
    }
}

private fun selectionSplices(text: String, selection: MdRange, requested: LineMarkerKind): List<Splice> {
    val content = selectedLines(text, selection).filter { !text.substring(it.start, it.end).isBlank() }
    if (content.isEmpty()) return emptyList()
    val clear = requested == LineMarkerKind.None ||
        content.all { lineMarkerKindAt(text, it.start) == requested }
    val kind = if (clear) LineMarkerKind.None else requested
    if (kind == LineMarkerKind.Ordered) {
        val separator = content
            .map { scanLinePrefix(text, it) }
            .firstOrNull { isOrderedMarker(it.marker) }
            ?.marker?.lastOrNull() ?: '.'
        return content.mapIndexed { index, line ->
            lineMarkerSplice(text, line, kind, orderedNumber = index + 1, orderedSeparator = separator)
        }
    }
    return content.map { line ->
        lineMarkerSplice(text, line, kind, orderedNumber = null, orderedSeparator = '.')
    }
}

private fun selectedLines(text: String, selection: MdRange): List<MdRange> {
    val start = selection.start.coerceIn(0, text.length)
    val end = selection.end.coerceIn(0, text.length)
    val first = lineRangeAt(text, start)
    val lastStart = lineRangeAt(text, (end - 1).coerceAtLeast(start)).start
    val lines = mutableListOf<MdRange>()
    var cursor = first.start
    while (cursor <= lastStart) {
        val line = lineRangeAt(text, cursor)
        lines.add(line)
        cursor = line.end + 1
    }
    return lines
}

private fun listBlockRange(text: String, line: MdRange): MdRange {
    var blockStart = line.start
    while (blockStart > 0) {
        val previousLine = lineRangeAt(text, blockStart - 1)
        if (lineMarkerKindAt(text, previousLine.start) == LineMarkerKind.None) break
        blockStart = previousLine.start
    }
    var blockEnd = line.end
    while (blockEnd < text.length) {
        val nextLine = lineRangeAt(text, blockEnd + 1)
        if (lineMarkerKindAt(text, nextLine.start) == LineMarkerKind.None) break
        blockEnd = nextLine.end
    }
    return MdRange(blockStart, blockEnd)
}

private fun lineMarkerSplice(
    text: String,
    line: MdRange,
    kind: LineMarkerKind,
    orderedNumber: Int?,
    orderedSeparator: Char,
): Splice {
    val scan = scanLinePrefix(text, line)
    val bullet = if (scan.marker.isEmpty() || (scan.checkBox == null && isOrderedMarker(scan.marker))) {
        "-"
    } else {
        scan.marker
    }
    val checkBox = scan.checkBox ?: ' '
    val prefix = when (kind) {
        LineMarkerKind.None -> ""
        LineMarkerKind.Bullet -> "$bullet "
        LineMarkerKind.Task -> "$bullet [$checkBox] "
        LineMarkerKind.Ordered -> {
            val number = orderedNumber ?: 1
            "$number$orderedSeparator "
        }
    }
    return Splice(scan.indentEnd, scan.contentStart, prefix)
}

private fun isOrderedMarker(marker: String): Boolean = marker.isNotEmpty() && marker.first().isDigit()

fun lineMarkerKindAt(text: String, offset: Int): LineMarkerKind =
    kindOf(scanLinePrefix(text, lineRangeAt(text, offset)))

fun caretAfterTaskMarker(text: String, offset: Int): Boolean {
    val caret = offset.coerceIn(0, text.length)
    val scan = scanLinePrefix(text, lineRangeAt(text, caret))
    return scan.checkBox != null && caret in scan.checkBoxEnd..scan.contentStart
}

private fun taskMarkerDeletionEdit(text: String, caretOffset: Int, selection: MdRange): TextEdit {
    if (!caretAfterTaskMarker(text, caretOffset)) return TextEdit(text, selection)
    val scan = scanLinePrefix(text, lineRangeAt(text, caretOffset.coerceIn(0, text.length)))
    return applySplices(
        text = text,
        splices = listOf(Splice(scan.indentEnd, scan.contentStart, "")),
        selection = selection,
    )
}

private fun kindOf(scan: LinePrefixScan): LineMarkerKind = when {
    scan.checkBox != null -> LineMarkerKind.Task
    isOrderedMarker(scan.marker) -> LineMarkerKind.Ordered
    scan.marker.isNotEmpty() -> LineMarkerKind.Bullet
    else -> LineMarkerKind.None
}

private fun enterEdit(text: String, caretOffset: Int, selection: MdRange): TextEdit {
    val caret = caretOffset.coerceIn(0, text.length)
    val withNewline = text.substring(0, caret) + "\n" + text.substring(caret)
    val afterCaret = MdRange(caret + 1, caret + 1)
    if (lineMarkerKindAt(text, caret) == LineMarkerKind.None) return TextEdit(withNewline, afterCaret)
    val continued = continuationEdit(withNewline, caret, afterCaret)
    return if (continued.text == withNewline) TextEdit(withNewline, afterCaret) else continued
}

private fun continuationEdit(text: String, newlineOffset: Int, selection: MdRange): TextEdit {
    val newline = newlineOffset.coerceIn(0, text.length)
    if (newline >= text.length || text[newline] != '\n') return TextEdit(text, selection)
    val previousLine = lineRangeAt(text, if (newline == 0) 0 else newline - 1)
    val scan = scanLinePrefix(text, previousLine)
    val kind = kindOf(scan)
    if (kind == LineMarkerKind.None) return TextEdit(text, selection)

    val isEmptyItem = text.substring(scan.contentStart, previousLine.end).isBlank() &&
        scan.prefix == scan.canonicalPrefix
    if (isEmptyItem) {
        return applySplices(
            text = text,
            splices = listOf(Splice(previousLine.start, previousLine.end, "")),
            selection = selection,
        )
    }
    val marker = text.substring(previousLine.start, scan.indentEnd) + nextMarker(scan, kind)
    if (text.startsWith(marker, newline + 1)) return TextEdit(text, selection)
    return applySplices(
        text = text,
        splices = listOf(Splice(newline + 1, newline + 1, marker)),
        selection = selection,
    )
}

private fun nextMarker(scan: LinePrefixScan, kind: LineMarkerKind): String = when (kind) {
    LineMarkerKind.Bullet -> "${scan.marker.ifEmpty { "-" }} "
    LineMarkerKind.Task -> "${scan.marker.ifEmpty { "-" }} [ ] "
    LineMarkerKind.Ordered -> {
        val number = (scan.marker.takeWhile { it.isDigit() }.toIntOrNull() ?: 1) + 1
        val separator = scan.marker.lastOrNull { !it.isDigit() } ?: '.'
        "$number$separator "
    }
    LineMarkerKind.None -> ""
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
        if (styled.kind.isHeading && offset in styled.range.start..styled.range.end) {
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
    val checkBoxEnd: Int,
    val prefix: String,
    val canonicalPrefix: String,
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
    if (marker.isEmpty()) {
        return LinePrefixScan(indentEnd, indentEnd, "", null, -1, "", "")
    }
    while (index < line.end && text[index] == ' ') index++

    var checkBox: Char? = null
    var rawCheckBox = ""
    var checkBoxEnd = -1
    if (index + 3 <= line.end && text[index] == '[' && text[index + 2] == ']') {
        val inner = text[index + 1]
        if (inner == ' ' || inner == 'x' || inner == 'X') {
            checkBox = if (inner == ' ') ' ' else 'x'
            rawCheckBox = text.substring(index, index + 3)
            index += 3
            checkBoxEnd = index
            while (index < line.end && text[index] == ' ') index++
        }
    }
    val canonicalPrefix = if (rawCheckBox.isEmpty()) "$marker " else "$marker $rawCheckBox "
    return LinePrefixScan(
        indentEnd = indentEnd,
        contentStart = index,
        marker = marker,
        checkBox = checkBox,
        checkBoxEnd = checkBoxEnd,
        prefix = text.substring(indentEnd, index),
        canonicalPrefix = canonicalPrefix,
    )
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
