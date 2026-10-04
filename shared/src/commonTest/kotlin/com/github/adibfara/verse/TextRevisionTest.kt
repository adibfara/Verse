package com.github.adibfara.verse

import com.github.adibfara.verse.core.markdown.MdRange
import com.github.adibfara.verse.core.markdown.textRevisionBetween
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TextRevisionTest {

    @Test
    fun identicalTextHasNoRevision() {
        assertNull(textRevisionBetween("# Hi", "# Hi"))
    }

    @Test
    fun insertionIsLocatedAtTheChangedRegion() {
        val revision = textRevisionBetween("# Hi", "# Hik")!!
        assertEquals(4, revision.start)
        assertEquals(4, revision.removedEnd)
        assertEquals(5, revision.insertedEnd)
        assertEquals(1, revision.delta)
    }

    @Test
    fun deletionIsLocatedAtTheChangedRegion() {
        val revision = textRevisionBetween("# Hi", "# H")!!
        assertEquals(3, revision.start)
        assertEquals(4, revision.removedEnd)
        assertEquals(3, revision.insertedEnd)
        assertEquals(-1, revision.delta)
    }

    @Test
    fun rangesBeforeTheRevisionSurviveUnchanged() {
        val revision = textRevisionBetween("# Hi", "# Hik")!!
        assertEquals(MdRange(0, 2), revision.shift(MdRange(0, 2)))
    }

    @Test
    fun rangesAfterTheRevisionAreShiftedByTheDelta() {
        val revision = textRevisionBetween("ab\ncd", "aXb\ncd")!!
        assertEquals(MdRange(4, 6), revision.shift(MdRange(3, 5)))
    }

    @Test
    fun touchedRangesAreDropped() {
        val revision = textRevisionBetween("**bold**", "**bld**")!!
        assertNull(revision.shift(MdRange(2, 6)))
    }

    @Test
    fun stretchingKeepsAnEnclosingRangeAlive() {
        val revision = textRevisionBetween("# Hi", "# Hik")!!
        assertEquals(MdRange(0, 5), revision.stretch(MdRange(0, 4)))
    }

    @Test
    fun stretchingShrinksAnEnclosingRangeOnDeletion() {
        val revision = textRevisionBetween("# Hi", "# H")!!
        assertEquals(MdRange(0, 3), revision.stretch(MdRange(0, 4)))
    }

    @Test
    fun replayingARevisionReproducesTheNewText() {
        val pairs = listOf(
            "" to "a",
            "a" to "",
            "# Hi" to "# Hello",
            "aaa" to "aaaa",
            "- one\n- two" to "- one\n- two\n- three",
            "abc" to "cba",
        )
        pairs.forEach { (before, after) ->
            val revision = textRevisionBetween(before, after)
            if (revision == null) {
                assertEquals(before, after)
            } else {
                val rebuilt = before.substring(0, revision.start) +
                    after.substring(revision.start, revision.insertedEnd) +
                    before.substring(revision.removedEnd)
                assertEquals(after, rebuilt, "revision did not reproduce \"$after\"")
            }
        }
    }
}
