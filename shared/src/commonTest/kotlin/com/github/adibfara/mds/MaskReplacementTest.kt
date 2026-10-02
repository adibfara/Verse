package com.github.adibfara.mds

import com.github.adibfara.mds.core.markdown.MarkdownParser
import com.github.adibfara.mds.core.markdown.MaskEdit
import com.github.adibfara.mds.core.markdown.buildMaskedText
import com.github.adibfara.mds.core.markdown.lineRangeAt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val corpus = listOf(
    "- bullet one\n- bullet two\n",
    "- [ ] open\n- [x] done\n",
    "1. first\n2. second\n",
    "# Heading\n\n- [ ] task with **bold**\n",
    "* star bullet\n+ plus bullet\n",
    "    - indented\n        - deeper\n",
    "- [X] upper case done\n",
    "no lists here at all",
)

class MaskReplacementTest {

    private val parser = MarkdownParser()

    private fun maskedWith(text: String, revealedLine: com.github.adibfara.mds.core.markdown.MdRange?) =
        parser.parse(text).let { document ->
            buildMaskedText(
                text = text,
                edits = document.markers.map { MaskEdit(it, "") } + document.decorations,
                revealedLine = revealedLine,
            )
        }

    @Test
    fun bulletsBecomeGlyphsAndCheckboxesBecomeBoxes() {
        val masked = maskedWith("- bullet\n- [ ] open\n- [x] done\n", null)
        assertEquals("• bullet\n• ☐ open\n• ☑ done\n", masked.transformedText)
    }

    @Test
    fun orderedListsKeepTheirNumbers() {
        val masked = maskedWith("1. first\n2. second\n", null)
        assertEquals("1. first\n2. second\n", masked.transformedText)
    }

    @Test
    fun caretLineKeepsRawMarkdown() {
        val text = "- [ ] open\n- [x] done\n"
        val masked = maskedWith(text, lineRangeAt(text, 2))
        assertTrue(masked.transformedText.startsWith("- [ ] open"))
        assertTrue(masked.transformedText.contains("• ☑ done"))
    }

    @Test
    fun mappingStaysMonotonicAndInBoundsAcrossCorpus() {
        corpus.forEach { text ->
            for (caret in 0..text.length) {
                val mask = maskedWith(text, lineRangeAt(text, caret))
                val transformedLength = mask.transformedText.length

                var previous = -1
                for (offset in 0..text.length) {
                    val mapped = mask.originalToTransformed(offset)
                    assertTrue(
                        mapped in 0..transformedLength,
                        "forward out of bounds at $offset in <$text>",
                    )
                    assertTrue(mapped >= previous, "forward not monotonic at $offset in <$text>")
                    previous = mapped
                }

                var previousBack = -1
                for (offset in 0..transformedLength) {
                    val mapped = mask.transformedToOriginal(offset)
                    assertTrue(
                        mapped in 0..text.length,
                        "backward out of bounds at $offset in <$text>",
                    )
                    assertTrue(mapped >= previousBack, "backward not monotonic at $offset in <$text>")
                    previousBack = mapped
                }
            }
        }
    }

    @Test
    fun overlappingEditsDoNotCorruptTheMapping() {
        val text = "- [ ] task"
        val mask = buildMaskedText(
            text = text,
            edits = listOf(
                MaskEdit(com.github.adibfara.mds.core.markdown.MdRange(0, 2), "• "),
                MaskEdit(com.github.adibfara.mds.core.markdown.MdRange(1, 5), "X"),
                MaskEdit(com.github.adibfara.mds.core.markdown.MdRange(2, 5), "☐"),
            ),
            revealedLine = null,
        )
        assertEquals("• ☐ task", mask.transformedText)
        assertEquals(text.length, mask.transformedToOriginal(mask.transformedText.length))
    }
}
