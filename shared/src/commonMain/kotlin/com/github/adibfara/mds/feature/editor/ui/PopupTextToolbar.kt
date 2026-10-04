package com.github.adibfara.mds.feature.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.github.adibfara.mds.core.designsystem.component.MdsText
import com.github.adibfara.mds.core.designsystem.theme.MdsTheme

data class TextToolbarMenuState(
    val rect: Rect,
    val onCopy: (() -> Unit)?,
    val onPaste: (() -> Unit)?,
    val onCut: (() -> Unit)?,
    val onSelectAll: (() -> Unit)?,
)

class PopupTextToolbar : TextToolbar {
    var menuState by mutableStateOf<TextToolbarMenuState?>(null)
        private set

    override var status: TextToolbarStatus = TextToolbarStatus.Hidden
        private set

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
    ) {
        menuState = TextToolbarMenuState(rect, onCopyRequested, onPasteRequested, onCutRequested, onSelectAllRequested)
        status = TextToolbarStatus.Shown
    }

    override fun hide() {
        menuState = null
        status = TextToolbarStatus.Hidden
    }
}

@Composable
fun TextToolbarPopup(toolbar: PopupTextToolbar) {
    val state = toolbar.menuState ?: return
    val gap = with(LocalDensity.current) { MdsTheme.spacing.xs.roundToPx() }
    val provider = remember(state.rect, gap) { AnchorPositionProvider(state.rect, gap) }
    Popup(popupPositionProvider = provider, properties = PopupProperties(focusable = false)) {
        val colors = MdsTheme.colors
        Row(
            modifier = Modifier
                .background(colors.surfaceRaised, MdsTheme.shapes.medium)
                .border(MdsTheme.sizes.borderWidth, colors.border, MdsTheme.shapes.medium)
                .padding(MdsTheme.spacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MdsTheme.spacing.xs),
        ) {
            state.onCut?.let { ToolbarAction("Cut", it) }
            state.onCopy?.let { ToolbarAction("Copy", it) }
            state.onPaste?.let { ToolbarAction("Paste", it) }
            state.onSelectAll?.let { ToolbarAction("Select all", it) }
        }
    }
}

@Composable
private fun ToolbarAction(label: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    Box(
        modifier = Modifier
            .background(
                if (hovered) MdsTheme.colors.surfaceHover else Color.Transparent,
                MdsTheme.shapes.small,
            )
            .hoverable(interactionSource)
            .pointerInput(Unit) { detectTapGestures { onClick() } }
            .padding(horizontal = MdsTheme.spacing.sm, vertical = MdsTheme.spacing.xs),
    ) {
        MdsText(
            text = label,
            style = MdsTheme.typography.uiBody,
            color = MdsTheme.colors.textPrimary,
            maxLines = 1,
        )
    }
}
