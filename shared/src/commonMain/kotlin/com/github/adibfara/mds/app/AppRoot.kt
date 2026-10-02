package com.github.adibfara.mds.app

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.adibfara.mds.app.ui.DesktopShell
import com.github.adibfara.mds.app.ui.MobileShell
import com.github.adibfara.mds.app.ui.WorkspaceEffects
import com.github.adibfara.mds.app.ui.rememberLinkOpener
import com.github.adibfara.mds.app.ui.rememberSpaceImageLoader
import com.github.adibfara.mds.core.command.CommandId
import com.github.adibfara.mds.core.command.CommandRegistry
import com.github.adibfara.mds.core.command.Commands
import com.github.adibfara.mds.core.command.commandShortcuts
import com.github.adibfara.mds.core.designsystem.theme.MdsTheme
import com.github.adibfara.mds.core.designsystem.theme.resolveTheme
import com.github.adibfara.mds.core.model.FilePath
import com.github.adibfara.mds.core.platform.LocationResolver
import com.github.adibfara.mds.core.platform.PlatformFolderPickerOverlay
import com.github.adibfara.mds.core.platform.PlatformKind
import com.github.adibfara.mds.core.platform.SystemOpener
import com.github.adibfara.mds.core.platform.currentPlatformKind
import com.github.adibfara.mds.feature.editor.EditorEvent
import com.github.adibfara.mds.feature.editor.EditorViewModel
import com.github.adibfara.mds.feature.viewer.ui.LocalSpaceImageLoader
import com.github.adibfara.mds.feature.explorer.ExplorerEvent
import com.github.adibfara.mds.feature.explorer.ExplorerViewModel
import com.github.adibfara.mds.feature.settings.ui.SettingsPanel
import com.github.adibfara.mds.feature.search.SearchAction
import com.github.adibfara.mds.feature.search.SearchMode
import com.github.adibfara.mds.feature.search.SearchViewModel
import com.github.adibfara.mds.feature.search.ui.SearchOverlay
import com.github.adibfara.mds.feature.space.SpaceRepository
import com.github.adibfara.mds.feature.space.ui.SpacePickerScreen
import com.github.adibfara.mds.feature.tabs.TabsEvent
import com.github.adibfara.mds.feature.tabs.TabsViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

data class LaunchArguments(
    val initialFilePath: String? = null,
)

@Composable
fun AppRoot(
    launchArguments: LaunchArguments = LaunchArguments(),
    windowChrome: WindowChrome? = null,
) {
    val workspaceViewModel: WorkspaceViewModel = koinViewModel()
    val appState by workspaceViewModel.state.collectAsStateWithLifecycle()
    val theme = resolveTheme(appState.themeMode)

    LaunchedEffect(Unit) { workspaceViewModel.start(launchArguments) }

    val isMobile = currentPlatformKind() == PlatformKind.Mobile
    LaunchedEffect(appState.hasSpace) {
        if (isMobile && !appState.hasSpace) workspaceViewModel.refreshStorageAccess()
    }

    MdsTheme(theme = theme, editorFontSize = appState.settings.editorFontSize) {
        Box(modifier = Modifier.fillMaxSize().background(MdsTheme.colors.window)) {
            when {
                !appState.isReady -> Unit
                !appState.hasSpace -> SpacePickerScreen(
                    recentSpaces = appState.recentSpaces,
                    canChooseFolder = !isMobile || appState.isStorageAccessGranted,
                    onOpenRecent = workspaceViewModel::openRecent,
                    onForgetRecent = workspaceViewModel::forgetRecent,
                    onChooseFolder = workspaceViewModel::chooseFolder,
                    showStorageOnboarding = isMobile,
                    isStorageAccessGranted = appState.isStorageAccessGranted,
                    onRequestStorageAccess = workspaceViewModel::requestStorageAccess,
                    onUseInternalFolder = workspaceViewModel::openInternalFolder,
                )

                else -> WorkspaceScreen(
                    workspaceViewModel = workspaceViewModel,
                    appState = appState,
                    isDarkTheme = theme.isDark,
                    windowChrome = windowChrome,
                )
            }
            PlatformFolderPickerOverlay()
        }
    }
}

@Composable
private fun WorkspaceScreen(
    workspaceViewModel: WorkspaceViewModel,
    appState: AppUiState,
    isDarkTheme: Boolean,
    windowChrome: WindowChrome?,
) {
    val explorerViewModel: ExplorerViewModel = koinViewModel()
    val tabsViewModel: TabsViewModel = koinViewModel()
    val editorViewModel: EditorViewModel = koinViewModel()
    val searchViewModel: SearchViewModel = koinViewModel()
    val registry: CommandRegistry = koinInject()
    val spaceRepository: SpaceRepository = koinInject()
    val systemOpener: SystemOpener = koinInject()
    val externalOpenRequests: ExternalOpenRequests = koinInject()
    val locationResolver: LocationResolver = koinInject()
    var settingsVisible by remember { mutableStateOf(false) }

    val explorerState by explorerViewModel.state.collectAsStateWithLifecycle()
    val tabsState by tabsViewModel.state.collectAsStateWithLifecycle()
    val editorState by editorViewModel.state.collectAsStateWithLifecycle()
    val searchState by searchViewModel.state.collectAsStateWithLifecycle()

    WorkspaceEffects(
        workspaceViewModel = workspaceViewModel,
        explorerViewModel = explorerViewModel,
        tabsViewModel = tabsViewModel,
        editorViewModel = editorViewModel,
        tabsState = tabsState,
    )

    val clipboard = LocalClipboardManager.current
    val focusRequester = remember { FocusRequester() }
    val imageLoader = rememberSpaceImageLoader(spaceRepository, editorState.path)
    val openLink = rememberLinkOpener(
        spaceRepository = spaceRepository,
        activeFile = editorState.path,
        onOpenPath = { tabsViewModel.onEvent(TabsEvent.OpenPath(it)) },
        onOpenExternal = { systemOpener.openUrl(it) },
    )

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val absolutePathOf: (FilePath) -> String = { path ->
        spaceRepository.current.value?.fileSystem?.absolutePathOf(path).orEmpty()
    }

    val handleCommand: (CommandId) -> Boolean = handler@{ id ->
        Commands.tabIndexOf(id)?.let { index ->
            tabsViewModel.onEvent(TabsEvent.JumpTo(index))
            return@handler true
        }
        when (id) {
            Commands.NewNote.id -> explorerViewModel.onEvent(ExplorerEvent.RequestNewNote(null))
            Commands.NewFolder.id -> explorerViewModel.onEvent(ExplorerEvent.RequestNewFolder(null))
            Commands.Save.id -> editorViewModel.onEvent(EditorEvent.Save)
            Commands.CloseTab.id -> tabsViewModel.onEvent(TabsEvent.CloseActive)
            Commands.NextTab.id -> tabsViewModel.onEvent(TabsEvent.Next)
            Commands.PreviousTab.id -> tabsViewModel.onEvent(TabsEvent.Previous)
            Commands.ToggleLivePreview.id -> editorViewModel.onEvent(EditorEvent.ToggleLivePreview)
            Commands.ToggleRawMode.id -> editorViewModel.onEvent(EditorEvent.ToggleRawMode)
            Commands.ToggleSidebar.id -> workspaceViewModel.toggleSidebar()
            Commands.ToggleTheme.id -> workspaceViewModel.toggleTheme()
            Commands.OpenSettings.id -> settingsVisible = true
            Commands.CloseSpace.id -> workspaceViewModel.closeSpace()
            Commands.QuickOpen.id -> searchViewModel.open(SearchMode.QuickOpen)
            Commands.SearchInFiles.id -> searchViewModel.open(SearchMode.ContentSearch)
            Commands.CommandPalette.id -> searchViewModel.open(SearchMode.CommandPalette)
            else -> return@handler false
        }
        true
    }

    LaunchedEffect(searchViewModel) {
        searchViewModel.actions.collect { action ->
            when (action) {
                is SearchAction.OpenFile -> tabsViewModel.onEvent(TabsEvent.OpenPath(action.path))
                is SearchAction.RunCommand -> handleCommand(action.id)
            }
        }
    }

    LaunchedEffect(explorerViewModel) {
        explorerViewModel.actions.collect { searchViewModel.invalidateFileCache() }
    }

    LaunchedEffect(externalOpenRequests) {
        externalOpenRequests.requests.collect { absolutePath ->
            val opened = spaceRepository.current.value
            val relative = opened?.let {
                locationResolver.relativize(it.space.location, absolutePath)
            }
            if (relative != null) {
                tabsViewModel.onEvent(TabsEvent.OpenPath(FilePath(relative)))
            } else {
                workspaceViewModel.openSingleFile(absolutePath)
            }
        }
    }

    CompositionLocalProvider(LocalSpaceImageLoader provides imageLoader) {
        val shellModifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .commandShortcuts(registry, handleCommand)

        if (currentPlatformKind() == PlatformKind.Desktop) {
            DesktopShell(
                appState = appState,
                explorerState = explorerState,
                tabsState = tabsState,
                editorState = editorState,
                isDarkTheme = isDarkTheme,
                windowChrome = windowChrome,
                onExplorerEvent = explorerViewModel::onEvent,
                onTabsEvent = tabsViewModel::onEvent,
                onEditorEvent = editorViewModel::onEvent,
                onToggleSidebar = workspaceViewModel::toggleSidebar,
                onResizeSidebar = workspaceViewModel::resizeSidebar,
                onToggleTheme = workspaceViewModel::toggleTheme,
                onOpenSettings = { settingsVisible = true },
                onOpenQuickOpen = { searchViewModel.open(SearchMode.QuickOpen) },
                onOpenInDefaultApplication = {
                    editorState.path?.let { systemOpener.openInDefaultApplication(absolutePathOf(it)) }
                },
                onRevealInFileManager = {
                    editorState.path?.let { systemOpener.revealInFileManager(absolutePathOf(it)) }
                },
                onOpenLink = openLink,
                onCopyPath = { clipboard.setText(AnnotatedString(it)) },
                absolutePathOf = absolutePathOf,
                modifier = shellModifier,
            )
            if (settingsVisible) {
                SettingsPanel(
                    settings = appState.settings,
                    onSettingsChange = workspaceViewModel::updateSettings,
                    onCloseSpace = {
                        settingsVisible = false
                        workspaceViewModel.closeSpace()
                    },
                    onDismiss = { settingsVisible = false },
                )
            }

            if (searchState.isOpen) {
                SearchOverlay(
                    state = searchState,
                    onQueryChange = searchViewModel::queryChanged,
                    onMoveSelection = searchViewModel::moveSelection,
                    onActivate = searchViewModel::activate,
                    onActivateSelected = searchViewModel::activateSelected,
                    onDismiss = searchViewModel::close,
                )
            }
        } else {
            MobileShell(
                appState = appState,
                explorerState = explorerState,
                tabsState = tabsState,
                editorState = editorState,
                onExplorerEvent = explorerViewModel::onEvent,
                onTabsEvent = tabsViewModel::onEvent,
                onEditorEvent = editorViewModel::onEvent,
                onToggleTheme = workspaceViewModel::toggleTheme,
                onOpenInDefaultApplication = {
                    editorState.path?.let { systemOpener.openInDefaultApplication(absolutePathOf(it)) }
                },
                onOpenLink = openLink,
                modifier = shellModifier,
            )
        }
    }
}
