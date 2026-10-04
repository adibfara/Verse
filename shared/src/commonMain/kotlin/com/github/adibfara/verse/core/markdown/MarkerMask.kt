package com.github.adibfara.verse.core.markdown

data class MaskEdit(val range: MdRange, val replacement: String)

class MarkerMask(
    val originalText: String,
    val transformedText: String,
    private val originalToTransformedIndex: IntArray?,
    private val transformedToOriginalIndex: IntArray?,
) {

    val isIdentity: Boolean get() = originalToTransformedIndex == null

    fun originalToTransformed(offset: Int): Int {
        val clamped = offset.coerceIn(0, originalText.length)
        return originalToTransformedIndex?.get(clamped) ?: clamped
    }

    fun transformedToOriginal(offset: Int): Int {
        val clamped = offset.coerceIn(0, transformedText.length)
        return transformedToOriginalIndex?.get(clamped) ?: clamped
    }

    companion object {
        fun identity(text: String): MarkerMask = MarkerMask(text, text, null, null)
    }
}

fun lineRangeAt(text: String, offset: Int): MdRange {
    val clamped = offset.coerceIn(0, text.length)
    var start = clamped
    while (start > 0 && text[start - 1] != '\n') start--
    var end = clamped
    while (end < text.length && text[end] != '\n') end++
    return MdRange(start, end)
}

fun normalizeRanges(ranges: List<MdRange>, limit: Int): List<MdRange> {
    if (ranges.isEmpty()) return emptyList()
    val sanitized = ranges
        .mapNotNull { range ->
            val start = range.start.coerceIn(0, limit)
            val end = range.end.coerceIn(0, limit)
            if (end > start) MdRange(start, end) else null
        }
        .sortedBy { it.start }
    if (sanitized.isEmpty()) return emptyList()

    val merged = mutableListOf<MdRange>()
    var current = sanitized.first()
    sanitized.drop(1).forEach { range ->
        current = if (range.start <= current.end) {
            MdRange(current.start, maxOf(current.end, range.end))
        } else {
            merged.add(current)
            range
        }
    }
    merged.add(current)
    return merged
}

private fun normalizeEdits(
    edits: List<MaskEdit>,
    limit: Int,
    revealedLine: MdRange?,
): List<MaskEdit> {
    val sanitized = edits
        .mapNotNull { edit ->
            if (revealedLine != null && edit.range.overlaps(revealedLine)) return@mapNotNull null
            val start = edit.range.start.coerceIn(0, limit)
            val end = edit.range.end.coerceIn(0, limit)
            when {
                end <= start -> null
                start == edit.range.start && end == edit.range.end -> edit
                else -> MaskEdit(MdRange(start, end), edit.replacement)
            }
        }
        .sortedWith(compareBy({ it.range.start }, { -it.range.end }))

    val accepted = mutableListOf<MaskEdit>()
    var consumedUpTo = 0
    sanitized.forEach { edit ->
        if (edit.range.start >= consumedUpTo) {
            accepted.add(edit)
            consumedUpTo = edit.range.end
        }
    }
    return accepted
}

fun buildMarkerMask(
    text: String,
    markers: List<MdRange>,
    revealedLine: MdRange? = null,
): MarkerMask = buildMaskedText(
    text = text,
    edits = markers.map { MaskEdit(it, "") },
    revealedLine = revealedLine,
)

fun buildMaskedText(
    text: String,
    edits: List<MaskEdit>,
    revealedLine: MdRange? = null,
): MarkerMask {
    val applied = normalizeEdits(edits, text.length, revealedLine)
    if (applied.isEmpty()) return MarkerMask.identity(text)

    val length = text.length
    var transformedLength = length
    applied.forEach { edit ->
        transformedLength += edit.replacement.length - edit.range.length
    }

    val forward = IntArray(length + 1)
    val backward = IntArray(transformedLength + 1)
    val builder = StringBuilder(transformedLength)
    var written = 0
    var position = 0

    applied.forEach { edit ->
        val keepEnd = edit.range.start
        if (position < keepEnd) {
            builder.appendRange(text, position, keepEnd)
            while (position < keepEnd) {
                forward[position] = written
                backward[written] = position
                written++
                position++
            }
        }
        val transformedStart = written
        if (edit.replacement.isNotEmpty()) {
            builder.append(edit.replacement)
            repeat(edit.replacement.length) {
                backward[written] = edit.range.start
                written++
            }
        }
        while (position < edit.range.end) {
            forward[position] = transformedStart
            position++
        }
    }
    if (position < length) {
        builder.appendRange(text, position, length)
        while (position < length) {
            forward[position] = written
            backward[written] = position
            written++
            position++
        }
    }
    forward[length] = written
    backward[written] = length

    return MarkerMask(
        originalText = text,
        transformedText = builder.toString(),
        originalToTransformedIndex = forward,
        transformedToOriginalIndex = backward,
    )
}
