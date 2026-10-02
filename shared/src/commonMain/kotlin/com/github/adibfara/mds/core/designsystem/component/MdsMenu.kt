package com.github.adibfara.mds.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.github.adibfara.mds.core.designsystem.theme.MdsTheme

sealed interface MenuEntry {

    data class Action(
        val label: String,
        val icon: ImageVector? = null,
        val shortcut: String? = null,
        val enabled: Boolean = true,
        val danger: Boolean = false,
        val onClick: () -> Unit,
    ) : MenuEntry

    data object Separator : MenuEntry
}

@Composable
fun MdsContextMenu(
    entries: List<MenuEntry>,
    offset: IntOffset,
    onDismiss: () -> Unit,
) {
    val colors = MdsTheme.colors
    Popup(
        alignment = Alignment.TopStart,
        offset = offset,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 210.dp, max = 320.dp)
                .background(colors.surfaceRaised, MdsTheme.shapes.medium)
                .border(MdsTheme.sizes.borderWidth, colors.border, MdsTheme.shapes.medium)
                .padding(vertical = MdsTheme.spacing.xs),
        ) {
            entries.forEach { entry ->
                when (entry) {
                    is MenuEntry.Separator -> MenuSeparator()
                    is MenuEntry.Action -> MenuActionRow(entry = entry, onDismiss = onDismiss)
                }
            }
        }
    }
}

@Composable
private fun MenuSeparator() {
    MdsDivider(
        modifier = Modifier.padding(
            horizontal = MdsTheme.spacing.sm,
            vertical = MdsTheme.spacing.xs,
        ),
    )
}

@Composable
private fun MenuActionRow(entry: MenuEntry.Action, onDismiss: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val colors = MdsTheme.colors
    val contentColor = when {
        !entry.enabled -> colors.textMuted
        entry.danger -> colors.danger
        else -> colors.textPrimary
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MdsTheme.spacing.xs)
            .background(
                if (hovered && entry.enabled) colors.surfaceHover else Color.Transparent,
                MdsTheme.shapes.small,
            )
            .hoverable(interactionSource, entry.enabled)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = entry.enabled,
            ) {
                onDismiss()
                entry.onClick()
            }
            .padding(horizontal = MdsTheme.spacing.sm, vertical = MdsTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(16.dp), contentAlignment = Alignment.Center) {
            entry.icon?.let {
                MdsIcon(icon = it, contentDescription = null, tint = contentColor)
            }
        }
        Spacer(modifier = Modifier.padding(horizontal = MdsTheme.spacing.xs))
        MdsText(
            text = entry.label,
            modifier = Modifier.weight(1f),
            style = MdsTheme.typography.uiBody,
            color = contentColor,
            maxLines = 1,
        )
        entry.shortcut?.let { shortcut ->
            Spacer(modifier = Modifier.padding(horizontal = MdsTheme.spacing.xs))
            MdsText(
                text = shortcut,
                style = MdsTheme.typography.uiSmall,
                color = colors.textMuted,
                maxLines = 1,
            )
        }
    }
}
