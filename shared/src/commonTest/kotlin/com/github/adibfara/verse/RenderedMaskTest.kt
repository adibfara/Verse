package com.github.adibfara.verse

import com.github.adibfara.verse.core.markdown.MarkdownParser
import com.github.adibfara.verse.core.markdown.MdRange
import com.github.adibfara.verse.core.markdown.buildMaskedText
import com.github.adibfara.verse.core.markdown.maskEditsFor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val corpus = listOf(
    "# Heading\n\nsome **bold** and *italic* text\n",
    "#",
    "# ",
    "## \n\nparagraph\n",
    "**bold** at the start\n",
    "a `code` span and ~~gone~~\n",
    "> quoted line\n> second\n",
    "> \n",
    "[label](target.md) and ![alt](image.png)\n",
    "[]()\n",
    "```kotlin\nval a = 1\n```\n",
    "- [ ] open\n- [x] done\n",
    "plain text only",
    "before\n\n---\n\nafter\n",
    "---",
    "| a | b |\n| --- | --- |\n| 1 | 22 |\n",
    "| **a** | b |\n| :-- | --: |\n|1|2|\n",
    "|a|\n|-|\n",
    "> quote with **bold**\n>\n> second paragraph\n",
    ">> nested\n",
    "```\nplain fence\n```\n",
)

class RenderedMaskTest {

    private val parser = MarkdownParser()

    private fun rendered(text: String): String {
        val document = parser.parse(text)
        return buildMaskedText(text, maskEditsFor(document), null).transformedText
    }

    @Test
    fun headingMarkersAreHiddenEvenWithoutCaretReveal() {
        assertEquals("Hi", rendered("# Hi"))
    }

    @Test
    fun headingWithoutContentKeepsItsMarker() {
        assertEquals("# ", rendered("# "))
        assertEquals("#", rendered("#"))
        assertEquals("## \n\nbody", rendered("## \n\nbody"))
    }

    @Test
    fun emphasisMarkersAreHiddenWhenThereIsContent() {
        assertEquals("bold and italic", rendered("**bold** and *italic*"))
    }

    @Test
    fun thematicBreaksHideTheirMarkersBecauseARuleIsDrawnInstead() {
        assertEquals("a\n\n\n\nb", rendered("a\n\n---\n\nb"))
        assertEquals("", rendered("** **"))
        assertEquals(listOf(MdRange(3, 6)), parser.parse("a\n\n---\n\nb").rules)
    }

    @Test
    fun emptyInlineCodeKeepsItsBackticks() {
        assertEquals("` `", rendered("` `"))
    }

    @Test
    fun emptyQuoteKeepsItsMarker() {
        assertEquals("> ", rendered("> "))
    }

    @Test
    fun emptyLinkKeepsItsPunctuation() {
        assertEquals("[]()", rendered("[]()"))
    }

    @Test
    fun linkWithLabelShowsOnlyTheLabel() {
        assertEquals("label", rendered("[label](target.md)"))
    }

    @Test
    fun mappingStaysMonotonicAndInBoundsAcrossCorpus() {
        corpus.forEach { text ->
            val document = parser.parse(text)
            val mask = buildMaskedText(text, maskEditsFor(document), null)
            val transformedLength = mask.transformedText.length
            var previous = -1
            for (offset in 0..text.length) {
                val mapped = mask.originalToTransformed(offset)
                assertTrue(mapped in 0..transformedLength, "forward out of bounds in \"$text\"")
                assertTrue(mapped >= previous, "forward not monotonic in \"$text\"")
                previous = mapped
            }
            previous = -1
            for (offset in 0..transformedLength) {
                val mapped = mask.transformedToOriginal(offset)
                assertTrue(mapped in 0..text.length, "backward out of bounds in \"$text\"")
                assertTrue(mapped >= previous, "backward not monotonic in \"$text\"")
                previous = mapped
            }
        }
    }

    @Test
    fun typingThroughAHeadingRevealsAndHidesTheMarker() {
        val steps = listOf("#" to "#", "# " to "# ", "# H" to "H", "# Hi" to "Hi")
        steps.forEach { (source, expected) ->
            assertEquals(expected, rendered(source), "unexpected rendering of \"$source\"")
        }
    }

    @Test
    fun revealedLineStillWinsOverHiding() {
        val text = "# Hi\n\nbody"
        val document = parser.parse(text)
        val mask = buildMaskedText(text, maskEditsFor(document), MdRange(0, 4))
        assertTrue(mask.transformedText.startsWith("# Hi"))
    }
}
