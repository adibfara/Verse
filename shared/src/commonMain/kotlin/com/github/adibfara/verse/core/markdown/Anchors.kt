package com.github.adibfara.verse.core.markdown

fun isAnchorTarget(target: String): Boolean = target.trimStart().startsWith("#")

fun headingSlug(heading: String): String {
    val builder = StringBuilder(heading.length)
    heading.trim().lowercase().forEach { character ->
        when {
            character.isLetterOrDigit() || character == '-' || character == '_' ->
                builder.append(character)

            character == ' ' || character == '\t' -> builder.append('-')
            else -> Unit
        }
    }
    return builder.toString()
}

fun anchorOffsetIn(document: MdDocument, target: String): Int? {
    val slug = headingSlug(target.trim().removePrefix("#"))
    if (slug.isEmpty()) return null
    val text = document.text
    document.styles.forEach { styled ->
        if (!styled.kind.isHeading) return@forEach
        val enclosed = document.markers.filter { it.overlaps(styled.range) }
        val visible = visibleOffsets(text, styled.range, enclosed)
        if (headingSlug(visible.map(text::get).joinToString("")) != slug) return@forEach
        return visible.firstOrNull { !text[it].isWhitespace() } ?: styled.range.start
    }
    return null
}

private fun visibleOffsets(text: String, range: MdRange, markers: List<MdRange>): List<Int> {
    val end = range.end.coerceIn(0, text.length)
    var index = range.start.coerceIn(0, end)
    val offsets = mutableListOf<Int>()
    while (index < end) {
        val marker = markers.firstOrNull { it.start <= index && index < it.end }
        if (marker != null) {
            index = marker.end
            continue
        }
        offsets.add(index)
        index++
    }
    return offsets
}
