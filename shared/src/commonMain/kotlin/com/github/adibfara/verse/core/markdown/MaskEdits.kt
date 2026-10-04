package com.github.adibfara.verse.core.markdown

private val ContentBearingKinds = setOf(
    StyleKind.Heading1,
    StyleKind.Heading2,
    StyleKind.Heading3,
    StyleKind.Heading4,
    StyleKind.Heading5,
    StyleKind.Heading6,
    StyleKind.Bold,
    StyleKind.Italic,
    StyleKind.Strikethrough,
    StyleKind.InlineCode,
    StyleKind.CodeBlock,
    StyleKind.Link,
    StyleKind.Quote,
)

private val QuoteIndent = " ".repeat(QuoteIndentSpaces)

fun maskEditsFor(document: MdDocument): List<MaskEdit> {
    if (document.markers.isEmpty()) return document.decorations
    val revealed = emptyConstructMarkers(document)
    val hidden = if (revealed.isEmpty()) {
        document.markers
    } else {
        document.markers.filterNot { it in revealed }
    }
    val quotes = document.quoteMarkers.toSet()
    return hidden.map { MaskEdit(it, if (it in quotes) QuoteIndent else "") } + document.decorations
}

private fun emptyConstructMarkers(document: MdDocument): Set<MdRange> {
    val revealed = mutableSetOf<MdRange>()
    document.styles.forEach { styled ->
        if (styled.kind !in ContentBearingKinds) return@forEach
        val enclosed = markersWithin(document.markers, styled.range)
        if (enclosed.isEmpty()) return@forEach
        if (hasVisibleContent(document.text, styled.range, enclosed)) return@forEach
        revealed.addAll(enclosed)
    }
    return revealed
}

private fun markersWithin(markers: List<MdRange>, range: MdRange): List<MdRange> {
    var index = firstMarkerFrom(markers, range.start)
    val enclosed = mutableListOf<MdRange>()
    while (index < markers.size && markers[index].start < range.end) {
        if (markers[index].end > range.start) enclosed.add(markers[index])
        index++
    }
    return enclosed
}

private fun firstMarkerFrom(markers: List<MdRange>, start: Int): Int {
    var low = 0
    var high = markers.size
    while (low < high) {
        val middle = (low + high) / 2
        if (markers[middle].start < start) low = middle + 1 else high = middle
    }
    return low
}

private fun hasVisibleContent(
    text: String,
    range: MdRange,
    markers: List<MdRange>,
): Boolean {
    val end = range.end.coerceIn(0, text.length)
    var index = range.start.coerceIn(0, end)
    var markerIndex = 0
    while (index < end) {
        while (markerIndex < markers.size && markers[markerIndex].end <= index) markerIndex++
        val marker = markers.getOrNull(markerIndex)
        if (marker != null && index >= marker.start) {
            index = marker.end
            continue
        }
        if (!text[index].isWhitespace()) return true
        index++
    }
    return false
}
