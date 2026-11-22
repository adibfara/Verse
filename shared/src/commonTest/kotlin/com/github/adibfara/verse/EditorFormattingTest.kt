package com.github.adibfara.verse

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.github.adibfara.verse.core.filesystem.FileSystemException
import com.github.adibfara.verse.core.filesystem.InMemorySpaceFileSystem
import com.github.adibfara.verse.core.filesystem.SpaceFileSystem
import com.github.adibfara.verse.core.filesystem.SpaceFileSystemFactory
import com.github.adibfara.verse.core.markdown.FormatAction
import com.github.adibfara.verse.core.markdown.ImeKey
import com.github.adibfara.verse.core.markdown.imeBatchFixup
import com.github.adibfara.verse.core.markdown.imeKeyEdit
import com.github.adibfara.verse.core.markdown.InlineFormat
import com.github.adibfara.verse.core.markdown.LineMarkerKind
import com.github.adibfara.verse.core.markdown.MarkdownParser
import com.github.adibfara.verse.core.markdown.MdRange
import com.github.adibfara.verse.core.markdown.TaskBoxPlaceholder
import com.github.adibfara.verse.core.markdown.maskDocument
import com.github.adibfara.verse.core.markdown.maskEditsFor
import com.github.adibfara.verse.core.model.FileKind
import com.github.adibfara.verse.core.model.FilePath
import com.github.adibfara.verse.core.model.Space
import com.github.adibfara.verse.core.persistence.SettingsStore
import com.github.adibfara.verse.core.platform.LocationResolver
import com.github.adibfara.verse.feature.editor.EditorAction
import com.github.adibfara.verse.feature.editor.EditorEvent
import com.github.adibfara.verse.feature.editor.EditorUiState
import com.github.adibfara.verse.feature.editor.EditorViewModel
import com.github.adibfara.verse.feature.space.SpaceRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

private const val NotePath = "note.md"

private class SingleFileSystemFactory(private val fileSystem: SpaceFileSystem) :
    SpaceFileSystemFactory {
    override fun create(space: Space): SpaceFileSystem = fileSystem
}

private class WriteFailingFileSystem(private val delegate: SpaceFileSystem) : SpaceFileSystem by delegate {
    override suspend fun writeText(path: FilePath, content: String) {
        throw FileSystemException("Cannot write to ${path.value} because it is locked")
    }
}

private class ReadFailingFileSystem(private val delegate: SpaceFileSystem) : SpaceFileSystem by delegate {
    override suspend fun readText(path: FilePath): String {
        throw FileSystemException("Cannot read ${path.value} because it is locked")
    }
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
    fun anEmptyHeadingHidesItsMarkerOnceTheSpaceIsTyped() {
        assertRenders("", FormatAction.SetHeading(0, 1), "")
        assertRenders("a\n", FormatAction.SetHeading(2, 3), "a\n")
        val parser = MarkdownParser()
        fun rendered(text: String): String {
            val document = parser.parse(text)
            return maskDocument(document, maskEditsFor(document), text, revealedLine = null).mask.transformedText
        }
        assertEquals("", rendered("# "))
        assertEquals("", rendered("###### "))
        assertEquals("#", rendered("#"))
        assertEquals("#tag", rendered("#tag"))
    }

    @Test
    fun removingAHeadingRendersThePlainLine() {
        assertRenders("## Title", FormatAction.SetHeading(0, 0), "Title")
    }

    @Test
    fun addingACheckboxRendersTheDrawnPlaceholderAndNoBrackets() {
        val state = stateAfter("- milk", FormatAction.SetLineMarker(LineMarkerKind.Task))
        assertEquals("- [ ] milk", state.value.text)
        assertEquals(state.value.text, state.document.text)
        assertEquals(" $TaskBoxPlaceholder  milk", renderedTextOf(state))
    }

    @Test
    fun switchingACheckboxToABulletRendersTheBulletGlyph() {
        assertRenders("- [x] milk", FormatAction.SetLineMarker(LineMarkerKind.Bullet), "• milk")
    }

    @Test
    fun clearingAListMarkerRendersPlainText() {
        assertRenders("- [ ] milk", FormatAction.SetLineMarker(LineMarkerKind.None), "milk")
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
        imeCommit(
            viewModel,
            TextFieldValue(
                text = initial.replaceRange(caret, caret, "\n"),
                selection = TextRange(caret + 1),
            ),
        )
        return viewModel.state.value
    }

    private fun imeCommit(viewModel: EditorViewModel, committed: TextFieldValue) {
        val before = viewModel.state.value.value.text
        viewModel.onEvent(EditorEvent.TextChanged(committed))
        val selection = MdRange(committed.selection.min, committed.selection.max)
        val fixup = imeBatchFixup(before, committed.text, selection) ?: return
        viewModel.onEvent(
            EditorEvent.TextChanged(
                TextFieldValue(fixup.text, TextRange(fixup.selection.start, fixup.selection.end)),
                parseNow = true,
            ),
        )
    }

    @Test
    fun aSoftEnterKeyMidWordContinuesTheListInOneParsedEdit() {
        val viewModel = viewModelFor("- [ ] milk")
        val composing = TextFieldValue("- [ ] milk", TextRange(10), composition = TextRange(6, 10))
        viewModel.onEvent(EditorEvent.TextChanged(composing))
        val edit = imeKeyEdit(composing.text, MdRange(10, 10), ImeKey.Enter)
        assertNotNull(edit)
        viewModel.onEvent(
            EditorEvent.TextChanged(
                TextFieldValue(edit.text, TextRange(edit.selection.start, edit.selection.end)),
                parseNow = true,
            ),
        )
        val state = viewModel.state.value
        assertEquals("- [ ] milk\n- [ ] ", state.value.text)
        assertEquals(TextRange(17), state.value.selection)
        assertEquals(state.value.text, state.document.text)
        assertFalse("[" in renderedTextOf(state))
    }

    @Test
    fun aSoftKeyOnlyRewritesListLinesAndCheckboxBackspaces() {
        assertEquals("milk", imeKeyEdit("- [ ] milk", MdRange(6, 6), ImeKey.Backspace)?.text)
        assertNull(imeKeyEdit("- [ ] milk", MdRange(10, 10), ImeKey.Backspace))
        assertNull(imeKeyEdit("plain", MdRange(5, 5), ImeKey.Enter))
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
    fun aTypedNewlineOnAnEmptyItemEndsTheListAndStartsANewLine() {
        val state = afterTypingNewline("- milk\n- ", 9)
        assertEquals("- milk\n\n", state.value.text)
        assertEquals(8, state.value.selection.start)
    }

    @Test
    fun aTypedNewlineOnAnEmptyTaskEndsTheListAndLeavesNoMarkerOnScreen() {
        val state = afterTypingNewline("- [x] milk\n- [ ] ", 17)
        assertEquals("- [x] milk\n\n", state.value.text)
        assertEquals(12, state.value.selection.start)
        val rendered = renderedTextOf(state)
        assertFalse("-" in rendered)
        assertFalse("[" in rendered)
    }

    @Test
    fun aTypedNewlineAfterASecondSpaceContinuesTheList() {
        val state = afterTypingNewline("-  ", 3)
        assertEquals("-  \n- ", state.value.text)
        assertEquals(6, state.value.selection.start)
    }

    @Test
    fun aTypedNewlineInTheMiddleOfAnItemSplitsItIntoTwoItems() {
        val state = afterTypingNewline("- milk and honey", 6)
        assertEquals("- milk\n-  and honey", state.value.text)
        assertEquals(9, state.value.selection.start)
    }

    @Test
    fun aTypedNewlineOnAnIndentedItemKeepsTheIndent() {
        val state = afterTypingNewline("  - milk", 8)
        assertEquals("  - milk\n  - ", state.value.text)
        assertEquals(13, state.value.selection.start)
    }

    @Test
    fun anEnterKeyOnAListLineContinuesItWithoutShowingAMarker() {
        val state = stateAfter("- milk", FormatAction.ContinueList(6), MdRange(6, 6))
        assertEquals("- milk\n- ", state.value.text)
        assertEquals(9, state.value.selection.start)
        assertEquals(state.value.text, state.document.text)
        assertFalse("-" in renderedTextOf(state))
    }

    @Test
    fun anEnterKeyOnADocumentThatEndsInAListLineContinuesFromTheLastLine() {
        val state = stateAfter("- one\n- two", FormatAction.ContinueList(11), MdRange(11, 11))
        assertEquals("- one\n- two\n- ", state.value.text)
        assertEquals(14, state.value.selection.start)
    }

    @Test
    fun anEnterKeyOnAnEmptyTaskEndsTheListAndLeavesNoMarkerOnScreen() {
        val state = stateAfter("- [ ] ", FormatAction.ContinueList(6), MdRange(6, 6))
        assertEquals("\n", state.value.text)
        assertEquals(1, state.value.selection.start)
    }

    @Test
    fun aTypedNewlineContinuesEvenWhenTheImeCommitsASpaceWithIt() {
        val viewModel = viewModelFor("- milk")
        viewModel.onEvent(EditorEvent.TextChanged(TextFieldValue("- milk", TextRange(6))))
        imeCommit(viewModel, TextFieldValue("- milk \n", TextRange(8)))
        val state = viewModel.state.value
        assertEquals("- milk \n- ", state.value.text)
        assertEquals(10, state.value.selection.start)
        assertEquals(state.value.text, state.document.text)
    }

    @Test
    fun aTypedNewlineContinuesEvenWhenTheCaretEndsUpPastTheNewline() {
        val viewModel = viewModelFor("- milk")
        viewModel.onEvent(EditorEvent.TextChanged(TextFieldValue("- milk", TextRange(6))))
        imeCommit(viewModel, TextFieldValue("- milk\na", TextRange(8)))
        val state = viewModel.state.value
        assertEquals("- milk\n- a", state.value.text)
        assertEquals(10, state.value.selection.start)
    }

    @Test
    fun aTypedNewlineOutsideAListStaysAPlainNewline() {
        val state = afterTypingNewline("plain", 5)
        assertEquals("plain\n", state.value.text)
    }

    @Test
    fun aSoftKeyboardBackspaceInsideACheckboxRemovesTheWholePrefix() {
        val viewModel = viewModelFor("- [ ] milk")
        viewModel.onEvent(EditorEvent.TextChanged(TextFieldValue("- [ ] milk", TextRange(5))))
        imeCommit(viewModel, TextFieldValue("- [  milk", TextRange(4)))
        val state = viewModel.state.value
        assertEquals("milk", state.value.text)
        assertEquals(0, state.value.selection.start)
        assertEquals(state.value.text, state.document.text)
        assertFalse("[" in renderedTextOf(state))
    }

    @Test
    fun aSoftKeyboardBackspaceOnAWholeCheckboxRemovesTheWholePrefix() {
        val viewModel = viewModelFor("- [ ] milk")
        viewModel.onEvent(EditorEvent.TextChanged(TextFieldValue("- [ ] milk", TextRange(5))))
        imeCommit(viewModel, TextFieldValue("-  milk", TextRange(2)))
        assertEquals("milk", viewModel.state.value.value.text)
        assertEquals(0, viewModel.state.value.value.selection.start)
    }

    @Test
    fun aSoftKeyboardBackspaceInPlainTextStaysAPlainDeletion() {
        val viewModel = viewModelFor("- milk")
        viewModel.onEvent(EditorEvent.TextChanged(TextFieldValue("- milk", TextRange(2))))
        imeCommit(viewModel, TextFieldValue("- mil", TextRange(1)))
        assertEquals("- mil", viewModel.state.value.value.text)
    }

    @Test
    fun anEnterKeyAfterACheckboxContinuesTheListWithoutMarkersOnScreen() {
        val state = stateAfter("- [ ] milk", FormatAction.DeleteTaskMarker(5), MdRange(5, 5))
        assertEquals("milk", state.value.text)
        assertEquals(0, state.value.selection.start)
        assertEquals(state.value.text, state.document.text)
        assertFalse("[" in renderedTextOf(state))
    }

    @Test
    fun aTypedNewlineMidCompositionStillContinuesTheList() {
        val viewModel = viewModelFor("- milk")
        viewModel.onEvent(EditorEvent.TextChanged(TextFieldValue("- milk", TextRange(6), TextRange(2, 6))))
        imeCommit(viewModel, TextFieldValue("- milk\n", TextRange(7), TextRange(2, 6)))
        val state = viewModel.state.value
        assertEquals("- milk\n- ", state.value.text)
    }

    @Test
    fun formattingKeepsTheCompositionWhenItEditsAwayFromIt() {
        val viewModel = viewModelFor("- [ ] milk")
        viewModel.onEvent(
            EditorEvent.TextChanged(
                TextFieldValue("- [ ] milk", TextRange(10), TextRange(6, 10)),
            ),
        )
        viewModel.onEvent(EditorEvent.ApplyFormat(FormatAction.DeleteTaskMarker(5)))
        val state = viewModel.state.value
        assertEquals("milk", state.value.text)
        assertEquals(4, state.value.selection.start)
        assertEquals(TextRange(0, 4), state.value.composition)
    }

    @Test
    fun formattingOverlappingTheCompositionCommitsItInsteadOfFightingTheIme() {
        val viewModel = viewModelFor("- [ ] milk")
        viewModel.onEvent(
            EditorEvent.TextChanged(
                TextFieldValue("- [ ] milk", TextRange(5), TextRange(0, 6)),
            ),
        )
        viewModel.onEvent(EditorEvent.ApplyFormat(FormatAction.DeleteTaskMarker(5)))
        val state = viewModel.state.value
        assertEquals("milk", state.value.text)
        assertNull(state.value.composition)
    }

    @Test
    fun autosavePersistsAnEditAfterTheQuietPeriod() = runTest {
        val main = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(main)
        try {
            val dispatchers = TestDispatchers(main)
            val settingsStore = SettingsStore(
                storage = InMemoryFileStorage(),
                appPaths = TestAppPaths(),
                dispatchers = dispatchers,
                scope = CoroutineScope(main),
            )
            val fileSystem = InMemorySpaceFileSystem(seed = mapOf(NotePath to "hello"))
            val spaceRepository = SpaceRepository(
                fileSystemFactory = SingleFileSystemFactory(fileSystem),
                locationResolver = TestLocationResolver(),
                settingsStore = settingsStore,
            )
            spaceRepository.openFolder("memory://space")
            val viewModel = EditorViewModel(spaceRepository, settingsStore, dispatchers)
            viewModel.onEvent(EditorEvent.Open(FilePath(NotePath), FileKind.Markdown))
            advanceUntilIdle()
            assertEquals("hello", viewModel.state.value.value.text)

            viewModel.onEvent(EditorEvent.TextChanged(TextFieldValue("hello, autosave", TextRange(15))))
            advanceTimeBy(2_000)
            advanceUntilIdle()

            assertEquals("hello, autosave", fileSystem.readText(FilePath(NotePath)))
            assertFalse(viewModel.state.value.isDirty)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun openingAMissingFileDropsTheTabAndAsksForItsRemoval() = runTest {
        val main = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(main)
        try {
            val dispatchers = TestDispatchers(main)
            val settingsStore = SettingsStore(
                storage = InMemoryFileStorage(),
                appPaths = TestAppPaths(),
                dispatchers = dispatchers,
                scope = CoroutineScope(main),
            )
            val fileSystem = InMemorySpaceFileSystem(seed = mapOf(NotePath to "hello"))
            val spaceRepository = SpaceRepository(
                fileSystemFactory = SingleFileSystemFactory(fileSystem),
                locationResolver = TestLocationResolver(),
                settingsStore = settingsStore,
            )
            spaceRepository.openFolder("memory://space")
            val viewModel = EditorViewModel(spaceRepository, settingsStore, dispatchers)
            val missing = mutableListOf<EditorAction>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.actions.collect { action -> missing.add(action) }
            }

            viewModel.onEvent(EditorEvent.Open(FilePath("ghost.md"), FileKind.Markdown))
            advanceUntilIdle()

            assertFalse(viewModel.state.value.hasFile)
            assertEquals(1, missing.count { it is EditorAction.Missing })
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun openingAnExistingFileThatFailsToReadKeepsTheTabAndShowsTheError() = runTest {
        val main = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(main)
        try {
            val dispatchers = TestDispatchers(main)
            val settingsStore = SettingsStore(
                storage = InMemoryFileStorage(),
                appPaths = TestAppPaths(),
                dispatchers = dispatchers,
                scope = CoroutineScope(main),
            )
            val spaceRepository = SpaceRepository(
                fileSystemFactory = SingleFileSystemFactory(
                    ReadFailingFileSystem(InMemorySpaceFileSystem(seed = mapOf(NotePath to "hello"))),
                ),
                locationResolver = TestLocationResolver(),
                settingsStore = settingsStore,
            )
            spaceRepository.openFolder("memory://space")
            val viewModel = EditorViewModel(spaceRepository, settingsStore, dispatchers)
            val actions = mutableListOf<EditorAction>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.actions.collect { action -> actions.add(action) }
            }

            viewModel.onEvent(EditorEvent.Open(FilePath(NotePath), FileKind.Markdown))
            advanceUntilIdle()

            assertTrue(viewModel.state.value.hasFile)
            assertTrue(viewModel.state.value.errorMessage?.contains("locked") == true)
            assertEquals(0, actions.size)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun aFailedAutosaveSurfacesTheReasonAndKeepsTheNoteDirty() = runTest {
        val main = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(main)
        try {
            val dispatchers = TestDispatchers(main)
            val settingsStore = SettingsStore(
                storage = InMemoryFileStorage(),
                appPaths = TestAppPaths(),
                dispatchers = dispatchers,
                scope = CoroutineScope(main),
            )
            val spaceRepository = SpaceRepository(
                fileSystemFactory = SingleFileSystemFactory(
                    WriteFailingFileSystem(InMemorySpaceFileSystem(seed = mapOf(NotePath to "hello"))),
                ),
                locationResolver = TestLocationResolver(),
                settingsStore = settingsStore,
            )
            spaceRepository.openFolder("memory://space")
            val viewModel = EditorViewModel(spaceRepository, settingsStore, dispatchers)
            viewModel.onEvent(EditorEvent.Open(FilePath(NotePath), FileKind.Markdown))
            advanceUntilIdle()

            viewModel.onEvent(EditorEvent.TextChanged(TextFieldValue("hello again", TextRange(11))))
            advanceTimeBy(2_000)
            advanceUntilIdle()

            assertEquals("Cannot write to $NotePath because it is locked", viewModel.state.value.errorMessage)
            assertTrue(viewModel.state.value.isDirty)

            viewModel.onEvent(EditorEvent.DismissError)
            assertNull(viewModel.state.value.errorMessage)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun aLaterSuccessfulSaveClearsTheSurfacedError() = runTest {
        val main = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(main)
        try {
            val dispatchers = TestDispatchers(main)
            val settingsStore = SettingsStore(
                storage = InMemoryFileStorage(),
                appPaths = TestAppPaths(),
                dispatchers = dispatchers,
                scope = CoroutineScope(main),
            )
            val fileSystem = InMemorySpaceFileSystem(seed = mapOf(NotePath to "hello"))
            val spaceRepository = SpaceRepository(
                fileSystemFactory = SingleFileSystemFactory(fileSystem),
                locationResolver = TestLocationResolver(),
                settingsStore = settingsStore,
            )
            spaceRepository.openFolder("memory://space")
            val viewModel = EditorViewModel(spaceRepository, settingsStore, dispatchers)
            viewModel.onEvent(EditorEvent.Open(FilePath(NotePath), FileKind.Markdown))
            advanceUntilIdle()

            viewModel.onEvent(EditorEvent.ReportError("Cannot write to $NotePath because it is locked"))
            assertEquals("Cannot write to $NotePath because it is locked", viewModel.state.value.errorMessage)

            viewModel.onEvent(EditorEvent.TextChanged(TextFieldValue("hello again", TextRange(11))))
            advanceTimeBy(2_000)
            advanceUntilIdle()

            assertNull(viewModel.state.value.errorMessage)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun startingTheBodyPutsABlankLineAboveTheTitleLineAndLeavesTheCaretAtTheTop() {
        val viewModel = viewModelFor("# Title\nbody")
        viewModel.onEvent(EditorEvent.StartNoteBody)
        val state = viewModel.state.value
        assertEquals("\n# Title\nbody", state.value.text)
        assertEquals(0, state.value.selection.start)
        assertTrue(state.value.selection.collapsed)
        assertEquals(state.value.text, state.document.text)
    }

    @Test
    fun startingTheBodyOnAnEmptyNoteOnlyMovesTheCaret() {
        val viewModel = viewModelFor("")
        viewModel.onEvent(EditorEvent.StartNoteBody)
        assertEquals("", viewModel.state.value.value.text)
        assertEquals(0, viewModel.state.value.value.selection.start)
    }

    @Test
    fun startingTheBodyTwiceDoesNotStackBlankLines() {
        val viewModel = viewModelFor("# Title")
        viewModel.onEvent(EditorEvent.StartNoteBody)
        viewModel.onEvent(EditorEvent.StartNoteBody)
        assertEquals("\n# Title", viewModel.state.value.value.text)
        assertEquals(0, viewModel.state.value.value.selection.start)
    }

    @Test
    fun startingTheBodyNeverRendersAMarkerCharacter() {
        val viewModel = viewModelFor("# Title")
        viewModel.onEvent(EditorEvent.StartNoteBody)
        val rendered = renderedTextOf(viewModel.state.value)
        listOf("#", "*", "[", "]").forEach { marker ->
            assertFalse(marker in rendered, "\"$marker\" is visible in \"$rendered\"")
        }
        assertTrue(rendered.startsWith("\nTitle"), "\"$rendered\" should start with a blank line then the title")
    }

    @Test
    fun leavingTheStartOfTheNoteDeletesAnEmptyFirstLineAndAsksForTheTitle() {
        val viewModel = viewModelFor("\n# Title")
        viewModel.onEvent(EditorEvent.JumpToTitle)
        val state = viewModel.state.value
        assertEquals("# Title", state.value.text)
        assertEquals(state.value.text, state.document.text)
        assertNotNull(state.focusTitleRequestId)
    }

    @Test
    fun leavingTheStartOfTheNoteKeepsALineThatHasContent() {
        val viewModel = viewModelFor("# Title")
        viewModel.onEvent(EditorEvent.JumpToTitle)
        val state = viewModel.state.value
        assertEquals("# Title", state.value.text)
        assertNotNull(state.focusTitleRequestId)
    }

    @Test
    fun noFormatActionLeavesAMarkerCharacterOnScreen() {
        val cases = listOf(
            "Title" to FormatAction.SetHeading(0, 3),
            "- item" to FormatAction.SetLineMarker(LineMarkerKind.Task),
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
