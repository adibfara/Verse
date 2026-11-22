package com.github.adibfara.verse.core.markdown

data class OutlineEntry(val level: Int, val title: String, val offset: Int)

fun outlineOf(document: MdDocument): List<OutlineEntry> {
    val text = document.text
    return document.styles
        .filter { it.kind.isHeading }
        .sortedBy { it.range.start }
        .mapNotNull { styled ->
            val visible = headingVisibleOffsets(document, styled.range)
            val title = visible.map(text::get).joinToString("").trim()
            if (title.isEmpty()) {
                null
            } else {
                OutlineEntry(
                    level = styled.kind.headingLevel,
                    title = title,
                    offset = visible.firstOrNull { !text[it].isWhitespace() } ?: styled.range.start,
                )
            }
        }
}
