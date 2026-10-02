package com.github.adibfara.mds.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.adibfara.mds.app.AppUiState
import com.github.adibfara.mds.core.designsystem.component.MdsDivider
import com.github.adibfara.mds.core.designsystem.component.MdsIcon
import com.github.adibfara.mds.core.designsystem.component.MdsIconButton
import com.github.adibfara.mds.core.designsystem.component.MdsIcons
import com.github.adibfara.mds.core.designsystem.component.MdsText
import com.github.adibfara.mds.core.designsystem.theme.MdsTheme
import com.github.adibfara.mds.core.model.FileNode
import com.github.adibfara.mds.core.model.FilePath
import com.github.adibfara.mds.core.platform.PlatformBackHandler
import com.github.adibfara.mds.feature.editor.EditorEvent
import com.github.adibfara.mds.feature.editor.EditorUiState
import com.github.adibfara.mds.feature.editor.ui.EditorPane
import com.github.adibfara.mds.feature.explorer.ExplorerEvent
import com.github.adibfara.mds.feature.explorer.ExplorerUiState
import com.github.adibfara.mds.feature.tabs.TabsEvent
import com.github.adibfara.mds.feature.tabs.TabsUiState

@Composable
fun MobileShell(
    appState: AppUiState,
    explorerState: ExplorerUiState,
    tabsState: TabsUiState,
    editorState: EditorUiState,
    onExplorerEvent: (ExplorerEvent) -> Unit,
    onTabsEvent: (TabsEvent) -> Unit,
    onEditorEvent: (EditorEvent) -> Unit,
    onToggleTheme: () -> Unit,
    onOpenInDefaultApplication: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var browsePath by remember { mutableStateOf(FilePath.Root) }
    val showEditor = tabsState.activeTab != null

    PlatformBackHandler(enabled = showEditor || !browsePath.isRoot) {
        if (showEditor) {
            onTabsEvent(TabsEvent.CloseActive)
        } else {
            browsePath = browsePath.parent ?: FilePath.Root
        }
    }

    Column(modifier = modifier.fillMaxSize().safeContentPadding()) {
        if (showEditor) {
            MobileTopBar(
                title = editorState.path?.nameWithoutExtension.orEmpty(),
                canGoBack = true,
                onBack = { onTabsEvent(TabsEvent.CloseActive) },
                onToggleTheme = onToggleTheme,
            )
            MdsDivider()
            EditorPane(
                state = editorState,
                onEvent = onEditorEvent,
                onOpenInDefaultApplication = onOpenInDefaultApplication,
                onRevealInFileManager = onOpenInDefaultApplication,
                showStatusBar = appState.settings.showStatusBar,
                modifier = Modifier.weight(1f),
            )
        } else {
            MobileTopBar(
                title = if (browsePath.isRoot) explorerState.spaceName else browsePath.name,
                canGoBack = !browsePath.isRoot,
                onBack = { browsePath = browsePath.parent ?: FilePath.Root },
                onToggleTheme = onToggleTheme,
            )
            MdsDivider()
            FolderList(
                nodes = explorerState.rows
                    .map { it.node }
                    .filter { it.path.parent?.value == browsePath.value },
                onOpenFolder = { node ->
                    onExplorerEvent(ExplorerEvent.ToggleFolder(node.path))
                    browsePath = node.path
                },
                onOpenFile = { node -> onExplorerEvent(ExplorerEvent.Activate(node)) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MobileTopBar(
    title: String,
    canGoBack: Boolean,
    onBack: () -> Unit,
    onToggleTheme: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(MdsTheme.colors.sidebar)
            .padding(horizontal = MdsTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MdsTheme.spacing.sm),
    ) {
        if (canGoBack) {
            MdsIconButton(
                icon = MdsIcons.ArrowLeft,
                contentDescription = "Back",
                onClick = onBack,
            )
        }
        MdsText(
            text = title,
            modifier = Modifier.weight(1f),
            style = MdsTheme.typography.uiTitle,
            color = MdsTheme.colors.textPrimary,
            maxLines = 1,
        )
        MdsIconButton(
            icon = MdsIcons.Moon,
            contentDescription = "Toggle theme",
            onClick = onToggleTheme,
        )
    }
}

@Composable
private fun FolderList(
    nodes: List<FileNode>,
    onOpenFolder: (FileNode) -> Unit,
    onOpenFile: (FileNode) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (nodes.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            MdsText(
                text = "Nothing here",
                style = MdsTheme.typography.uiBody,
                color = MdsTheme.colors.textMuted,
            )
        }
        return
    }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(items = nodes, key = { it.path.value }) { node ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (node.isDirectory) onOpenFolder(node) else onOpenFile(node)
                    }
                    .padding(horizontal = MdsTheme.spacing.lg, vertical = MdsTheme.spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MdsTheme.spacing.md),
            ) {
                Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                    MdsIcon(
                        icon = if (node.isDirectory) MdsIcons.Folder else MdsIcons.FileText,
                        contentDescription = null,
                        tint = MdsTheme.colors.textMuted,
                    )
                }
                MdsText(
                    text = if (node.isDirectory) node.name else node.path.nameWithoutExtension,
                    modifier = Modifier.weight(1f),
                    style = MdsTheme.typography.uiBody,
                    color = MdsTheme.colors.textPrimary,
                    maxLines = 1,
                )
                if (node.isDirectory) {
                    MdsIcon(
                        icon = MdsIcons.ChevronRight,
                        contentDescription = null,
                        tint = MdsTheme.colors.textMuted,
                    )
                }
            }
        }
    }
}
