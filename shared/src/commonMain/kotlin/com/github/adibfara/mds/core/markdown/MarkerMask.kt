package com.github.adibfara.mds.core.markdown

data class MaskEdit(val range: MdRange, val replacement: String)

class MarkerMask(
    val originalText: String,
    val transformedText: String,
    private val originalToTransformedIndex: IntArray,
    private val transformedToOriginalIndex: IntArray,
) {

    val isIdentity: Boolean get() = originalText == transformedText

    fun originalToTransformed(offset: Int): Int {
        val clamped = offset.coerceIn(0, originalText.length)
        return originalToTransformedIndex[clamped]
    }

    fun transformedToOriginal(offset: Int): Int {
        val clamped = offset.coerceIn(0, transformedText.length)
        return transformedToOriginalIndex[clamped]
    }

    companion object {
        fun identity(text: String): MarkerMask {
            val forward = IntArray(text.length + 1) { it }
            val backward = IntArray(text.length + 1) { it }
            return MarkerMask(text, text, forward, backward)
        }
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

private fun normalizeEdits(edits: List<MaskEdit>, limit: Int): List<MaskEdit> {
    val sanitized = edits
        .mapNotNull { edit ->
            val start = edit.range.start.coerceIn(0, limit)
            val end = edit.range.end.coerceIn(0, limit)
            if (end > start) MaskEdit(MdRange(start, end), edit.replacement) else null
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
    val candidates = edits.filterNot { edit ->
        revealedLine != null && edit.range.overlaps(revealedLine)
    }
    val applied = normalizeEdits(candidates, text.length)
    if (applied.isEmpty()) return MarkerMask.identity(text)

    val length = text.length
    val forward = IntArray(length + 1)
    val backward = ArrayList<Int>(length + 1)
    val builder = StringBuilder(length)

    var position = 0
    applied.forEach { edit ->
        while (position < edit.range.start) {
            forward[position] = builder.length
            backward.add(position)
            builder.append(text[position])
            position++
        }
        val transformedStart = builder.length
        edit.replacement.forEach { character ->
            backward.add(edit.range.start)
            builder.append(character)
        }
        while (position < edit.range.end) {
            forward[position] = transformedStart
            position++
        }
    }
    while (position < length) {
        forward[position] = builder.length
        backward.add(position)
        builder.append(text[position])
        position++
    }
    forward[length] = builder.length
    backward.add(length)

    return MarkerMask(
        originalText = text,
        transformedText = builder.toString(),
        originalToTransformedIndex = forward,
        transformedToOriginalIndex = backward.toIntArray(),
    )
}
