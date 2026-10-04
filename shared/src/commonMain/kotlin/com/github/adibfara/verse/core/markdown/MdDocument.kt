package com.github.adibfara.verse.core.markdown

data class MdRange(val start: Int, val end: Int) {
    val length: Int get() = end - start

    fun contains(offset: Int): Boolean = offset >= start && offset < end

    fun overlaps(other: MdRange): Boolean = start < other.end && other.start < end
}

enum class InlineStyle {
    Bold,
    Italic,
    Strikethrough,
}

sealed interface MdInline {
    val range: MdRange

    data class Text(val text: String, override val range: MdRange) : MdInline

    data class Styled(
        val style: InlineStyle,
        val children: List<MdInline>,
        override val range: MdRange,
    ) : MdInline

    data class Code(val code: String, override val range: MdRange) : MdInline

    data class Link(
        val target: String,
        val children: List<MdInline>,
        override val range: MdRange,
    ) : MdInline

    data class Image(
        val target: String,
        val alt: String,
        override val range: MdRange,
    ) : MdInline

    data class LineBreak(override val range: MdRange) : MdInline
}

data class MdListItem(
    val marker: String,
    val checked: Boolean?,
    val checkBoxRange: MdRange?,
    val content: List<MdBlock>,
    val range: MdRange,
)

sealed interface MdBlock {
    val range: MdRange

    data class Heading(
        val level: Int,
        val content: List<MdInline>,
        override val range: MdRange,
    ) : MdBlock

    data class Paragraph(
        val content: List<MdInline>,
        override val range: MdRange,
    ) : MdBlock

    data class ListBlock(
        val ordered: Boolean,
        val items: List<MdListItem>,
        override val range: MdRange,
    ) : MdBlock

    data class CodeBlock(
        val language: String?,
        val code: String,
        override val range: MdRange,
    ) : MdBlock

    data class Quote(
        val children: List<MdBlock>,
        override val range: MdRange,
    ) : MdBlock

    data class Table(
        val header: List<List<MdInline>>,
        val rows: List<List<List<MdInline>>>,
        override val range: MdRange,
    ) : MdBlock

    data class HorizontalRule(override val range: MdRange) : MdBlock
}

enum class StyleKind {
    Heading1,
    Heading2,
    Heading3,
    Heading4,
    Heading5,
    Heading6,
    Bold,
    Italic,
    Strikethrough,
    InlineCode,
    CodeBlock,
    Link,
    Quote,
    ListMarker,
    TaskMarker,
    TaskMarkerDone,
    TaskText,
    Table,
    TableHeader;

    val isHeading: Boolean get() = ordinal <= Heading6.ordinal

    val headingLevel: Int get() = if (isHeading) ordinal + 1 else 0

    companion object {
        fun heading(level: Int): StyleKind = when (level.coerceIn(1, 6)) {
            1 -> Heading1
            2 -> Heading2
            3 -> Heading3
            4 -> Heading4
            5 -> Heading5
            else -> Heading6
        }
    }
}

data class StyledRange(val range: MdRange, val kind: StyleKind)

const val TaskBoxPlaceholder = ' '

const val QuoteIndentSpaces = 3

const val CodeIndentSpaces = 2

data class MdTask(val range: MdRange, val checked: Boolean)

data class MdLink(val range: MdRange, val target: String)

data class MdCodeBlock(
    val range: MdRange,
    val headerRange: MdRange,
    val contentRange: MdRange,
    val language: String,
    val code: String,
)

data class MdTableDivider(
    val range: MdRange,
    val leadingSpaces: Int,
    val gutterSpaces: Int,
) {
    val replacementLength: Int get() = leadingSpaces + gutterSpaces
}

data class MdTableGrid(
    val range: MdRange,
    val headerRange: MdRange?,
    val rowRanges: List<MdRange>,
    val dividers: List<MdTableDivider>,
    val delimiterRange: MdRange?,
)

data class DocumentStats(val words: Int, val characters: Int)

data class MdDocument(
    val text: String,
    val blocks: List<MdBlock>,
    val styles: List<StyledRange>,
    val markers: List<MdRange>,
    val decorations: List<MaskEdit>,
    val tasks: List<MdTask>,
    val links: List<MdLink>,
    val codeBlocks: List<MdCodeBlock>,
    val rules: List<MdRange>,
    val quoteMarkers: List<MdRange>,
    val tables: List<MdTableGrid>,
    val stats: DocumentStats,
) {
    companion object {
        val Empty = MdDocument(
            text = "",
            blocks = emptyList(),
            styles = emptyList(),
            markers = emptyList(),
            decorations = emptyList(),
            tasks = emptyList(),
            links = emptyList(),
            codeBlocks = emptyList(),
            rules = emptyList(),
            quoteMarkers = emptyList(),
            tables = emptyList(),
            stats = DocumentStats(0, 0),
        )
    }
}

fun computeStats(text: String): DocumentStats {
    var words = 0
    var inWord = false
    text.forEach { character ->
        if (character.isWhitespace()) {
            inWord = false
        } else if (!inWord) {
            inWord = true
            words++
        }
    }
    return DocumentStats(words = words, characters = text.length)
}
