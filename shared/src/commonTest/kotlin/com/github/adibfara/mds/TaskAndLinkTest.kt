package com.github.adibfara.mds

import com.github.adibfara.mds.core.markdown.MarkdownParser
import com.github.adibfara.mds.core.markdown.StyleKind
import com.github.adibfara.mds.core.markdown.documentWithTaskToggled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TaskAndLinkTest {

    private val parser = MarkdownParser()

    @Test
    fun inlineLinksAreCollectedWithTheirTarget() {
        val document = parser.parse("A [label](notes/target.md) here.")
        val link = document.links.single()
        assertEquals("notes/target.md", link.target)
        assertEquals("[label](notes/target.md)", document.text.substring(link.range.start, link.range.end))
    }

    @Test
    fun externalLinksKeepTheirScheme() {
        val document = parser.parse("See [docs](https://example.com/page).")
        assertEquals("https://example.com/page", document.links.single().target)
    }

    @Test
    fun bareAndAngledAutolinksAreCollected() {
        assertEquals(
            "https://example.com",
            parser.parse("see https://example.com now").links.single().target,
        )
        assertEquals(
            "https://example.com",
            parser.parse("see <https://example.com> now").links.single().target,
        )
    }

    @Test
    fun fencedCodeBlocksExposeTheirLanguageAndCode() {
        val document = parser.parse("intro\n\n```kotlin\nfun main() {}\n```\n")
        val block = document.codeBlocks.single()
        assertEquals("kotlin", block.language)
        assertEquals("fun main() {}", block.code)
        assertEquals("```kotlin", document.text.substring(block.headerRange.start, block.headerRange.end))
    }

    @Test
    fun fencedCodeBlocksWithoutALanguageExposeAnEmptyOne() {
        val block = parser.parse("```\nplain\n```\n").codeBlocks.single()
        assertEquals("", block.language)
        assertEquals("plain", block.code)
    }

    @Test
    fun fencedCodeBlocksInsideListsAreCollected() {
        val document = parser.parse("- item\n\n  ```js\n  let a = 1\n  ```\n")
        assertEquals(1, document.codeBlocks.size)
    }

    @Test
    fun referenceLinksWithoutDestinationAreNotCollected() {
        val document = parser.parse("- [x] done\n\n[unresolved] text\n")
        assertTrue(document.links.isEmpty(), "collected ${document.links}")
    }

    @Test
    fun imagesAreNotCollectedAsLinks() {
        val document = parser.parse("![alt](picture.png)")
        assertTrue(document.links.isEmpty())
    }

    @Test
    fun tasksAreExposedWithTheirCheckedState() {
        val document = parser.parse("- [ ] open\n- [x] done\n")
        assertEquals(listOf(false, true), document.tasks.map { it.checked })
        document.tasks.forEach { task ->
            val raw = document.text.substring(task.range.start, task.range.end)
            assertTrue(raw == "[ ]" || raw == "[x]", "unexpected marker \"$raw\"")
        }
    }

    @Test
    fun openAndDoneTasksGetDifferentMarkerStyles() {
        val document = parser.parse("- [ ] open\n- [x] done\n")
        val kinds = document.styles.map { it.kind }
        assertTrue(StyleKind.TaskMarker in kinds, "missing open marker style")
        assertTrue(StyleKind.TaskMarkerDone in kinds, "missing done marker style")
    }

    @Test
    fun completedTaskTextStartsAfterTheCheckboxSpacing() {
        val text = "- [x] done\n- [ ] open\n"
        val document = parser.parse(text)
        val done = document.styles.single { it.kind == StyleKind.TaskText }
        assertEquals("done", text.substring(done.range.start, done.range.end))
    }

    @Test
    fun taskTextStyleStopsAtTheEndOfItsLine() {
        val text = "- [x] done\nnext paragraph\n"
        val document = parser.parse(text)
        val done = document.styles.single { it.kind == StyleKind.TaskText }
        assertEquals("done", text.substring(done.range.start, done.range.end))
    }

    @Test
    fun togglingATaskMatchesAFullReparse() {
        val before = "- [ ] alpha\n- [x] beta\n"
        val after = "- [x] alpha\n- [x] beta\n"
        val document = parser.parse(before)
        val task = document.tasks.first()

        val optimistic = documentWithTaskToggled(document, after, task.range, checked = true)
        val reparsed = parser.parse(after)

        assertEquals(reparsed.text, optimistic.text)
        assertEquals(reparsed.tasks, optimistic.tasks)
        assertEquals(reparsed.decorations, optimistic.decorations)
        assertEquals(reparsed.markers, optimistic.markers)
        assertEquals(reparsed.styles, optimistic.styles)
        assertEquals(reparsed.stats, optimistic.stats)
    }

    @Test
    fun untogglingATaskMatchesAFullReparse() {
        val before = "- [x] alpha\n"
        val after = "- [ ] alpha\n"
        val document = parser.parse(before)
        val task = document.tasks.first()

        val optimistic = documentWithTaskToggled(document, after, task.range, checked = false)
        val reparsed = parser.parse(after)

        assertEquals(reparsed.styles, optimistic.styles)
        assertEquals(reparsed.tasks, optimistic.tasks)
    }
}
