package com.github.adibfara.verse

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.github.adibfara.verse.core.filesystem.InMemorySpaceFileSystem
import com.github.adibfara.verse.core.filesystem.SpaceFileSystem
import com.github.adibfara.verse.core.filesystem.SpaceFileSystemFactory
import com.github.adibfara.verse.core.markdown.FormatAction
import com.github.adibfara.verse.core.markdown.InlineFormat
import com.github.adibfara.verse.core.markdown.LineMarkerKind
import com.github.adibfara.verse.core.markdown.MdRange
import com.github.adibfara.verse.core.markdown.TaskBoxPlaceholder
import com.github.adibfara.verse.core.markdown.maskDocument
import com.github.adibfara.verse.core.markdown.maskEditsFor
import com.github.adibfara.verse.core.model.FileKind
import com.github.adibfara.verse.core.model.FilePath
import com.github.adibfara.verse.core.model.Space
import com.github.adibfara.verse.core.persistence.SettingsStore
import com.github.adibfara.verse.core.platform.LocationResolver
import com.github.adibfara.verse.feature.editor.EditorEvent
import com.github.adibfara.verse.feature.editor.EditorUiState
import com.github.adibfara.verse.feature.editor.EditorViewModel
import com.github.adibfara.verse.feature.space.SpaceRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

private const val NotePath = "note.md"

private class SingleFileSystemFactory(private val fileSystem: SpaceFileSystem) :
    SpaceFileSystemFactory {
    override fun create(space: Space): SpaceFileSystem = fileSystem
}

private class TestLocationResolver : LocationResolver {
    override fun nameOf(location: String): String = location.substringAfterLast('/')
    override fun parentOf(location: String): String? = location.substringBeforeLast('/')
    override fun isFile(location: String): Boolean = location.endsWith(".md")
    override fun isDirectory(location: String): Boolean = !isFile(location)
    override fun relativize(root: String, location: String): String? =
        location.removePrefix(root).removePrefix("/")
}

class EditorFormattingTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModelFor(initial: String): EditorViewModel {
        val dispatchers = TestDispatchers(dispatcher)
        val scope = CoroutineScope(dispatcher)
        val settingsStore = SettingsStore(
            storage = InMemoryFileStorage(),
            appPaths = TestAppPaths(),
            dispatchers = dispatchers,
            scope = scope,
        )
        val spaceRepository = SpaceRepository(
            fileSystemFactory = SingleFileSystemFactory(
                InMemorySpaceFileSystem(seed = mapOf(NotePath to initial)),
            ),
            locationResolver = TestLocationResolver(),
            settingsStore = settingsStore,
        )
        spaceRepository.openFolder("memory://space")
        val viewModel = EditorViewModel(spaceRepository, settingsStore, dispatchers)
        viewModel.onEvent(EditorEvent.Open(FilePath(NotePath), FileKind.Markdown))
        assertEquals(initial, viewModel.state.value.value.text)
        return viewModel
    }

    private fun stateAfter(
        initial: String,
        action: FormatAction,
        selection: MdRange? = null,
    ): EditorUiState {
        val viewModel = viewModelFor(initial)
        if (selection != null) {
            viewModel.onEvent(
                EditorEvent.TextChanged(
                    viewModel.state.value.value.copy(
                        selection = TextRange(selection.start, selection.end),
                    ),
                ),
            )
        }
        viewModel.onEvent(EditorEvent.ApplyFormat(action))
        return viewModel.state.value
    }

    private fun renderedTextOf(state: EditorUiState): String {
        val document = state.document
        return maskDocument(
            document = document,
            edits = maskEditsFor(document),
            text = state.value.text,
            revealedLine = null,
        ).mask.transformedText
    }

    private fun assertRenders(initial: String, action: FormatAction, rendered: String) {
        val state = stateAfter(initial, action)
        assertEquals(
            state.value.text,
            state.document.text,
            "the published document must be parsed from the published text",
        )
        assertEquals(rendered, renderedTextOf(state))
    }

    @Test
    fun addingAHeadingNeverRendersItsHashMarker() {
        assertRenders("Title", FormatAction.SetHeading(0, 1), "Title")
        assertRenders("### Title", FormatAction.SetHeading(0, 2), "Title")
    }

    @Test
    fun removingAHeadingRendersThePlainLine() {
        assertRenders("## Title", FormatAction.SetHeading(0, 0), "Title")
    }

    @Test
    fun addingACheckboxRendersTheDrawnPlaceholderAndNoBrackets() {
        val state = stateAfter("- milk", FormatAction.SetLineMarker(0, LineMarkerKind.Task))
        assertEquals("- [ ] milk", state.value.text)
        assertEquals(state.value.text, state.document.text)
        assertEquals("$TaskBoxPlaceholder milk", renderedTextOf(state))
    }

    @Test
    fun switchingACheckboxToABulletRendersTheBulletGlyph() {
        assertRenders("- [x] milk", FormatAction.SetLineMarker(0, LineMarkerKind.Bullet), "• milk")
    }

    @Test
    fun clearingAListMarkerRendersPlainText() {
        assertRenders("- [ ] milk", FormatAction.SetLineMarker(0, LineMarkerKind.None), "milk")
    }

    @Test
    fun wrappingEmphasisNeverRendersItsMarkers() {
        assertRenders(
            "say loud now",
            FormatAction.ToggleInline(MdRange(4, 8), InlineFormat.Bold),
            "say loud now",
        )
        assertRenders(
            "say loud now",
            FormatAction.ToggleInline(MdRange(4, 8), InlineFormat.Code),
            "say loud now",
        )
    }

    @Test
    fun formattingKeepsTheCaretCollapsedInsteadOfSelectingTheSpan() {
        val state = stateAfter(
            initial = "say loud now",
            action = FormatAction.ToggleInline(MdRange(4, 8), InlineFormat.Bold),
            selection = MdRange(6, 6),
        )
        assertEquals("say **loud** now", state.value.text)
        assertTrue(state.value.selection.collapsed, "the caret must not become a selection")
        assertEquals("say **lo", state.value.text.substring(0, state.value.selection.start))
    }

    @Test
    fun formattingAnExistingSelectionKeepsItOnTheSameWords() {
        val state = stateAfter(
            initial = "say loud now",
            action = FormatAction.ToggleInline(MdRange(4, 8), InlineFormat.Bold),
            selection = MdRange(4, 8),
        )
        assertEquals("say **loud** now", state.value.text)
        assertEquals(
            "loud",
            state.value.text.substring(state.value.selection.min, state.value.selection.max),
        )
    }

    private fun afterTypingNewline(initial: String, caret: Int): EditorUiState {
        val viewModel = viewModelFor(initial)
        viewModel.onEvent(EditorEvent.TextChanged(TextFieldValue(initial, TextRange(caret))))
        viewModel.onEvent(
            EditorEvent.TextChanged(
                TextFieldValue(
                    text = initial.replaceRange(caret, caret, "\n"),
                    selection = TextRange(caret + 1),
                ),
            ),
        )
        return viewModel.state.value
    }

    @Test
    fun aTypedNewlineContinuesABulletList() {
        val state = afterTypingNewline("- milk", 6)
        assertEquals("- milk\n- ", state.value.text)
        assertEquals(9, state.value.selection.start)
    }

    @Test
    fun aTypedNewlineContinuesATaskAlwaysUnchecked() {
        val state = afterTypingNewline("- [x] milk", 10)
        assertEquals("- [x] milk\n- [ ] ", state.value.text)
    }

    @Test
    fun aTypedNewlineNumbersTheNextOrderedItem() {
        val state = afterTypingNewline("1. first", 8)
        assertEquals("1. first\n2. ", state.value.text)
    }

    @Test
    fun aTypedNewlineOnAnEmptyItemDropsTheMarker() {
        val state = afterTypingNewline("- milk\n- ", 9)
        assertEquals("- milk\n", state.value.text)
    }

    @Test
    fun aTypedNewlineOutsideAListStaysAPlainNewline() {
        val state = afterTypingNewline("plain", 5)
        assertEquals("plain\n", state.value.text)
    }

    @Test
    fun noFormatActionLeavesAMarkerCharacterOnScreen() {
        val cases = listOf(
            "Title" to FormatAction.SetHeading(0, 3),
            "- item" to FormatAction.SetLineMarker(0, LineMarkerKind.Task),
            "word" to FormatAction.ToggleInline(MdRange(0, 4), InlineFormat.Bold),
            "word" to FormatAction.ToggleInline(MdRange(0, 4), InlineFormat.Italic),
            "word" to FormatAction.ToggleInline(MdRange(0, 4), InlineFormat.Strikethrough),
        )
        cases.forEach { (initial, action) ->
            val rendered = renderedTextOf(stateAfter(initial, action))
            listOf("#", "*", "~", "[", "]").forEach { marker ->
                assertFalse(marker in rendered, "\"$marker\" is visible in \"$rendered\"")
            }
        }
    }
}
