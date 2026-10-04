package com.github.adibfara.verse

import com.github.adibfara.verse.core.markdown.MarkdownParser
import com.github.adibfara.verse.core.markdown.MdRange
import com.github.adibfara.verse.core.markdown.buildMarkerMask
import com.github.adibfara.verse.core.markdown.lineRangeAt
import com.github.adibfara.verse.core.markdown.normalizeRanges
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val corpus = listOf(
    "",
    "plain text with no markdown at all",
    "# Heading one",
    "### Heading three\n\nbody text\n",
    "**bold** and *italic* and ***both***",
    "- bullet\n- another\n    - nested\n",
    "- [ ] task\n- [x] done\n",
    "`inline code` plus ```\nfenced\ncode\n```",
    "[a link](https://example.com) and ![image](pic.png)",
    "> quoted line\n> second line\n",
    "| a | b |\n| --- | --- |\n| 1 | 2 |\n",
    "~~struck~~ text",
    "#not a heading\n**unclosed bold\n*x*",
    "mixed **bold `code` inside** and [link *em*](u)",
)

class MarkerMaskTest {

    private val parser = MarkdownParser()

    @Test
    fun maskRoundTripsEveryCaretPositionAcrossCorpus() {
        corpus.forEach { text ->
            val markers = parser.parse(text).markers
            for (caret in 0..text.length) {
                val mask = buildMarkerMask(text, markers, lineRangeAt(text, caret))
                val transformedLength = mask.transformedText.length

                assertTrue(
                    transformedLength <= text.length,
                    "transformed longer than original for caret $caret in <$text>",
                )

                var previous = -1
                for (offset in 0..text.length) {
                    val mapped = mask.originalToTransformed(offset)
                    assertTrue(
                        mapped in 0..transformedLength,
                        "forward out of bounds at $offset for <$text>",
                    )
                    assertTrue(mapped >= previous, "forward not monotonic at $offset for <$text>")
                    previous = mapped
                }

                var previousBack = -1
                for (offset in 0..transformedLength) {
                    val mapped = mask.transformedToOriginal(offset)
                    assertTrue(
                        mapped in 0..text.length,
                        "backward out of bounds at $offset for <$text>",
                    )
                    assertTrue(mapped > previousBack, "backward not increasing at $offset for <$text>")
                    previousBack = mapped
                }

                for (offset in 0..transformedLength) {
                    val original = mask.transformedToOriginal(offset)
                    assertEquals(
                        offset,
                        mask.originalToTransformed(original),
                        "round trip failed at transformed $offset for <$text>",
                    )
                }
            }
        }
    }

    @Test
    fun maskHidesHeadingMarkerAndKeepsContent() {
        val text = "## Title\n\nbody"
        val markers = parser.parse(text).markers
        val mask = buildMarkerMask(text, markers, revealedLine = null)
        assertEquals("Title\n\nbody", mask.transformedText)
    }

    @Test
    fun maskRevealsMarkersOnTheCaretLine() {
        val text = "## Title\n\nbody"
        val markers = parser.parse(text).markers
        val mask = buildMarkerMask(text, markers, revealedLine = lineRangeAt(text, 2))
        assertEquals(text, mask.transformedText)
    }

    @Test
    fun maskHidesEmphasisMarkers() {
        val text = "a **b** c"
        val markers = parser.parse(text).markers
        val mask = buildMarkerMask(text, markers, revealedLine = null)
        assertEquals("a b c", mask.transformedText)
    }

    @Test
    fun identityMaskUsedWhenNothingHidden() {
        val text = "nothing to hide"
        val mask = buildMarkerMask(text, emptyList(), revealedLine = null)
        assertEquals(text, mask.transformedText)
        assertEquals(4, mask.originalToTransformed(4))
        assertEquals(4, mask.transformedToOriginal(4))
    }

    @Test
    fun lineRangeCoversTheWholeLine() {
        val text = "first\nsecond\nthird"
        assertEquals(MdRange(0, 5), lineRangeAt(text, 0))
        assertEquals(MdRange(6, 12), lineRangeAt(text, 8))
        assertEquals(MdRange(13, 18), lineRangeAt(text, 18))
    }

    @Test
    fun normalizeMergesOverlappingRanges() {
        val merged = normalizeRanges(
            listOf(MdRange(5, 8), MdRange(0, 3), MdRange(2, 6), MdRange(20, 10)),
            limit = 30,
        )
        assertEquals(listOf(MdRange(0, 8)), merged)
    }

    @Test
    fun normalizeClampsToTextLength() {
        val merged = normalizeRanges(listOf(MdRange(-4, 100)), limit = 10)
        assertEquals(listOf(MdRange(0, 10)), merged)
    }
}
