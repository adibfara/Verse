package com.github.adibfara.mds.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.github.adibfara.mds.core.designsystem.theme.MdsTheme
import com.github.adibfara.mds.core.persistence.ExplorerLayoutMode
import com.github.adibfara.mds.feature.explorer.ExplorerEvent
import com.github.adibfara.mds.feature.explorer.ExplorerUiState
import com.github.adibfara.mds.feature.explorer.ui.DualPaneExplorer
import com.github.adibfara.mds.feature.explorer.ui.FileTree

@Composable
fun MobileDrawerOverlay(
    explorerState: ExplorerUiState,
    onExplorerEvent: (ExplorerEvent) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MdsTheme.colors.overlay)
                .clickable(onClick = onDismiss),
        )
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.85f)
                .align(Alignment.CenterStart)
                .clickable(onClick = {}),
        ) {
            if (explorerState.layoutMode == ExplorerLayoutMode.DualPane) {
                DualPaneExplorer(
                    state = explorerState,
                    onEvent = onExplorerEvent,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                FileTree(
                    state = explorerState,
                    onEvent = onExplorerEvent,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
