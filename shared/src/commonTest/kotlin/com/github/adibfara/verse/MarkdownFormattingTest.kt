package com.github.adibfara.verse

import com.github.adibfara.verse.core.markdown.FormatAction
import com.github.adibfara.verse.core.markdown.InlineFormat
import com.github.adibfara.verse.core.markdown.LineMarkerKind
import com.github.adibfara.verse.core.markdown.MarkdownParser
import com.github.adibfara.verse.core.markdown.MdRange
import com.github.adibfara.verse.core.markdown.SpecialSpanKind
import com.github.adibfara.verse.core.markdown.TextEdit
import com.github.adibfara.verse.core.markdown.applyFormatAction
import com.github.adibfara.verse.core.markdown.caretAfterTaskMarker
import com.github.adibfara.verse.core.markdown.caretTargetForTaskRange
import com.github.adibfara.verse.core.markdown.compositionThroughEdit
import com.github.adibfara.verse.core.markdown.continueListAfterNewline
import com.github.adibfara.verse.core.markdown.headingLevelAt
import com.github.adibfara.verse.core.markdown.inlineFormatsCovering
import com.github.adibfara.verse.core.markdown.insertedNewlineOffset
import com.github.adibfara.verse.core.markdown.lineMarkerKindAt
import com.github.adibfara.verse.core.markdown.specialSpanAt
import com.github.adibfara.verse.core.markdown.taskMarkerBackspaceEdit
import com.github.adibfara.verse.core.markdown.taskCaretRange
import com.github.adibfara.verse.core.markdown.imeBatchFixup
import com.github.adibfara.verse.feature.editor.ui.listContinuationCaret
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MarkdownFormattingTest {

    private val parser = MarkdownParser()

    private fun enter(line: String): TextEdit =
        applyFormatAction(
            text = line,
            selection = MdRange(line.length, line.length),
            action = FormatAction.ContinueList(line.length),
        )

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
            action = FormatAction.SetLineMarker(LineMarkerKind.Task),
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
            applyFormatAction("- milk", MdRange(0, 0), FormatAction.SetLineMarker(LineMarkerKind.Task)).text,
        )
        assertEquals(
            "- milk",
            applyFormatAction("- [ ] milk", MdRange(0, 0), FormatAction.SetLineMarker(LineMarkerKind.Bullet)).text,
        )
        assertEquals(
            "milk",
            applyFormatAction("- [x] milk", MdRange(0, 0), FormatAction.SetLineMarker(LineMarkerKind.None)).text,
        )
        assertEquals(
            "- [ ] milk",
            applyFormatAction("milk", MdRange(0, 0), FormatAction.SetLineMarker(LineMarkerKind.Task)).text,
        )
    }

    @Test
    fun lineMarkerKeepsIndentationAndOrderedMarkersReplaceCheckboxes() {
        assertEquals(
            "  1. step",
            applyFormatAction("  1. [x] step", MdRange(2, 2), FormatAction.SetLineMarker(LineMarkerKind.Ordered)).text,
        )
        assertEquals(
            "  1. step",
            applyFormatAction("  1. [x] step", MdRange(2, 2), FormatAction.SetLineMarker(LineMarkerKind.Bullet)).text,
        )
    }

    @Test
    fun settingOrderedOnABulletStartsCountingFromOne() {
        assertEquals(
            "1. milk",
            applyFormatAction("- milk", MdRange(0, 0), FormatAction.SetLineMarker(LineMarkerKind.Ordered)).text,
        )
    }

    @Test
    fun settingOrderedContinuesFromThePreviousSiblingNumber() {
        val text = "1. first\n- second"
        val edit = applyFormatAction(text, MdRange(9, 9), FormatAction.SetLineMarker(LineMarkerKind.Ordered))
        assertEquals("1. first\n2. second", edit.text)
    }

    @Test
    fun reapplyingALineMarkerTogglesItOff() {
        assertEquals(
            "step",
            applyFormatAction("3) step", MdRange(0, 0), FormatAction.SetLineMarker(LineMarkerKind.Ordered)).text,
        )
        assertEquals(
            "milk",
            applyFormatAction("- milk", MdRange(0, 0), FormatAction.SetLineMarker(LineMarkerKind.Bullet)).text,
        )
        assertEquals(
            "done",
            applyFormatAction("- [x] done", MdRange(0, 0), FormatAction.SetLineMarker(LineMarkerKind.Task)).text,
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
        val edit = enter("- first")
        assertEquals("- first\n- ", edit.text)
        assertEquals(MdRange(10, 10), edit.selection)
    }

    @Test
    fun enterContinuesAnOrderedListWithTheNextNumber() {
        val edit = enter("1. first")
        assertEquals("1. first\n2. ", edit.text)
    }

    @Test
    fun enterContinuesATaskAsUnchecked() {
        val edit = enter("- [x] done")
        assertEquals("- [x] done\n- [ ] ", edit.text)
    }

    @Test
    fun enterKeepsTheBulletCharacterAndTheIndentation() {
        val edit = enter("  * one")
        assertEquals("  * one\n  * ", edit.text)
        assertEquals(MdRange(12, 12), edit.selection)
    }

    @Test
    fun enterInTheMiddleOfAListItemSplitsItIntoTwoItems() {
        val edit = applyFormatAction("- item", MdRange(4, 4), FormatAction.ContinueList(4))
        assertEquals("- it\n- em", edit.text)
        assertEquals(MdRange(7, 7), edit.selection)
    }

    @Test
    fun enterOnAnEmptyListItemEndsTheListAndStartsANewLine() {
        val edit = applyFormatAction("- first\n- ", MdRange(10, 10), FormatAction.ContinueList(10))
        assertEquals("- first\n\n", edit.text)
        assertEquals(MdRange(9, 9), edit.selection)
    }

    @Test
    fun enterOnAnEmptyTaskItemEndsTheListAndStartsANewLine() {
        val edit = applyFormatAction("- [x] done\n- [ ] ", MdRange(17, 17), FormatAction.ContinueList(17))
        assertEquals("- [x] done\n\n", edit.text)
        assertEquals(MdRange(12, 12), edit.selection)
    }

    @Test
    fun enterOnAnEmptyOrderedItemEndsTheListAndStartsANewLine() {
        val edit = applyFormatAction("1. first\n2. ", MdRange(12, 12), FormatAction.ContinueList(12))
        assertEquals("1. first\n\n", edit.text)
        assertEquals(MdRange(10, 10), edit.selection)
    }

    @Test
    fun aSecondSpaceMakesAnEmptyItemIntoContentSoTheListContinues() {
        val edit = applyFormatAction("-  ", MdRange(3, 3), FormatAction.ContinueList(3))
        assertEquals("-  \n- ", edit.text)
        assertEquals(MdRange(6, 6), edit.selection)
    }

    @Test
    fun aSecondSpaceAfterACheckboxKeepsTheListGoingToo() {
        val edit = applyFormatAction("- [ ]  ", MdRange(7, 7), FormatAction.ContinueList(7))
        assertEquals("- [ ]  \n- [ ] ", edit.text)
        assertEquals(MdRange(14, 14), edit.selection)
    }

    @Test
    fun enterOnAPlainLineJustAddsANewline() {
        val edit = applyFormatAction("plain text", MdRange(10, 10), FormatAction.ContinueList(10))
        assertEquals("plain text\n", edit.text)
        assertEquals(MdRange(11, 11), edit.selection)
    }

    @Test
    fun enterOnASelectionLeavesTheTextAlone() {
        val edit = applyFormatAction("- first", MdRange(0, 7), FormatAction.ContinueList(7))
        assertEquals("- first", edit.text)
        assertEquals(MdRange(0, 7), edit.selection)
    }

    @Test
    fun aNewlineTheFieldAlreadyInsertedIsContinuedInPlace() {
        val edit = continueListAfterNewline("- first\n", newlineOffset = 7, selection = MdRange(8, 8))
        assertEquals("- first\n- ", edit.text)
        assertEquals(MdRange(10, 10), edit.selection)
    }

    @Test
    fun theMarkerGoesRightAfterTheNewlineEvenWhenTheImeTypedMore() {
        val edit = continueListAfterNewline("- milk \n", newlineOffset = 7, selection = MdRange(8, 8))
        assertEquals("- milk \n- ", edit.text)
        assertEquals(MdRange(10, 10), edit.selection)
    }

    @Test
    fun anAlreadyContinuedLineIsNotContinuedTwice() {
        val edit = continueListAfterNewline("- milk\n- ", newlineOffset = 6, selection = MdRange(8, 8))
        assertEquals("- milk\n- ", edit.text)
        assertEquals(MdRange(8, 8), edit.selection)
    }

    @Test
    fun anOffsetThatIsNotANewlineChangesNothing() {
        val edit = continueListAfterNewline("- first", newlineOffset = 3, selection = MdRange(7, 7))
        assertEquals("- first", edit.text)
        assertEquals(MdRange(7, 7), edit.selection)
    }

    @Test
    fun aPlainLineAboveTheNewlineIsLeftAlone() {
        val edit = continueListAfterNewline("plain text\n", newlineOffset = 10, selection = MdRange(11, 11))
        assertEquals("plain text\n", edit.text)
    }

    @Test
    fun theInsertedNewlineIsFoundWhateverTheImeCommittedWithIt() {
        assertEquals(6, insertedNewlineOffset("- milk", "- milk\n"))
        assertEquals(7, insertedNewlineOffset("- milk", "- milk \n"))
        assertEquals(6, insertedNewlineOffset("- milk", "- milk\na"))
        assertEquals(2, insertedNewlineOffset("a", "a\r\n"))
        assertEquals(0, insertedNewlineOffset("milk", "\nmilk"))
    }

    @Test
    fun anEditWithNoSingleNewlineIsNotANewline() {
        assertNull(insertedNewlineOffset("- milk", "- milk"))
        assertNull(insertedNewlineOffset("- milk", "- milk\n\n"))
        assertNull(insertedNewlineOffset("- milk\n", "- milk"))
        assertNull(insertedNewlineOffset("", ""))
    }

    private fun backspaceAfter(text: String, caret: Int): TextEdit =
        applyFormatAction(
            text = text,
            selection = MdRange(caret, caret),
            action = FormatAction.DeleteTaskMarker(caret),
        )

    @Test
    fun backspaceAfterACheckboxDeletesTheWholeTaskPrefix() {
        val edit = backspaceAfter("- [ ] milk", 5)
        assertEquals("milk", edit.text)
        assertEquals(MdRange(0, 0), edit.selection)
    }

    @Test
    fun backspaceAfterACheckedCheckboxDeletesTheWholeTaskPrefix() {
        val edit = backspaceAfter("- [x] milk", 5)
        assertEquals("milk", edit.text)
        assertEquals(MdRange(0, 0), edit.selection)
    }

    @Test
    fun backspaceAfterACheckboxKeepsTheIndentAndTheOrderedMarker() {
        val indented = backspaceAfter("  - [ ] milk", 7)
        assertEquals("  milk", indented.text)
        assertEquals(MdRange(2, 2), indented.selection)

        val ordered = backspaceAfter("1. [ ] milk", 6)
        assertEquals("milk", ordered.text)
        assertEquals(MdRange(0, 0), ordered.selection)
    }

    @Test
    fun backspaceAfterAnEmptyCheckboxEmptiesTheLine() {
        val edit = backspaceAfter("- [ ] ", 5)
        assertEquals("", edit.text)
        assertEquals(MdRange(0, 0), edit.selection)
    }

    @Test
    fun backspaceAnywhereButRightAfterTheCheckboxChangesNothing() {
        val inTheContent = backspaceAfter("- [ ] milk", 7)
        assertEquals("- [ ] milk", inTheContent.text)
        assertEquals(MdRange(7, 7), inTheContent.selection)

        val pastTheSpace = backspaceAfter("- [ ] milk", 6)
        assertEquals("milk", pastTheSpace.text)
        assertEquals(MdRange(0, 0), pastTheSpace.selection)

        val bullet = backspaceAfter("- milk", 2)
        assertEquals("- milk", bullet.text)
        assertEquals(MdRange(2, 2), bullet.selection)
    }

    @Test
    fun aCaretIsAfterATaskMarkerBetweenTheClosingBracketAndTheContent() {
        assertTrue(caretAfterTaskMarker("- [ ] milk", 5))
        assertTrue(caretAfterTaskMarker("- [x] milk", 5))
        assertTrue(caretAfterTaskMarker("- [ ]", 5))
        assertTrue(caretAfterTaskMarker("- [ ] milk", 6))
        assertFalse(caretAfterTaskMarker("- [ ] milk", 7))
        assertFalse(caretAfterTaskMarker("- milk", 2))
        assertFalse(caretAfterTaskMarker("- [ ] milk", 0))
    }

    @Test
    fun aCaretInsideACheckboxSnapsToTheSideItCameFrom() {
        val range = MdRange(2, 5)
        assertEquals(2, caretTargetForTaskRange(range, 3, previousCaret = 8, tappedAfterPlaceholder = null))
        assertEquals(2, caretTargetForTaskRange(range, 3, previousCaret = 5, tappedAfterPlaceholder = null))
        assertEquals(5, caretTargetForTaskRange(range, 3, previousCaret = 0, tappedAfterPlaceholder = null))
        assertEquals(5, caretTargetForTaskRange(range, 3, previousCaret = null, tappedAfterPlaceholder = null))
        assertEquals(5, caretTargetForTaskRange(range, 3, previousCaret = 8, tappedAfterPlaceholder = true))
        assertEquals(2, caretTargetForTaskRange(range, 3, previousCaret = 8, tappedAfterPlaceholder = false))
    }

    @Test
    fun theCaretAfterACheckboxLandsPastItsSpaceSoTypingKeepsTheTask() {
        val text = "- [ ] milk"
        val task = parser.parse(text).tasks.single()
        val range = taskCaretRange(text, task.range)
        assertEquals(MdRange(2, 6), range)
        assertEquals(6, caretTargetForTaskRange(range, 5, previousCaret = null, tappedAfterPlaceholder = true))
        assertEquals(2, caretTargetForTaskRange(range, 5, previousCaret = 6, tappedAfterPlaceholder = null))
        assertEquals(6, caretTargetForTaskRange(range, 5, previousCaret = 2, tappedAfterPlaceholder = null))
        val typed = text.substring(0, 6) + "x" + text.substring(6)
        assertEquals(1, parser.parse(typed).tasks.size)
        assertEquals(MdRange(2, 5), taskCaretRange("- [ ]", MdRange(2, 5)))
    }

    @Test
    fun anImeBatchThatInsertsANewlineIsContinuedAfterTheFact() {
        val text = "- [ ] milk"
        assertEquals(text.length, listContinuationCaret(TextFieldValue(text, selection = TextRange(text.length))))
        val fixup = imeBatchFixup(text, "$text\n", MdRange(11, 11))
        assertEquals("$text\n- [ ] ", fixup?.text)
        assertEquals(MdRange(17, 17), fixup?.selection)
        assertNull(imeBatchFixup(text, "$text!", MdRange(11, 11)))
        assertNull(imeBatchFixup("plain", "plain\n", MdRange(6, 6)))
    }

    @Test
    fun aCheckboxTheImeShortenedIsCompletedToTheWholePrefix() {
        val edit = taskMarkerBackspaceEdit(
            before = "- [ ] milk",
            after = "- [  milk",
            selection = MdRange(4, 4),
        )
        assertEquals("milk", edit!!.text)
        assertEquals(MdRange(0, 0), edit.selection)
    }

    @Test
    fun aCheckboxTheImeDeletedWholeIsCompletedToo() {
        val edit = taskMarkerBackspaceEdit(
            before = "- [ ] milk",
            after = "-  milk",
            selection = MdRange(2, 2),
        )
        assertEquals("milk", edit!!.text)
        assertEquals(MdRange(0, 0), edit.selection)
    }

    @Test
    fun anImeDeletionOutsideTheCheckboxChangesNothing() {
        assertNull(taskMarkerBackspaceEdit("- [ ] milk", "- [ ]milk", MdRange(5, 5)))
        assertNull(taskMarkerBackspaceEdit("- [ ] milk", "- [ ilk", MdRange(4, 4)))
        assertNull(taskMarkerBackspaceEdit("hello", "helo", MdRange(3, 3)))
        assertNull(taskMarkerBackspaceEdit("- milk", "- mil", MdRange(4, 4)))
        assertNull(taskMarkerBackspaceEdit("- [ ] milk", "- [ ] milk!", MdRange(9, 9)))
        assertNull(taskMarkerBackspaceEdit("- [ ] milk", "-  milk", MdRange(2, 4)))
    }

    @Test
    fun anEditAwayFromTheCompositionShiftsIt() {
        assertEquals(MdRange(2, 6), compositionThroughEdit("- milk\n", "- milk\n- ", MdRange(2, 6)))
        assertEquals(MdRange(4, 6), compositionThroughEdit("milk", "- milk", MdRange(2, 4)))
        assertEquals(MdRange(6, 10), compositionThroughEdit("- [ ] milk", "- [ ] milk!!", MdRange(6, 10)))
        assertEquals(MdRange(0, 4), compositionThroughEdit("- [ ] milk", "milk", MdRange(6, 10)))
    }

    @Test
    fun anEditOverlappingTheCompositionDropsIt() {
        assertNull(compositionThroughEdit("- [ ] milk", "milk", MdRange(2, 6)))
        assertNull(compositionThroughEdit("- [ ] milk", "- [x] milk", MdRange(2, 5)))
        assertNull(compositionThroughEdit("abc", "abd", MdRange(1, 3)))
    }

    @Test
    fun lineMarkerAppliesToTheWholeContiguousList() {
        val text = "- one\n- [ ] two\n- three"
        val edit = applyFormatAction(text, MdRange(6, 6), FormatAction.SetLineMarker(LineMarkerKind.None))
        assertEquals("one\ntwo\nthree", edit.text)
    }

    @Test
    fun aSelectionWhereNotEveryLineHasTheMarkerAddsItToAllOfThem() {
        val edit = applyFormatAction(
            text = "one\n- two\nthree",
            selection = MdRange(0, 15),
            action = FormatAction.SetLineMarker(LineMarkerKind.Task),
        )
        assertEquals("- [ ] one\n- [ ] two\n- [ ] three", edit.text)
    }

    @Test
    fun aSelectionWhereEveryLineHasTheMarkerRemovesIt() {
        val edit = applyFormatAction(
            text = "- [ ] one\n- [ ] two",
            selection = MdRange(0, 19),
            action = FormatAction.SetLineMarker(LineMarkerKind.Task),
        )
        assertEquals("one\ntwo", edit.text)
    }

    @Test
    fun aSelectionEndingAtALineStartLeavesThatLineAlone() {
        val edit = applyFormatAction(
            text = "- one\n- two",
            selection = MdRange(0, 6),
            action = FormatAction.SetLineMarker(LineMarkerKind.Task),
        )
        assertEquals("- [ ] one\n- two", edit.text)
    }

    @Test
    fun aSelectionAcrossBlankLinesSkipsThem() {
        val edit = applyFormatAction(
            text = "one\n\ntwo",
            selection = MdRange(0, 8),
            action = FormatAction.SetLineMarker(LineMarkerKind.Bullet),
        )
        assertEquals("- one\n\n- two", edit.text)
    }

    @Test
    fun aSelectionNumbersTheSelectedLinesInOrder() {
        val edit = applyFormatAction(
            text = "- [ ] a\n- b\nplain",
            selection = MdRange(0, 17),
            action = FormatAction.SetLineMarker(LineMarkerKind.Ordered),
        )
        assertEquals("1. a\n2. b\n3. plain", edit.text)
    }

    @Test
    fun aSelectionWhereEveryLineIsOrderedRemovesTheNumbers() {
        val edit = applyFormatAction(
            text = "1. a\n2. b",
            selection = MdRange(0, 9),
            action = FormatAction.SetLineMarker(LineMarkerKind.Ordered),
        )
        assertEquals("a\nb", edit.text)
    }

    @Test
    fun orderedOnACheckboxBlockDropsTheCheckboxesAndNumbersTheLines() {
        val edit = applyFormatAction(
            text = "- [ ] a\n- [ ] b",
            selection = MdRange(0, 0),
            action = FormatAction.SetLineMarker(LineMarkerKind.Ordered),
        )
        assertEquals("1. a\n2. b", edit.text)
    }

    @Test
    fun aSelectionApplyingTasksKeepsEachLinesCheckedState() {
        val edit = applyFormatAction(
            text = "- [x] done\n- plain",
            selection = MdRange(0, 18),
            action = FormatAction.SetLineMarker(LineMarkerKind.Task),
        )
        assertEquals("- [x] done\n- [ ] plain", edit.text)
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
    fun theHeadingLevelIsActiveWithTheCaretAtTheEndOfTheLine() {
        assertEquals(1, headingLevelAt(parser.parse("# ").styles, 2))
        assertEquals(3, headingLevelAt(parser.parse("### Title").styles, 9))
        val twoLines = parser.parse("# Title\nbody").styles
        assertEquals(1, headingLevelAt(twoLines, 7))
        assertEquals(0, headingLevelAt(twoLines, 8))
        assertEquals(0, headingLevelAt(twoLines, 12))
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
