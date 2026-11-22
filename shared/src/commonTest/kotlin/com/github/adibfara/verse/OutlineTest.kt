package com.github.adibfara.verse

import com.github.adibfara.verse.core.markdown.MarkdownParser
import com.github.adibfara.verse.core.markdown.anchorOffsetIn
import com.github.adibfara.verse.core.markdown.outlineOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OutlineTest {

    private val parser = MarkdownParser()

    @Test
    fun listsHeadingsInDocumentOrderWithLevels() {
        val outline = outlineOf(parser.parse("# One\n\ntext\n\n## Two\n\n### Three\n\n# Four\n"))
        assertEquals(listOf("One", "Two", "Three", "Four"), outline.map { it.title })
        assertEquals(listOf(1, 2, 3, 1), outline.map { it.level })
        assertTrue(outline.zipWithNext().all { (a, b) -> a.offset < b.offset })
    }

    @Test
    fun titlesDropMarkersAndEmphasis() {
        val outline = outlineOf(parser.parse("## A **bold** `code` title\n\nbody\n\n# Plain\n"))
        assertEquals("A bold code title", outline.first().title)
    }

    @Test
    fun emptyHeadingsAreSkipped() {
        val outline = outlineOf(parser.parse("# \n\n## Real\n"))
        assertEquals(listOf("Real"), outline.map { it.title })
    }

    @Test
    fun offsetPointsAtTheFirstVisibleCharacterAndMatchesAnchors() {
        val text = "intro\n\n## Second part\n\nbody\n"
        val document = parser.parse(text)
        val entry = outlineOf(document).single()
        assertEquals(text.indexOf("Second"), entry.offset)
        assertEquals(anchorOffsetIn(document, "#second-part"), entry.offset)
    }

    @Test
    fun codeBlockHashLinesAreNotHeadings() {
        val outline = outlineOf(parser.parse("```\n# not a heading\n```\n\n# Real\n"))
        assertEquals(listOf("Real"), outline.map { it.title })
    }
}
