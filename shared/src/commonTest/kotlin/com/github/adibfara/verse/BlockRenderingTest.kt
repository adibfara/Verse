package com.github.adibfara.verse

import com.github.adibfara.verse.core.markdown.CodeIndentSpaces
import com.github.adibfara.verse.core.markdown.MarkdownParser
import com.github.adibfara.verse.core.markdown.QuoteIndentSpaces
import com.github.adibfara.verse.core.markdown.anchorOffsetIn
import com.github.adibfara.verse.core.markdown.buildMaskedText
import com.github.adibfara.verse.core.markdown.headingSlug
import com.github.adibfara.verse.core.markdown.isAnchorTarget
import com.github.adibfara.verse.core.markdown.maskEditsFor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BlockRenderingTest {

    private val parser = MarkdownParser()

    private fun rendered(text: String): String {
        val document = parser.parse(text)
        return buildMaskedText(text, maskEditsFor(document), null).transformedText
    }

    private val quoteIndent = " ".repeat(QuoteIndentSpaces)
    private val codeIndent = " ".repeat(CodeIndentSpaces)

    @Test
    fun quoteLinesAreIndentedByAFixedAmount() {
        assertEquals(
            "${quoteIndent}first line\n${quoteIndent}second line",
            rendered("> first line\n> second line"),
        )
    }

    @Test
    fun emptyQuoteStillKeepsItsMarker() {
        assertEquals("> ", rendered("> "))
    }

    @Test
    fun nestedQuotesIndentOncePerLevel() {
        assertEquals("${quoteIndent}${quoteIndent}deep", rendered(">> deep"))
    }

    @Test
    fun codeFenceLinesGainLeadingPadding() {
        assertEquals(
            "\n${codeIndent}val a = 1\n${codeIndent}val b = 2\n",
            rendered("```kotlin\nval a = 1\nval b = 2\n```"),
        )
    }

    @Test
    fun emptyCodeFenceIsNotIndented() {
        assertEquals("```\n```", rendered("```\n```"))
    }

    @Test
    fun tableCellsArePaddedToAConsistentColumnWidth() {
        val lines = rendered("| a | bb |\n| --- | --- |\n| ccc | d |\n").lines()
        assertEquals("  a     bb  ", lines[0])
        assertEquals("  ccc   d   ", lines[1])
        assertEquals(lines[0].length, lines[1].length)
    }

    @Test
    fun tableDelimiterRowCollapsesWithItsLineBreak() {
        val text = "| a | b |\n| --- | --- |\n| c | d |\n"
        assertEquals(3, rendered(text).lines().size)
    }

    @Test
    fun tableColumnsStayAlignedWhenCellsCarryHiddenMarkers() {
        val lines = rendered("| **ab** | x |\n| --- | --- |\n| cdef | y |\n").lines()
        assertEquals(lines[1].indexOf('y'), lines[0].indexOf('x'))
    }

    @Test
    fun tableGridRecordsOneDividerPerColumnBoundary() {
        val table = parser.parse("| a | b |\n| --- | --- |\n| c | d |\n").tables.single()
        assertEquals(3, table.dividers.size)
        assertEquals(2, table.rowRanges.size)
        assertTrue(table.headerRange != null)
    }

    @Test
    fun headingSlugsFollowTheUsualAnchorSpelling() {
        assertEquals("sample-heading", headingSlug("Sample Heading!"))
        assertEquals("a-b-c", headingSlug("A, B, C"))
        assertEquals("keep_underscores-1", headingSlug("Keep_Underscores 1"))
    }

    @Test
    fun anchorTargetsResolveToTheHeadingContent() {
        val text = "# Top\n\n[go](#sample-heading)\n\n## Sample Heading!\n\nbody\n"
        val document = parser.parse(text)
        assertTrue(isAnchorTarget("#sample-heading"))
        assertEquals(text.indexOf("Sample Heading!"), anchorOffsetIn(document, "#sample-heading"))
        assertEquals(text.indexOf("Top"), anchorOffsetIn(document, "#top"))
        assertNull(anchorOffsetIn(document, "#missing"))
    }

    @Test
    fun relativeTargetsAreNotTreatedAsAnchors() {
        assertTrue(!isAnchorTarget("note.md"))
        assertTrue(!isAnchorTarget("https://example.com/#x"))
    }
}
