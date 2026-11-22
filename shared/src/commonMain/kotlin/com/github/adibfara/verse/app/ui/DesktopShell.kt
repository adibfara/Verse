package com.github.adibfara.verse.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.adibfara.verse.app.AppUiState
import com.github.adibfara.verse.app.WindowChrome
import com.github.adibfara.verse.core.designsystem.component.VerseDivider
import com.github.adibfara.verse.core.designsystem.component.VerseIconButton
import com.github.adibfara.verse.core.designsystem.component.VerseIcons
import com.github.adibfara.verse.core.designsystem.component.VerseSplitter
import com.github.adibfara.verse.core.designsystem.theme.VerseTheme
import com.github.adibfara.verse.feature.editor.EditorEvent
import com.github.adibfara.verse.feature.editor.EditorUiState
import com.github.adibfara.verse.feature.editor.ui.EditorPane
import com.github.adibfara.verse.feature.explorer.ExplorerEvent
import com.github.adibfara.verse.feature.explorer.ExplorerUiState
import com.github.adibfara.verse.core.persistence.ExplorerLayoutMode
import com.github.adibfara.verse.feature.explorer.ui.DualPaneExplorer
import com.github.adibfara.verse.feature.explorer.ui.EmptyAreaContextMenu
import com.github.adibfara.verse.feature.explorer.ui.ExplorerDialogHost
import com.github.adibfara.verse.feature.explorer.ui.FileContextMenu
import com.github.adibfara.verse.feature.explorer.ui.FileTree
import com.github.adibfara.verse.feature.tabs.TabsEvent
import com.github.adibfara.verse.feature.tabs.TabsUiState
import com.github.adibfara.verse.feature.tabs.ui.TabStrip

@Composable
fun DesktopShell(
    appState: AppUiState,
    explorerState: ExplorerUiState,
    tabsState: TabsUiState,
    editorState: EditorUiState,
    isDarkTheme: Boolean,
    windowChrome: WindowChrome?,
    onExplorerEvent: (ExplorerEvent) -> Unit,
    onTabsEvent: (TabsEvent) -> Unit,
    onEditorEvent: (EditorEvent) -> Unit,
    onToggleSidebar: () -> Unit,
    onResizeSidebar: (Float) -> Unit,
    onToggleTheme: () -> Unit,
    onToggleFormattingToolbar: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenQuickOpen: () -> Unit,
    onOpenInDefaultApplication: () -> Unit,
    onRevealInFileManager: () -> Unit,
    onOpenLink: (String) -> Unit,
    onCopyPath: (String) -> Unit,
    absolutePathOf: (com.github.adibfara.verse.core.model.FilePath) -> String,
    onEditorCopyPath: () -> Unit,
    onEditorShare: () -> Unit,
    onEditorRequestRename: () -> Unit,
    onEditorRequestMove: () -> Unit,
    onEditorRequestDelete: () -> Unit,
    modifier: Modifier = Modifier,
    fontSize: Float? = null,
    onAdjustFontSize: ((Float) -> Unit)? = null,
) {
    val colors = VerseTheme.colors
    val windowShapeModifier = if (windowChrome != null && !windowChrome.isMaximized) {
        Modifier.clip(VerseTheme.shapes.window)
    } else {
        Modifier
    }

    Box(modifier = modifier.fillMaxSize().then(windowShapeModifier).background(colors.window)) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(
                tabsState = tabsState,
                isDark = isDarkTheme,
                showFormattingToolbar = appState.settings.showFormattingToolbar,
                windowChrome = windowChrome,
                onTabsEvent = onTabsEvent,
                onToggleSidebar = onToggleSidebar,
                onToggleTheme = onToggleTheme,
                onToggleFormattingToolbar = onToggleFormattingToolbar,
                onOpenSettings = onOpenSettings,
                onOpenQuickOpen = onOpenQuickOpen,
            )
            VerseDivider()

            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                AnimatedVisibility(
                    visible = appState.sidebarVisible,
                    enter = SidebarEnter,
                    exit = SidebarExit,
                ) {
                    Row {
                        if (explorerState.layoutMode == ExplorerLayoutMode.DualPane) {
                            DualPaneExplorer(
                                state = explorerState,
                                onEvent = onExplorerEvent,
                                modifier = Modifier.width(appState.sidebarWidth.dp * 1.6f).fillMaxHeight(),
                            )
                        } else {
                            FileTree(
                                state = explorerState,
                                onEvent = onExplorerEvent,
                                modifier = Modifier.width(appState.sidebarWidth.dp).fillMaxHeight(),
                            )
                        }
                        VerseSplitter(onDelta = onResizeSidebar)
                    }
                }

                EditorPane(
                    state = editorState,
                    onEvent = onEditorEvent,
                    onOpenInDefaultApplication = onOpenInDefaultApplication,
                    onRevealInFileManager = onRevealInFileManager,
                    onOpenLink = onOpenLink,
                    showStatusBar = appState.settings.showStatusBar,
                    showFormattingToolbar = appState.settings.showFormattingToolbar,
                    centerBreadcrumb = true,
                    onCopyPath = onEditorCopyPath,
                    onShare = onEditorShare,
                    onRequestRename = onEditorRequestRename,
                    onRequestMove = onEditorRequestMove,
                    onRequestDelete = onEditorRequestDelete,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    fontSize = fontSize,
                    onAdjustFontSize = onAdjustFontSize,
                )
            }
        }

        explorerState.contextMenu?.let { target ->
            FileContextMenu(
                target = target,
                onEvent = onExplorerEvent,
                onCopyPath = onCopyPath,
                absolutePathOf = { absolutePathOf(it.node.path) },
                isFavorite = target.node.path in explorerState.favoritedPaths,
            )
        }

        explorerState.emptyAreaMenu?.let { target ->
            EmptyAreaContextMenu(
                target = target,
                onEvent = onExplorerEvent,
            )
        }

        explorerState.dialog?.let { dialog ->
            ExplorerDialogHost(
                dialog = dialog,
                onEvent = onExplorerEvent,
                onChooseFolder = {},
            )
        }
    }
}

private const val SidebarAnimationMillis = 220
private const val SidebarFadeMillis = 120

private val SidebarEnter: EnterTransition =
    expandHorizontally(animationSpec = tween(SidebarAnimationMillis)) +
        fadeIn(animationSpec = tween(SidebarFadeMillis))

private val SidebarExit: ExitTransition =
    shrinkHorizontally(animationSpec = tween(SidebarAnimationMillis)) +
        fadeOut(animationSpec = tween(SidebarFadeMillis))

@Composable
private fun TopBar(
    tabsState: TabsUiState,
    isDark: Boolean,
    showFormattingToolbar: Boolean,
    windowChrome: WindowChrome?,
    onTabsEvent: (TabsEvent) -> Unit,
    onToggleSidebar: () -> Unit,
    onToggleTheme: () -> Unit,
    onToggleFormattingToolbar: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenQuickOpen: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(VerseTheme.sizes.tabHeight + VerseTheme.spacing.md)
            .padding(horizontal = VerseTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VerseIconButton(
            icon = VerseIcons.PanelLeft,
            contentDescription = "Toggle sidebar",
            onClick = onToggleSidebar,
        )
        VerseIconButton(
            icon = VerseIcons.Search,
            contentDescription = "Quick open",
            onClick = onOpenQuickOpen,
        )
        Box(modifier = Modifier.weight(1f)) {
            if (windowChrome == null) {
                TabStrip(state = tabsState, onEvent = onTabsEvent)
            } else {
                windowChrome.draggableArea {
                    TabStrip(state = tabsState, onEvent = onTabsEvent)
                }
            }
        }
        VerseIconButton(
            icon = if (isDark) VerseIcons.Sun else VerseIcons.Moon,
            contentDescription = "Toggle theme",
            onClick = onToggleTheme,
        )
        VerseIconButton(
            icon = VerseIcons.TextFormat,
            contentDescription = "Toggle formatting toolbar",
            onClick = onToggleFormattingToolbar,
            selected = showFormattingToolbar,
        )
        VerseIconButton(
            icon = VerseIcons.Sliders,
            contentDescription = "Settings",
            onClick = onOpenSettings,
        )
        windowChrome?.let { chrome ->
            Spacer(modifier = Modifier.width(VerseTheme.spacing.sm))
            VerseIconButton(
                icon = VerseIcons.WindowMinimize,
                contentDescription = "Minimise window",
                onClick = chrome.onMinimize,
            )
            VerseIconButton(
                icon = if (chrome.isMaximized) VerseIcons.WindowRestore else VerseIcons.WindowMaximize,
                contentDescription = if (chrome.isMaximized) "Restore window" else "Maximise window",
                onClick = chrome.onToggleMaximize,
            )
            VerseIconButton(
                icon = VerseIcons.Close,
                contentDescription = "Close window",
                onClick = chrome.onClose,
            )
        }
    }
}
