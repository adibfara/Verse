package com.github.adibfara.mds.app.ui

import androidx.compose.foundation.background
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
import com.github.adibfara.mds.app.AppUiState
import com.github.adibfara.mds.app.WindowChrome
import com.github.adibfara.mds.core.designsystem.component.MdsDivider
import com.github.adibfara.mds.core.designsystem.component.MdsIconButton
import com.github.adibfara.mds.core.designsystem.component.MdsIcons
import com.github.adibfara.mds.core.designsystem.component.MdsSplitter
import com.github.adibfara.mds.core.designsystem.theme.MdsTheme
import com.github.adibfara.mds.feature.editor.EditorEvent
import com.github.adibfara.mds.feature.editor.EditorUiState
import com.github.adibfara.mds.feature.editor.ui.EditorPane
import com.github.adibfara.mds.feature.explorer.ExplorerEvent
import com.github.adibfara.mds.feature.explorer.ExplorerUiState
import com.github.adibfara.mds.core.persistence.ExplorerLayoutMode
import com.github.adibfara.mds.feature.explorer.ui.DualPaneExplorer
import com.github.adibfara.mds.feature.explorer.ui.ExplorerDialogHost
import com.github.adibfara.mds.feature.explorer.ui.FileContextMenu
import com.github.adibfara.mds.feature.explorer.ui.FileTree
import com.github.adibfara.mds.feature.tabs.TabsEvent
import com.github.adibfara.mds.feature.tabs.TabsUiState
import com.github.adibfara.mds.feature.tabs.ui.TabStrip

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
    onOpenSettings: () -> Unit,
    onOpenQuickOpen: () -> Unit,
    onOpenInDefaultApplication: () -> Unit,
    onRevealInFileManager: () -> Unit,
    onOpenLink: (String) -> Unit,
    onCopyPath: (String) -> Unit,
    absolutePathOf: (com.github.adibfara.mds.core.model.FilePath) -> String,
    onEditorCopyPath: () -> Unit,
    onEditorShare: () -> Unit,
    onEditorRequestRename: () -> Unit,
    onEditorRequestMove: () -> Unit,
    onEditorRequestDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MdsTheme.colors

    Box(modifier = modifier.fillMaxSize().background(colors.window)) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(
                tabsState = tabsState,
                isDark = isDarkTheme,
                windowChrome = windowChrome,
                onTabsEvent = onTabsEvent,
                onToggleSidebar = onToggleSidebar,
                onToggleTheme = onToggleTheme,
                onOpenSettings = onOpenSettings,
                onOpenQuickOpen = onOpenQuickOpen,
            )
            MdsDivider()

            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (appState.sidebarVisible) {
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
                    MdsSplitter(onDelta = onResizeSidebar)
                }

                EditorPane(
                    state = editorState,
                    onEvent = onEditorEvent,
                    onOpenInDefaultApplication = onOpenInDefaultApplication,
                    onRevealInFileManager = onRevealInFileManager,
                    onOpenLink = onOpenLink,
                    showStatusBar = appState.settings.showStatusBar,
                    onCopyPath = onEditorCopyPath,
                    onShare = onEditorShare,
                    onRequestRename = onEditorRequestRename,
                    onRequestMove = onEditorRequestMove,
                    onRequestDelete = onEditorRequestDelete,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }

        explorerState.contextMenu?.let { target ->
            FileContextMenu(
                target = target,
                onEvent = onExplorerEvent,
                onCopyPath = onCopyPath,
                absolutePathOf = { absolutePathOf(it.node.path) },
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

@Composable
private fun TopBar(
    tabsState: TabsUiState,
    isDark: Boolean,
    windowChrome: WindowChrome?,
    onTabsEvent: (TabsEvent) -> Unit,
    onToggleSidebar: () -> Unit,
    onToggleTheme: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenQuickOpen: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(MdsTheme.sizes.tabHeight + MdsTheme.spacing.md)
            .padding(horizontal = MdsTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MdsIconButton(
            icon = MdsIcons.PanelLeft,
            contentDescription = "Toggle sidebar",
            onClick = onToggleSidebar,
        )
        MdsIconButton(
            icon = MdsIcons.Search,
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
        MdsIconButton(
            icon = if (isDark) MdsIcons.Sun else MdsIcons.Moon,
            contentDescription = "Toggle theme",
            onClick = onToggleTheme,
        )
        MdsIconButton(
            icon = MdsIcons.Sliders,
            contentDescription = "Settings",
            onClick = onOpenSettings,
        )
        windowChrome?.let { chrome ->
            Spacer(modifier = Modifier.width(MdsTheme.spacing.sm))
            MdsIconButton(
                icon = MdsIcons.WindowMinimize,
                contentDescription = "Minimise window",
                onClick = chrome.onMinimize,
            )
            MdsIconButton(
                icon = if (chrome.isMaximized) MdsIcons.WindowRestore else MdsIcons.WindowMaximize,
                contentDescription = if (chrome.isMaximized) "Restore window" else "Maximise window",
                onClick = chrome.onToggleMaximize,
            )
            MdsIconButton(
                icon = MdsIcons.Close,
                contentDescription = "Close window",
                onClick = chrome.onClose,
            )
        }
    }
}
