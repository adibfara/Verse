package com.github.adibfara.verse

import com.github.adibfara.verse.core.markdown.FormatAction
import com.github.adibfara.verse.core.markdown.InlineFormat
import com.github.adibfara.verse.core.markdown.LineMarkerKind
import com.github.adibfara.verse.core.markdown.MarkdownParser
import com.github.adibfara.verse.core.markdown.MdRange
import com.github.adibfara.verse.core.markdown.SpecialSpanKind
import com.github.adibfara.verse.core.markdown.applyFormatAction
import com.github.adibfara.verse.core.markdown.headingLevelAt
import com.github.adibfara.verse.core.markdown.inlineFormatsCovering
import com.github.adibfara.verse.core.markdown.lineMarkerKindAt
import com.github.adibfara.verse.core.markdown.specialSpanAt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MarkdownFormattingTest {

    private val parser = MarkdownParser()

    @Test
    fun wrappingASelectionAddsMarkersAndKeepsTheSelectionOnTheContent() {
        val edit = applyFormatAction(
            text = "hello world",
            selection = MdRange(0, 5),
            action = FormatAction.ToggleInline(MdRange(0, 5), InlineFormat.Bold),
        )
        assertEquals("**hello** world", edit.text)
        assertEquals(MdRange(2, 7), edit.selection)
    }

    @Test
    fun wrappingAHoveredSpanLeavesTheCaretCollapsedWhereItWas() {
        val edit = applyFormatAction(
            text = "say loud now",
            selection = MdRange(6, 6),
            action = FormatAction.ToggleInline(MdRange(4, 8), InlineFormat.Bold),
        )
        assertEquals("say **loud** now", edit.text)
        assertEquals(MdRange(8, 8), edit.selection)
        assertEquals("say **lo", edit.text.substring(0, edit.selection.start))
    }

    @Test
    fun unwrappingAHoveredSpanLeavesTheCaretCollapsedWhereItWas() {
        val edit = applyFormatAction(
            text = "say **loud** now",
            selection = MdRange(8, 8),
            action = FormatAction.ToggleInline(MdRange(4, 12), InlineFormat.Bold),
        )
        assertEquals("say loud now", edit.text)
        assertEquals(MdRange(6, 6), edit.selection)
        assertEquals("say lo", edit.text.substring(0, edit.selection.start))
    }

    @Test
    fun headingAndMarkerChangesKeepTheCaretOnItsCharacter() {
        val heading = applyFormatAction(
            text = "Title",
            selection = MdRange(3, 3),
            action = FormatAction.SetHeading(0, 1),
        )
        assertEquals("# Title", heading.text)
        assertEquals("# Tit", heading.text.substring(0, heading.selection.start))

        val cleared = applyFormatAction(
            text = "## Title",
            selection = MdRange(5, 5),
            action = FormatAction.SetHeading(0, 0),
        )
        assertEquals("Title", cleared.text)
        assertEquals("Ti", cleared.text.substring(0, cleared.selection.start))

        val task = applyFormatAction(
            text = "- milk",
            selection = MdRange(4, 4),
            action = FormatAction.SetLineMarker(0, LineMarkerKind.Task),
        )
        assertEquals("- [ ] milk", task.text)
        assertEquals("- [ ] mi", task.text.substring(0, task.selection.start))
    }

    @Test
    fun aCaretInsideARemovedPrefixLandsAtTheContentStart() {
        val edit = applyFormatAction(
            text = "## Title",
            selection = MdRange(1, 1),
            action = FormatAction.SetHeading(0, 0),
        )
        assertEquals("Title", edit.text)
        assertEquals(MdRange(0, 0), edit.selection)
    }

    @Test
    fun togglingAConstructThatIncludesItsMarkersUnwrapsIt() {
        val edit = applyFormatAction(
            text = "**hello** world",
            selection = MdRange(2, 7),
            action = FormatAction.ToggleInline(MdRange(0, 9), InlineFormat.Bold),
        )
        assertEquals("hello world", edit.text)
        assertEquals(MdRange(0, 5), edit.selection)
    }

    @Test
    fun togglingASelectionSurroundedByMarkersUnwrapsIt() {
        val edit = applyFormatAction(
            text = "a ~~gone~~ b",
            selection = MdRange(4, 8),
            action = FormatAction.ToggleInline(MdRange(4, 8), InlineFormat.Strikethrough),
        )
        assertEquals("a gone b", edit.text)
        assertEquals(MdRange(2, 6), edit.selection)
    }

    @Test
    fun italicInsideBoldNestsInsteadOfStrippingOneStar() {
        val edit = applyFormatAction(
            text = "**hello**",
            selection = MdRange(2, 7),
            action = FormatAction.ToggleInline(MdRange(0, 9), InlineFormat.Italic),
        )
        assertEquals("***hello***", edit.text)
    }

    @Test
    fun headingLevelReplacesAnExistingPrefix() {
        assertEquals(
            "## Title",
            applyFormatAction("# Title", MdRange(0, 0), FormatAction.SetHeading(0, 2)).text,
        )
        assertEquals(
            "Title",
            applyFormatAction("### Title", MdRange(0, 0), FormatAction.SetHeading(0, 0)).text,
        )
        assertEquals(
            "# Title",
            applyFormatAction("Title", MdRange(0, 0), FormatAction.SetHeading(0, 1)).text,
        )
    }

    @Test
    fun lineMarkerCyclesBetweenTaskBulletAndNone() {
        assertEquals(
            "- [ ] milk",
            applyFormatAction("- milk", MdRange(0, 0), FormatAction.SetLineMarker(0, LineMarkerKind.Task)).text,
        )
        assertEquals(
            "- milk",
            applyFormatAction("- [ ] milk", MdRange(0, 0), FormatAction.SetLineMarker(0, LineMarkerKind.Bullet)).text,
        )
        assertEquals(
            "milk",
            applyFormatAction("- [x] milk", MdRange(0, 0), FormatAction.SetLineMarker(0, LineMarkerKind.None)).text,
        )
        assertEquals(
            "- [ ] milk",
            applyFormatAction("milk", MdRange(0, 0), FormatAction.SetLineMarker(0, LineMarkerKind.Task)).text,
        )
    }

    @Test
    fun lineMarkerKeepsIndentationAndOrderedMarkersAndCheckedState() {
        assertEquals(
            "  1. [x] step",
            applyFormatAction("  1. [x] step", MdRange(0, 0), FormatAction.SetLineMarker(2, LineMarkerKind.Ordered)).text,
        )
        assertEquals(
            "  1. step",
            applyFormatAction("  1. [x] step", MdRange(0, 0), FormatAction.SetLineMarker(2, LineMarkerKind.Bullet)).text,
        )
    }

    @Test
    fun settingOrderedOnABulletStartsCountingFromOne() {
        assertEquals(
            "1. milk",
            applyFormatAction("- milk", MdRange(0, 0), FormatAction.SetLineMarker(0, LineMarkerKind.Ordered)).text,
        )
    }

    @Test
    fun settingOrderedContinuesFromThePreviousSiblingNumber() {
        val text = "1. first\n- second"
        val edit = applyFormatAction(text, MdRange(0, 0), FormatAction.SetLineMarker(9, LineMarkerKind.Ordered))
        assertEquals("1. first\n2. second", edit.text)
    }

    @Test
    fun reapplyingALineMarkerTogglesItOff() {
        assertEquals(
            "step",
            applyFormatAction("3) step", MdRange(0, 0), FormatAction.SetLineMarker(0, LineMarkerKind.Ordered)).text,
        )
        assertEquals(
            "milk",
            applyFormatAction("- milk", MdRange(0, 0), FormatAction.SetLineMarker(0, LineMarkerKind.Bullet)).text,
        )
        assertEquals(
            "done",
            applyFormatAction("- [x] done", MdRange(0, 0), FormatAction.SetLineMarker(0, LineMarkerKind.Task)).text,
        )
    }

    @Test
    fun reapplyingAHeadingLevelTogglesItOff() {
        assertEquals(
            "Title",
            applyFormatAction("# Title", MdRange(0, 0), FormatAction.SetHeading(0, 1)).text,
        )
        assertEquals(
            "## Title",
            applyFormatAction("# Title", MdRange(0, 0), FormatAction.SetHeading(0, 2)).text,
        )
        assertEquals(
            "# Title",
            applyFormatAction("## Title", MdRange(0, 0), FormatAction.SetHeading(0, 1)).text,
        )
    }

    @Test
    fun aCollapsedCaretInsertsAMarkerPairAndSitsInsideIt() {
        val edit = applyFormatAction(
            text = "hello",
            selection = MdRange(5, 5),
            action = FormatAction.ToggleInline(MdRange(5, 5), InlineFormat.Bold),
        )
        assertEquals("hello****", edit.text)
        assertEquals(MdRange(7, 7), edit.selection)
    }

    @Test
    fun aCollapsedCaretBeforeAClosingRunStepsPastIt() {
        val edit = applyFormatAction(
            text = "**hi**",
            selection = MdRange(4, 4),
            action = FormatAction.ToggleInline(MdRange(4, 4), InlineFormat.Bold),
        )
        assertEquals("**hi**", edit.text)
        assertEquals(MdRange(6, 6), edit.selection)
    }

    @Test
    fun aCollapsedCaretInsideAnEmptyPairRemovesIt() {
        val edit = applyFormatAction(
            text = "hello****",
            selection = MdRange(7, 7),
            action = FormatAction.ToggleInline(MdRange(7, 7), InlineFormat.Bold),
        )
        assertEquals("hello", edit.text)
        assertEquals(MdRange(5, 5), edit.selection)
    }

    @Test
    fun orderedKindIsReadFromTheSourceLine() {
        val text = "1. first\n- second\nplain"
        assertEquals(LineMarkerKind.Ordered, lineMarkerKindAt(text, 0))
        assertEquals(LineMarkerKind.Bullet, lineMarkerKindAt(text, 10))
        assertEquals(LineMarkerKind.None, lineMarkerKindAt(text, 19))
    }

    @Test
    fun enterContinuesABulletList() {
        val edit = applyFormatAction("- first", MdRange(7, 7), FormatAction.ContinueList)
        assertEquals("- first\n- ", edit.text)
        assertEquals(MdRange(10, 10), edit.selection)
    }

    @Test
    fun enterContinuesAnOrderedListWithTheNextNumber() {
        val edit = applyFormatAction("1. first", MdRange(8, 8), FormatAction.ContinueList)
        assertEquals("1. first\n2. ", edit.text)
    }

    @Test
    fun enterContinuesATaskAsUnchecked() {
        val edit = applyFormatAction("- [x] done", MdRange(10, 10), FormatAction.ContinueList)
        assertEquals("- [x] done\n- [ ] ", edit.text)
    }

    @Test
    fun enterOnAnEmptyListItemEndsTheListAndStartsANewLine() {
        val edit = applyFormatAction("- first\n- ", MdRange(10, 10), FormatAction.ContinueList)
        assertEquals("- first\n\n", edit.text)
        assertEquals(MdRange(9, 9), edit.selection)
    }

    @Test
    fun enterOnAnEmptyTaskItemEndsTheListAndStartsANewLine() {
        val edit = applyFormatAction("- [x] done\n- [ ] ", MdRange(17, 17), FormatAction.ContinueList)
        assertEquals("- [x] done\n\n", edit.text)
        assertEquals(MdRange(12, 12), edit.selection)
    }

    @Test
    fun enterOnAnEmptyOrderedItemEndsTheListAndStartsANewLine() {
        val edit = applyFormatAction("1. first\n2. ", MdRange(12, 12), FormatAction.ContinueList)
        assertEquals("1. first\n\n", edit.text)
        assertEquals(MdRange(10, 10), edit.selection)
    }

    @Test
    fun enterDoesNothingOnAPlainLine() {
        val edit = applyFormatAction("plain text", MdRange(10, 10), FormatAction.ContinueList)
        assertEquals("plain text", edit.text)
    }

    @Test
    fun lineMarkerAppliesToTheWholeContiguousList() {
        val text = "- one\n- [ ] two\n- three"
        val edit = applyFormatAction(text, MdRange(0, 0), FormatAction.SetLineMarker(6, LineMarkerKind.None))
        assertEquals("one\ntwo\nthree", edit.text)
    }

    @Test
    fun markerKindIsReadFromTheSourceLine() {
        val text = "- [ ] task\n- bullet\nplain"
        assertEquals(LineMarkerKind.Task, lineMarkerKindAt(text, 0))
        assertEquals(LineMarkerKind.Bullet, lineMarkerKindAt(text, 12))
        assertEquals(LineMarkerKind.None, lineMarkerKindAt(text, 21))
    }

    @Test
    fun specialSpansAreFoundForEmphasisAndMarkersButNotPlainText() {
        val text = "plain **bold** text\n- item\n# Head"
        val document = parser.parse(text)
        val styles = document.styles

        assertNull(specialSpanAt(styles, text, 1))

        val bold = specialSpanAt(styles, text, text.indexOf("bold"))
        assertEquals(SpecialSpanKind.Inline, bold?.kind)
        assertEquals(MdRange(6, 14), bold?.range)

        val bullet = specialSpanAt(styles, text, text.indexOf("- item"))
        assertEquals(SpecialSpanKind.LineMarker, bullet?.kind)

        val heading = specialSpanAt(styles, text, text.indexOf("Head"))
        assertEquals(SpecialSpanKind.Inline, heading?.kind)
        assertEquals("Head", heading?.range?.let { text.substring(it.start, it.end) })
    }

    @Test
    fun activeFormatsAndHeadingLevelReflectTheSpanUnderTheCursor() {
        val text = "## A *mix* of **styles**"
        val document = parser.parse(text)
        val italic = specialSpanAt(document.styles, text, text.indexOf("mix"))!!

        assertTrue(InlineFormat.Italic in inlineFormatsCovering(document.styles, italic.range))
        assertTrue(InlineFormat.Bold !in inlineFormatsCovering(document.styles, italic.range))
        assertEquals(2, headingLevelAt(document.styles, italic.range.start))
    }

    @Test
    fun togglingBoldOnADetectedSpanRoundTripsThroughTheParser() {
        val text = "say **loud** now"
        val document = parser.parse(text)
        val span = specialSpanAt(document.styles, text, text.indexOf("loud"))!!
        val stripped = applyFormatAction(
            text = text,
            selection = span.range,
            action = FormatAction.ToggleInline(span.range, InlineFormat.Bold),
        )
        assertEquals("say loud now", stripped.text)

        val restored = applyFormatAction(
            text = stripped.text,
            selection = stripped.selection,
            action = FormatAction.ToggleInline(stripped.selection, InlineFormat.Bold),
        )
        assertEquals(text, restored.text)
        assertEquals(
            "loud",
            restored.text.substring(restored.selection.start, restored.selection.end),
        )
    }
}
