package com.github.adibfara.verse

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.github.adibfara.verse.feature.editor.EditHistory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EditHistoryTest {

    private var time = 0L
    private val history = EditHistory(now = { time })

    private fun value(text: String) = TextFieldValue(text, TextRange(text.length))

    private fun type(vararg steps: String) {
        steps.toList().zipWithNext().forEach { (before, after) ->
            history.record(value(before), value(after))
            time += 100
        }
    }

    @Test
    fun quickTypingUndoesAsOneStep() {
        type("", "h", "he", "hel")
        assertEquals("", history.undo(value("hel"))?.text)
        assertFalse(history.canUndo)
    }

    @Test
    fun aPauseStartsANewStep() {
        type("", "a", "ab")
        time += 5000
        type("ab", "abc")
        assertEquals("ab", history.undo(value("abc"))?.text)
        assertEquals("", history.undo(value("ab"))?.text)
    }

    @Test
    fun aLineBreakStartsANewStep() {
        type("", "a", "a\n", "a\nb")
        assertEquals("a", history.undo(value("a\nb"))?.text)
    }

    @Test
    fun aBoundaryEditIsItsOwnStep() {
        type("", "a")
        history.record(value("a"), value("**a**"), boundary = true)
        time += 100
        history.record(value("**a**"), value("**a**b"))
        assertEquals("**a**", history.undo(value("**a**b"))?.text)
        assertEquals("a", history.undo(value("**a**"))?.text)
        assertEquals("", history.undo(value("a"))?.text)
    }

    @Test
    fun redoReappliesAndANewEditClearsIt() {
        type("", "a")
        val undone = history.undo(value("a"))!!
        assertTrue(history.canRedo)
        assertEquals("a", history.redo(undone)?.text)
        history.undo(value("a"))
        history.record(value(""), value("z"))
        assertFalse(history.canRedo)
        assertNull(history.redo(value("z")))
    }

    @Test
    fun undoRestoresTheCaretFromBeforeTheEdit() {
        history.record(TextFieldValue("ab", TextRange(1)), TextFieldValue("axb", TextRange(2)))
        assertEquals(TextRange(1), history.undo(TextFieldValue("axb", TextRange(2)))?.selection)
    }
}
