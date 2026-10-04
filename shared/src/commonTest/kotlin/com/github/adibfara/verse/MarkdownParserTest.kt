package com.github.adibfara.verse

import com.github.adibfara.verse.core.markdown.InlineStyle
import com.github.adibfara.verse.core.markdown.MarkdownParser
import com.github.adibfara.verse.core.markdown.MdBlock
import com.github.adibfara.verse.core.markdown.MdInline
import com.github.adibfara.verse.core.markdown.StyleKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MarkdownParserTest {

    private val parser = MarkdownParser()

    private fun flattenText(inlines: List<MdInline>): String = inlines.joinToString("") { inline ->
        when (inline) {
            is MdInline.Text -> inline.text
            is MdInline.Styled -> flattenText(inline.children)
            is MdInline.Link -> flattenText(inline.children)
            is MdInline.Code -> inline.code
            is MdInline.Image -> inline.alt
            is MdInline.LineBreak -> "\n"
        }
    }

    @Test
    fun parsesHeadingLevels() {
        val document = parser.parse("# One\n\n## Two\n\n###### Six")
        val headings = document.blocks.filterIsInstance<MdBlock.Heading>()
        assertEquals(listOf(1, 2, 6), headings.map { it.level })
        assertEquals("One", flattenText(headings[0].content).trim())
    }

    @Test
    fun parsesEmphasisAndStrong() {
        val document = parser.parse("plain *italic* and **bold** done")
        val paragraph = document.blocks.filterIsInstance<MdBlock.Paragraph>().single()
        val styles = paragraph.content.filterIsInstance<MdInline.Styled>().map { it.style }
        assertTrue(InlineStyle.Italic in styles)
        assertTrue(InlineStyle.Bold in styles)
    }

    @Test
    fun parsesNestedBulletLists() {
        val document = parser.parse("- Bullets\n    - bullets\n    - bullet 2\n")
        val list = document.blocks.filterIsInstance<MdBlock.ListBlock>().single()
        assertEquals(false, list.ordered)
        assertEquals(1, list.items.size)
        val nested = list.items.single().content.filterIsInstance<MdBlock.ListBlock>().single()
        assertEquals(2, nested.items.size)
    }

    @Test
    fun parsesTaskItems() {
        val document = parser.parse("- [ ] open task\n- [x] done task\n")
        val list = document.blocks.filterIsInstance<MdBlock.ListBlock>().single()
        assertEquals(listOf(false, true), list.items.map { it.checked })
    }

    @Test
    fun parsesFencedCodeWithLanguage() {
        val document = parser.parse("```kotlin\nval a = 1\n```\n")
        val code = document.blocks.filterIsInstance<MdBlock.CodeBlock>().single()
        assertEquals("kotlin", code.language)
        assertEquals("val a = 1", code.code)
    }

    @Test
    fun parsesLinksAndImages() {
        val document = parser.parse("[text](https://example.com) and ![alt](pic.png)")
        val paragraph = document.blocks.filterIsInstance<MdBlock.Paragraph>().single()
        val link = paragraph.content.filterIsInstance<MdInline.Link>().firstOrNull()
        assertNotNull(link)
        assertEquals("https://example.com", link.target)
        val image = paragraph.content.filterIsInstance<MdInline.Image>().firstOrNull()
        assertNotNull(image)
        assertEquals("pic.png", image.target)
        assertEquals("alt", image.alt)
    }

    @Test
    fun parsesBlockQuote() {
        val document = parser.parse("> quoted\n")
        assertEquals(1, document.blocks.filterIsInstance<MdBlock.Quote>().size)
    }

    @Test
    fun parsesTable() {
        val document = parser.parse("| a | b |\n| --- | --- |\n| 1 | 2 |\n")
        val table = document.blocks.filterIsInstance<MdBlock.Table>().singleOrNull()
        assertNotNull(table)
        assertEquals(2, table.header.size)
        assertEquals(1, table.rows.size)
    }

    @Test
    fun emitsHeadingStyleCoveringTheHeading() {
        val document = parser.parse("## Test subtitle")
        val heading = document.styles.first { it.kind == StyleKind.Heading2 }
        assertEquals(0, heading.range.start)
        assertEquals(16, heading.range.end)
    }

    @Test
    fun computesWordAndCharacterCounts() {
        val document = parser.parse("one two three")
        assertEquals(3, document.stats.words)
        assertEquals(13, document.stats.characters)
    }

    @Test
    fun emptyTextParsesToEmptyDocument() {
        val document = parser.parse("")
        assertTrue(document.blocks.isEmpty())
        assertTrue(document.markers.isEmpty())
    }
}
