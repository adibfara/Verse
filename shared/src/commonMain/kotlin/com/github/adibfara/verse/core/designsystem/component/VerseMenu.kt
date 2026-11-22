package com.github.adibfara.verse.core.designsystem.component

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
import com.github.adibfara.verse.core.designsystem.theme.VerseTheme

sealed interface MenuEntry {

    data class Action(
        val label: String,
        val icon: ImageVector? = null,
        val shortcut: String? = null,
        val enabled: Boolean = true,
        val danger: Boolean = false,
        val onClick: () -> Unit,
    ) : MenuEntry

    data class Stepper(
        val label: String,
        val value: String,
        val onDecrease: () -> Unit,
        val onIncrease: () -> Unit,
        val decreaseEnabled: Boolean = true,
        val increaseEnabled: Boolean = true,
    ) : MenuEntry

    data object Separator : MenuEntry
}

@Composable
fun VerseContextMenu(
    entries: List<MenuEntry>,
    offset: IntOffset,
    onDismiss: () -> Unit,
    alignment: Alignment = Alignment.TopStart,
) {
    val colors = VerseTheme.colors
    Popup(
        alignment = alignment,
        offset = offset,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 210.dp, max = 320.dp)
                .background(colors.surfaceRaised, VerseTheme.shapes.medium)
                .border(VerseTheme.sizes.borderWidth, colors.border, VerseTheme.shapes.medium)
                .padding(vertical = VerseTheme.spacing.xs),
        ) {
            entries.forEach { entry ->
                when (entry) {
                    is MenuEntry.Separator -> MenuSeparator()
                    is MenuEntry.Action -> MenuActionRow(entry = entry, onDismiss = onDismiss)
                    is MenuEntry.Stepper -> MenuStepperRow(entry = entry)
                }
            }
        }
    }
}

@Composable
internal fun MenuStepperRow(entry: MenuEntry.Stepper) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = VerseTheme.spacing.xs)
            .padding(
                start = VerseTheme.spacing.sm,
                end = VerseTheme.spacing.xs,
                top = VerseTheme.spacing.xxs,
                bottom = VerseTheme.spacing.xxs,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VerseText(
            text = entry.label,
            modifier = Modifier.weight(1f),
            style = VerseTheme.typography.uiBody,
            color = VerseTheme.colors.textPrimary,
            maxLines = 1,
        )
        VerseStepper(
            value = entry.value,
            onDecrease = entry.onDecrease,
            onIncrease = entry.onIncrease,
            decreaseEnabled = entry.decreaseEnabled,
            increaseEnabled = entry.increaseEnabled,
        )
    }
}

@Composable
internal fun MenuSeparator() {
    VerseDivider(
        modifier = Modifier.padding(
            horizontal = VerseTheme.spacing.sm,
            vertical = VerseTheme.spacing.xs,
        ),
    )
}

@Composable
internal fun MenuActionRow(entry: MenuEntry.Action, onDismiss: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val colors = VerseTheme.colors
    val contentColor = when {
        !entry.enabled -> colors.textMuted
        entry.danger -> colors.danger
        else -> colors.textPrimary
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = VerseTheme.spacing.xs)
            .background(
                if (hovered && entry.enabled) colors.surfaceHover else Color.Transparent,
                VerseTheme.shapes.small,
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
            .padding(horizontal = VerseTheme.spacing.sm, vertical = VerseTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(16.dp), contentAlignment = Alignment.Center) {
            entry.icon?.let {
                VerseIcon(icon = it, contentDescription = null, tint = contentColor)
            }
        }
        Spacer(modifier = Modifier.padding(horizontal = VerseTheme.spacing.xs))
        VerseText(
            text = entry.label,
            modifier = Modifier.weight(1f),
            style = VerseTheme.typography.uiBody,
            color = contentColor,
            maxLines = 1,
        )
        entry.shortcut?.let { shortcut ->
            Spacer(modifier = Modifier.padding(horizontal = VerseTheme.spacing.xs))
            VerseText(
                text = shortcut,
                style = VerseTheme.typography.uiSmall,
                color = colors.textMuted,
                maxLines = 1,
            )
        }
    }
}
