package com.github.adibfara.verse

import com.github.adibfara.verse.core.model.FileKind
import com.github.adibfara.verse.core.model.FileNode
import com.github.adibfara.verse.core.model.FilePath
import com.github.adibfara.verse.core.persistence.WorkspaceSessionRepository
import com.github.adibfara.verse.core.persistence.WorkspaceStore
import com.github.adibfara.verse.core.persistence.WorkspaceTab
import com.github.adibfara.verse.feature.tabs.TabsEvent
import com.github.adibfara.verse.feature.tabs.TabsViewModel
import com.github.adibfara.verse.feature.tabs.tabIdOf
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

private fun node(path: String) = FileNode(
    path = FilePath(path),
    name = FilePath(path).name,
    isDirectory = false,
    kind = FileKind.Markdown,
    lastModified = 0,
    size = 0,
)

class TabsViewModelTest {

    private lateinit var viewModel: TabsViewModel

    @BeforeTest
    fun setUp() {
        val dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
        val dispatchers = TestDispatchers(dispatcher)
        val store = WorkspaceStore(
            storage = InMemoryFileStorage(),
            appPaths = TestAppPaths(),
            dispatchers = dispatchers,
            scope = CoroutineScope(dispatcher),
        )
        viewModel = TabsViewModel(WorkspaceSessionRepository(store))
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun openingAddsATabAndActivatesIt() {
        viewModel.onEvent(TabsEvent.Open(node("a.md")))
        val state = viewModel.state.value
        assertEquals(1, state.tabs.size)
        assertEquals(tabIdOf(FilePath("a.md")), state.activeTabId)
    }

    @Test
    fun openingTheSameFileTwiceReusesTheTab() {
        viewModel.onEvent(TabsEvent.Open(node("a.md")))
        viewModel.onEvent(TabsEvent.Open(node("b.md")))
        viewModel.onEvent(TabsEvent.Open(node("a.md")))
        val state = viewModel.state.value
        assertEquals(2, state.tabs.size)
        assertEquals(tabIdOf(FilePath("a.md")), state.activeTabId)
    }

    @Test
    fun closingActiveTabActivatesTheNeighbour() {
        listOf("a.md", "b.md", "c.md").forEach { viewModel.onEvent(TabsEvent.Open(node(it))) }
        viewModel.onEvent(TabsEvent.Activate(tabIdOf(FilePath("b.md"))))
        viewModel.onEvent(TabsEvent.CloseActive)
        val state = viewModel.state.value
        assertEquals(listOf("a.md", "c.md"), state.tabs.map { it.path.value })
        assertEquals(tabIdOf(FilePath("c.md")), state.activeTabId)
    }

    @Test
    fun closingTheLastTabLeavesNoActiveTab() {
        viewModel.onEvent(TabsEvent.Open(node("a.md")))
        viewModel.onEvent(TabsEvent.CloseActive)
        assertEquals(0, viewModel.state.value.tabs.size)
        assertNull(viewModel.state.value.activeTabId)
    }

    @Test
    fun closingOthersKeepsOnlyTheRequestedTab() {
        listOf("a.md", "b.md", "c.md").forEach { viewModel.onEvent(TabsEvent.Open(node(it))) }
        viewModel.onEvent(TabsEvent.CloseOthers(tabIdOf(FilePath("b.md"))))
        assertEquals(listOf("b.md"), viewModel.state.value.tabs.map { it.path.value })
    }

    @Test
    fun movingReordersTabs() {
        listOf("a.md", "b.md", "c.md").forEach { viewModel.onEvent(TabsEvent.Open(node(it))) }
        viewModel.onEvent(TabsEvent.Move(0, 2))
        assertEquals(listOf("b.md", "c.md", "a.md"), viewModel.state.value.tabs.map { it.path.value })
    }

    @Test
    fun movingOutOfBoundsIsIgnored() {
        listOf("a.md", "b.md").forEach { viewModel.onEvent(TabsEvent.Open(node(it))) }
        viewModel.onEvent(TabsEvent.Move(0, 9))
        assertEquals(listOf("a.md", "b.md"), viewModel.state.value.tabs.map { it.path.value })
    }

    @Test
    fun cyclingWrapsAround() {
        listOf("a.md", "b.md", "c.md").forEach { viewModel.onEvent(TabsEvent.Open(node(it))) }
        viewModel.onEvent(TabsEvent.Next)
        assertEquals(tabIdOf(FilePath("a.md")), viewModel.state.value.activeTabId)
        viewModel.onEvent(TabsEvent.Previous)
        assertEquals(tabIdOf(FilePath("c.md")), viewModel.state.value.activeTabId)
    }

    @Test
    fun jumpToIndexActivatesThatTab() {
        listOf("a.md", "b.md", "c.md").forEach { viewModel.onEvent(TabsEvent.Open(node(it))) }
        viewModel.onEvent(TabsEvent.JumpTo(1))
        assertEquals(tabIdOf(FilePath("b.md")), viewModel.state.value.activeTabId)
    }

    @Test
    fun renameUpdatesTabIdentityAndTitle() {
        viewModel.onEvent(TabsEvent.Open(node("a.md")))
        viewModel.onEvent(TabsEvent.Renamed(FilePath("a.md"), FilePath("renamed.md")))
        val tab = viewModel.state.value.tabs.single()
        assertEquals("renamed.md", tab.path.value)
        assertEquals("renamed", tab.title)
        assertEquals(tabIdOf(FilePath("renamed.md")), viewModel.state.value.activeTabId)
    }

    @Test
    fun dirtyFlagIsTracked() {
        viewModel.onEvent(TabsEvent.Open(node("a.md")))
        viewModel.onEvent(TabsEvent.MarkDirty(FilePath("a.md"), true))
        assertEquals(true, viewModel.state.value.tabs.single().isDirty)
        viewModel.onEvent(TabsEvent.MarkDirty(FilePath("a.md"), false))
        assertEquals(false, viewModel.state.value.tabs.single().isDirty)
    }

    @Test
    fun restoreRebuildsTabsAndActiveIndex() {
        viewModel.restore(
            tabs = listOf(WorkspaceTab(FilePath("a.md")), WorkspaceTab(FilePath("docs/b.md"))),
            activeIndex = 1,
        )
        val state = viewModel.state.value
        assertEquals(listOf("a.md", "docs/b.md"), state.tabs.map { it.path.value })
        assertEquals(tabIdOf(FilePath("docs/b.md")), state.activeTabId)
    }
}
