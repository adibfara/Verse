package com.github.adibfara.verse.app

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.adibfara.verse.app.ui.DesktopShell
import com.github.adibfara.verse.app.ui.MobileShell
import com.github.adibfara.verse.app.ui.WorkspaceEffects
import com.github.adibfara.verse.app.ui.rememberLinkOpener
import com.github.adibfara.verse.app.ui.rememberSpaceImageLoader
import com.github.adibfara.verse.core.command.CommandId
import com.github.adibfara.verse.core.command.CommandRegistry
import com.github.adibfara.verse.core.command.Commands
import com.github.adibfara.verse.core.command.commandShortcuts
import com.github.adibfara.verse.core.designsystem.theme.MobileUiScale
import com.github.adibfara.verse.core.designsystem.theme.VerseSizes
import com.github.adibfara.verse.core.designsystem.theme.VerseSpacing
import com.github.adibfara.verse.core.designsystem.theme.VerseTheme
import com.github.adibfara.verse.core.designsystem.theme.mobileSizes
import com.github.adibfara.verse.core.designsystem.theme.mobileSpacing
import com.github.adibfara.verse.core.designsystem.theme.resolveTheme
import com.github.adibfara.verse.core.model.FileNode
import com.github.adibfara.verse.core.model.FilePath
import com.github.adibfara.verse.core.persistence.EditorFontSizeStep
import com.github.adibfara.verse.core.persistence.withAdjustedEditorFontSize
import com.github.adibfara.verse.core.platform.LocationResolver
import com.github.adibfara.verse.core.platform.PlatformFolderPickerOverlay
import com.github.adibfara.verse.core.platform.PlatformKind
import com.github.adibfara.verse.core.platform.PlatformStatusBarStyle
import com.github.adibfara.verse.core.platform.SpeechRecognition
import com.github.adibfara.verse.core.platform.SystemOpener
import com.github.adibfara.verse.core.platform.currentPlatformKind
import com.github.adibfara.verse.feature.editor.EditorEvent
import com.github.adibfara.verse.feature.editor.EditorViewModel
import com.github.adibfara.verse.feature.viewer.ui.LocalSpaceImageLoader
import com.github.adibfara.verse.feature.explorer.ExplorerEvent
import com.github.adibfara.verse.feature.explorer.ExplorerViewModel
import com.github.adibfara.verse.feature.settings.ui.SettingsPanel
import com.github.adibfara.verse.feature.search.SearchAction
import com.github.adibfara.verse.feature.search.SearchMode
import com.github.adibfara.verse.feature.search.SearchViewModel
import com.github.adibfara.verse.feature.search.ui.SearchOverlay
import com.github.adibfara.verse.feature.space.SpaceRepository
import com.github.adibfara.verse.feature.space.ui.SpacePickerScreen
import com.github.adibfara.verse.feature.tabs.TabsEvent
import com.github.adibfara.verse.feature.tabs.TabsViewModel
import kotlinx.coroutines.launch
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

    PlatformStatusBarStyle(theme.isDark)

    VerseTheme(
        theme = theme,
        editorFontSize = appState.settings.editorFontSize,
        uiScale = if (isMobile) MobileUiScale else 1f,
        spacing = if (isMobile) mobileSpacing() else VerseSpacing(),
        sizes = if (isMobile) mobileSizes() else VerseSizes(),
    ) {
        Box(modifier = Modifier.fillMaxSize().background(VerseTheme.colors.window)) {
            when {
                !appState.isReady -> if (isMobile) StartupLoadingIndicator() else Unit
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
private fun StartupLoadingIndicator() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            color = VerseTheme.colors.accent,
            modifier = Modifier.size(28.dp),
            strokeWidth = 2.5.dp,
        )
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
    val speechRecognition: SpeechRecognition = koinInject()
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
        explorerState = explorerState,
    )

    val clipboard = LocalClipboardManager.current
    val focusRequester = remember { FocusRequester() }
    val imageLoader = rememberSpaceImageLoader(spaceRepository, editorState.path)
    val openLink = rememberLinkOpener(
        spaceRepository = spaceRepository,
        activeFile = editorState.path,
        onOpenPath = { tabsViewModel.onEvent(TabsEvent.OpenPath(it)) },
        onOpenExternal = { systemOpener.openUrl(it) },
        onUnresolved = { target ->
            editorViewModel.onEvent(EditorEvent.ReportError("Nothing in this space matches \"$target\""))
        },
    )

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    LaunchedEffect(registry, appState.settings.keyBindingOverrides) {
        registry.setOverrides(appState.settings.commandBindingOverrides)
    }

    val absolutePathOf: (FilePath) -> String = { path ->
        spaceRepository.current.value?.fileSystem?.absolutePathOf(path).orEmpty()
    }

    fun editorNode(): FileNode? {
        val path = editorState.path ?: return null
        return FileNode(
            path = path,
            name = path.name,
            isDirectory = false,
            kind = editorState.kind,
            lastModified = 0L,
            size = 0L,
        )
    }
    val onEditorRequestRename: () -> Unit = {
        editorNode()?.let { explorerViewModel.onEvent(ExplorerEvent.RequestRename(it)) }
    }
    val onEditorRequestMove: () -> Unit = {
        editorNode()?.let { explorerViewModel.onEvent(ExplorerEvent.RequestMove(it)) }
    }
    val onEditorRequestDelete: () -> Unit = {
        editorNode()?.let { explorerViewModel.onEvent(ExplorerEvent.RequestDelete(it)) }
    }
    val onEditorCopyPath: () -> Unit = {
        editorState.path?.let { clipboard.setText(AnnotatedString(absolutePathOf(it))) }
    }
    val onEditorShare: () -> Unit = {
        editorState.path?.let { systemOpener.shareText(it.name, editorState.value.text) }
    }
    val voiceInputScope = rememberCoroutineScope()
    val onEditorVoiceInput: (() -> Unit)? = if (speechRecognition.isSupported) {
        {
            voiceInputScope.launch {
                val recognized = speechRecognition.recognizeSpeech()
                if (!recognized.isNullOrBlank()) {
                    val value = editorState.value
                    val insertAt = value.selection.start.coerceIn(0, value.text.length)
                    val deleteEnd = value.selection.end.coerceIn(insertAt, value.text.length)
                    val updatedText = value.text.replaceRange(insertAt, deleteEnd, recognized)
                    editorViewModel.onEvent(
                        EditorEvent.TextChanged(
                            TextFieldValue(
                                text = updatedText,
                                selection = TextRange(insertAt + recognized.length),
                            ),
                        ),
                    )
                }
            }
            Unit
        }
    } else {
        null
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
            Commands.ToggleFormattingToolbar.id -> workspaceViewModel.updateSettings {
                it.copy(showFormattingToolbar = !it.showFormattingToolbar)
            }
            Commands.IncreaseFontSize.id -> workspaceViewModel.updateSettings {
                it.withAdjustedEditorFontSize(EditorFontSizeStep)
            }

            Commands.DecreaseFontSize.id -> workspaceViewModel.updateSettings {
                it.withAdjustedEditorFontSize(-EditorFontSizeStep)
            }
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
                onToggleFormattingToolbar = {
                    workspaceViewModel.updateSettings {
                        it.copy(showFormattingToolbar = !it.showFormattingToolbar)
                    }
                },
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
                onEditorCopyPath = onEditorCopyPath,
                onEditorShare = onEditorShare,
                onEditorRequestRename = onEditorRequestRename,
                onEditorRequestMove = onEditorRequestMove,
                onEditorRequestDelete = onEditorRequestDelete,
                modifier = shellModifier,
                fontSize = appState.settings.editorFontSize,
                onAdjustFontSize = { delta ->
                    workspaceViewModel.updateSettings { it.withAdjustedEditorFontSize(delta) }
                },
            )
        } else {
            MobileShell(
                appState = appState,
                explorerState = explorerState,
                tabsState = tabsState,
                editorState = editorState,
                isDarkTheme = isDarkTheme,
                onExplorerEvent = explorerViewModel::onEvent,
                onTabsEvent = tabsViewModel::onEvent,
                onEditorEvent = editorViewModel::onEvent,
                onToggleTheme = workspaceViewModel::toggleTheme,
                onOpenInDefaultApplication = {
                    editorState.path?.let { systemOpener.openInDefaultApplication(absolutePathOf(it)) }
                },
                onOpenLink = openLink,
                onEditorCopyPath = onEditorCopyPath,
                onEditorShare = onEditorShare,
                onEditorRequestRename = onEditorRequestRename,
                onEditorRequestMove = onEditorRequestMove,
                onEditorRequestDelete = onEditorRequestDelete,
                onVoiceInput = onEditorVoiceInput,
                onCopyPath = { clipboard.setText(AnnotatedString(it)) },
                absolutePathOf = absolutePathOf,
                onOpenSettings = { settingsVisible = true },
                onOpenSearch = { searchViewModel.open(SearchMode.QuickOpen) },
                recentSpaces = appState.recentSpaces,
                onOpenSpace = workspaceViewModel::openRecent,
                onAddSpace = workspaceViewModel::chooseFolder,
                modifier = shellModifier,
                fontSize = appState.settings.editorFontSize,
                onAdjustFontSize = { delta ->
                    workspaceViewModel.updateSettings { it.withAdjustedEditorFontSize(delta) }
                },
            )
        }

        if (settingsVisible) {
            SettingsPanel(
                settings = appState.settings,
                sortOrder = explorerState.sortOrder,
                followEnabled = explorerState.followEnabled,
                onSettingsChange = workspaceViewModel::updateSettings,
                onSortOrderChange = {
                    explorerViewModel.onEvent(ExplorerEvent.ChangeSortOrder(it))
                },
                onToggleFollow = {
                    explorerViewModel.onEvent(ExplorerEvent.ToggleFollowSelection)
                },
                onAddSpace = {
                    settingsVisible = false
                    workspaceViewModel.chooseFolder()
                },
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
    }
}
