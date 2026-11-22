package com.github.adibfara.verse.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalTextToolbar
import com.github.adibfara.verse.app.AppUiState
import com.github.adibfara.verse.core.designsystem.component.VerseDivider
import com.github.adibfara.verse.core.designsystem.component.VerseFloatingBar
import com.github.adibfara.verse.core.designsystem.component.VerseIconButton
import com.github.adibfara.verse.core.designsystem.component.VerseIcons
import com.github.adibfara.verse.core.designsystem.component.VerseText
import com.github.adibfara.verse.core.designsystem.theme.VerseTheme
import com.github.adibfara.verse.core.model.FileNode
import com.github.adibfara.verse.core.model.FilePath
import com.github.adibfara.verse.core.model.RecentSpace
import com.github.adibfara.verse.core.platform.PlatformBackHandler
import com.github.adibfara.verse.feature.editor.EditorEvent
import com.github.adibfara.verse.feature.editor.EditorUiState
import com.github.adibfara.verse.feature.editor.ui.EditorPane
import com.github.adibfara.verse.feature.editor.ui.PopupTextToolbar
import com.github.adibfara.verse.feature.editor.ui.TextToolbarPopup
import com.github.adibfara.verse.feature.explorer.ExplorerEvent
import com.github.adibfara.verse.feature.explorer.ExplorerUiState
import com.github.adibfara.verse.feature.explorer.ui.EmptyAreaActionsBottomSheet
import com.github.adibfara.verse.feature.explorer.ui.ExplorerDialogHost
import com.github.adibfara.verse.feature.explorer.ui.FileActionsBottomSheet
import com.github.adibfara.verse.feature.tabs.TabsEvent
import com.github.adibfara.verse.feature.tabs.TabsUiState
import com.github.adibfara.verse.feature.tabs.ui.TabsGridScreen

private sealed interface MobileScreen {
    data object Note : MobileScreen
    data object TabsGrid : MobileScreen
}

@Composable
fun MobileShell(
    appState: AppUiState,
    explorerState: ExplorerUiState,
    tabsState: TabsUiState,
    editorState: EditorUiState,
    isDarkTheme: Boolean,
    onExplorerEvent: (ExplorerEvent) -> Unit,
    onTabsEvent: (TabsEvent) -> Unit,
    onEditorEvent: (EditorEvent) -> Unit,
    onToggleTheme: () -> Unit,
    onOpenInDefaultApplication: () -> Unit,
    onOpenLink: (String) -> Unit,
    onEditorCopyPath: () -> Unit,
    onEditorShare: () -> Unit,
    onEditorRequestRename: () -> Unit,
    onEditorRequestMove: () -> Unit,
    onEditorRequestDelete: () -> Unit,
    onVoiceInput: (() -> Unit)? = null,
    onCopyPath: (String) -> Unit = {},
    absolutePathOf: (FilePath) -> String = { "" },
    onOpenSettings: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    recentSpaces: List<RecentSpace> = emptyList(),
    onOpenSpace: (RecentSpace) -> Unit = {},
    onAddSpace: () -> Unit = {},
    modifier: Modifier = Modifier,
    fontSize: Float? = null,
    onAdjustFontSize: ((Float) -> Unit)? = null,
) {
    var screen: MobileScreen by remember { mutableStateOf(MobileScreen.Note) }
    var previousScreen: MobileScreen by remember { mutableStateOf(MobileScreen.Note) }
    var drawerOpen by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    fun openDrawer() {
        keyboardController?.hide()
        focusManager.clearFocus(force = true)
        drawerOpen = true
    }

    fun openFile(node: FileNode) {
        onExplorerEvent(ExplorerEvent.Activate(node))
        if (node.isDirectory) return
        screen = MobileScreen.Note
        drawerOpen = false
    }

    fun openTabsGrid() {
        previousScreen = screen
        screen = MobileScreen.TabsGrid
    }

    val backHandlerEnabled = drawerOpen || screen == MobileScreen.TabsGrid
    PlatformBackHandler(enabled = backHandlerEnabled) {
        when {
            drawerOpen -> drawerOpen = false
            screen == MobileScreen.TabsGrid -> screen = previousScreen
            else -> Unit
        }
    }

    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val textToolbar = remember { PopupTextToolbar() }

    CompositionLocalProvider(LocalTextToolbar provides textToolbar) {
        Box(modifier = modifier.fillMaxSize().background(VerseTheme.colors.sidebar)) {
            val pushFraction by animateFloatAsState(targetValue = if (drawerOpen) 0.8f else 0f)

            if (drawerOpen || pushFraction > 0f) {
                MobileDrawerPanel(
                    explorerState = explorerState,
                    onExplorerEvent = { event ->
                        if (event is ExplorerEvent.Activate) openFile(event.node) else onExplorerEvent(event)
                    },
                    recentSpaces = recentSpaces,
                    onOpenSpace = { recent ->
                        drawerOpen = false
                        onOpenSpace(recent)
                    },
                    onAddSpace = {
                        drawerOpen = false
                        onAddSpace()
                    },
                    onOpenSettings = {
                        drawerOpen = false
                        onOpenSettings()
                    },
                    modifier = Modifier.fillMaxWidth(0.8f).align(Alignment.CenterStart),
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationX = size.width * pushFraction },
            ) {
                Box(modifier = Modifier.fillMaxSize().background(VerseTheme.colors.surface)) {
                    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                        when (screen) {
                            MobileScreen.Note -> {
                                EditorPane(
                                    state = editorState,
                                    onEvent = onEditorEvent,
                                    onOpenInDefaultApplication = onOpenInDefaultApplication,
                                    onRevealInFileManager = onOpenInDefaultApplication,
                                    onOpenLink = onOpenLink,
                                    showStatusBar = appState.settings.showStatusBar && imeVisible,
                                    showKeyboardToolbar = imeVisible,
                                    onCopyPath = onEditorCopyPath,
                                    onShare = onEditorShare,
                                    onRequestRename = onEditorRequestRename,
                                    onRequestMove = onEditorRequestMove,
                                    onRequestDelete = onEditorRequestDelete,
                                    headerLeading = {
                                        VerseFloatingBar {
                                            VerseIconButton(
                                                icon = VerseIcons.PanelLeft,
                                                contentDescription = "Open menu",
                                                onClick = { openDrawer() },
                                            )
                                        }
                                    },
                                    isDarkTheme = isDarkTheme,
                                    onToggleTheme = onToggleTheme,
                                    showBreadcrumbPath = false,
                                    showRevealAction = false,
                                    onVoiceInput = onVoiceInput,
                                    emptyStateTitle = explorerState.spaceName,
                                    contentHorizontalPadding = VerseTheme.spacing.lg,
                                    compactHeader = true,
                                    showFormatPopup = false,
                                    onOpenSettings = onOpenSettings,
                                    modifier = Modifier.weight(1f),
                                    fontSize = fontSize,
                                    onAdjustFontSize = onAdjustFontSize,
                                )
                            }

                            MobileScreen.TabsGrid -> {
                                MobileTopBar(
                                    title = "Tabs",
                                    canGoBack = true,
                                    isDarkTheme = isDarkTheme,
                                    onBack = { screen = previousScreen },
                                    onMenu = null,
                                    onToggleTheme = onToggleTheme,
                                )
                                VerseDivider()
                                TabsGridScreen(
                                    tabs = tabsState.tabs,
                                    activeTabId = tabsState.activeTabId,
                                    onActivate = { id ->
                                        onTabsEvent(TabsEvent.Activate(id))
                                        screen = MobileScreen.Note
                                    },
                                    onClose = { id -> onTabsEvent(TabsEvent.Close(id)) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }

                    if (screen == MobileScreen.Note && !imeVisible) {
                        MobileBottomBar(
                            tabCount = tabsState.tabs.size,
                            onSearch = onOpenSearch,
                            onAdd = { onExplorerEvent(ExplorerEvent.RequestNewNote(null)) },
                            onTabs = { openTabsGrid() },
                            onMenu = { openDrawer() },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(bottom = VerseTheme.sizes.floatingBarInset),
                        )
                    }
                }

                if (drawerOpen) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(VerseTheme.colors.overlay)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { drawerOpen = false },
                    )
                }
            }

            explorerState.dialog?.let { dialog ->
                ExplorerDialogHost(dialog = dialog, onEvent = onExplorerEvent, onChooseFolder = {})
            }

            explorerState.contextMenu?.let { target ->
                FileActionsBottomSheet(
                    target = target,
                    onEvent = onExplorerEvent,
                    onCopyPath = onCopyPath,
                    absolutePathOf = { absolutePathOf(it.node.path) },
                    isFavorite = target.node.path in explorerState.favoritedPaths,
                )
            }

            if (explorerState.emptyAreaMenu != null) {
                EmptyAreaActionsBottomSheet(onEvent = onExplorerEvent)
            }

            TextToolbarPopup(textToolbar)
        }
    }
}

@Composable
private fun MobileTopBar(
    title: String,
    canGoBack: Boolean,
    isDarkTheme: Boolean,
    onBack: () -> Unit,
    onMenu: (() -> Unit)?,
    onToggleTheme: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(VerseTheme.sizes.mobileTopBarHeight)
            .background(VerseTheme.colors.sidebar)
            .padding(horizontal = VerseTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VerseTheme.spacing.sm),
    ) {
        if (canGoBack) {
            VerseIconButton(
                icon = VerseIcons.ArrowLeft,
                contentDescription = "Back",
                onClick = onBack,
            )
        } else if (onMenu != null) {
            VerseIconButton(
                icon = VerseIcons.Menu,
                contentDescription = "Open menu",
                onClick = onMenu,
            )
        }
        VerseText(
            text = title,
            modifier = Modifier.weight(1f),
            style = VerseTheme.typography.uiTitle,
            color = VerseTheme.colors.textPrimary,
            maxLines = 1,
        )
        VerseIconButton(
            icon = if (isDarkTheme) VerseIcons.Sun else VerseIcons.Moon,
            contentDescription = "Toggle theme",
            onClick = onToggleTheme,
        )
    }
}
