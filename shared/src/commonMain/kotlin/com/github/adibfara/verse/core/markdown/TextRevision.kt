package com.github.adibfara.verse.core.markdown

data class TextRevision(val start: Int, val removedEnd: Int, val insertedEnd: Int) {

    val delta: Int get() = insertedEnd - removedEnd

    fun shift(range: MdRange): MdRange? = when {
        range.end <= start -> range
        range.start >= removedEnd -> MdRange(range.start + delta, range.end + delta)
        else -> null
    }

    fun stretch(range: MdRange): MdRange? = when {
        start >= range.start && removedEnd <= range.end ->
            MdRange(range.start, range.end + delta)

        else -> shift(range)
    }
}

fun textRevisionBetween(before: String, after: String): TextRevision? {
    if (before == after) return null
    val shortest = minOf(before.length, after.length)
    var prefix = 0
    while (prefix < shortest && before[prefix] == after[prefix]) prefix++
    var suffix = 0
    val remaining = minOf(before.length - prefix, after.length - prefix)
    while (
        suffix < remaining &&
        before[before.length - 1 - suffix] == after[after.length - 1 - suffix]
    ) {
        suffix++
    }
    return TextRevision(
        start = prefix,
        removedEnd = before.length - suffix,
        insertedEnd = after.length - suffix,
    )
}
