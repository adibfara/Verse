package com.github.adibfara.verse.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.adibfara.verse.core.designsystem.theme.VerseTheme

@Composable
fun VerseText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = VerseTheme.typography.uiBody,
    color: Color = VerseTheme.colors.textPrimary,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Ellipsis,
) {
    Text(
        text = text,
        modifier = modifier,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = overflow,
    )
}

@Composable
fun VerseText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = VerseTheme.typography.uiBody,
    color: Color = VerseTheme.colors.textPrimary,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Ellipsis,
) {
    Text(
        text = text,
        modifier = modifier,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = overflow,
    )
}

@Composable
fun VerseIcon(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = VerseTheme.colors.textSecondary,
    size: Dp = VerseTheme.sizes.icon,
) {
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        tint = tint,
    )
}

@Composable
fun VerseIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    selectedIcon: ImageVector? = null,
    iconSize: Dp = VerseTheme.sizes.icon,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val colors = VerseTheme.colors
    val background = if (hovered && enabled) colors.surfaceHover else Color.Transparent
    val tint = when {
        !enabled -> colors.textMuted
        selected -> colors.accent
        else -> colors.textSecondary
    }
    Box(
        modifier = modifier
            .size(VerseTheme.sizes.iconButton)
            .background(background, VerseTheme.shapes.small)
            .hoverable(interactionSource, enabled)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        VerseIcon(
            icon = if (selected) selectedIcon ?: icon else icon,
            contentDescription = contentDescription,
            tint = tint,
            size = iconSize,
        )
    }
}

@Composable
fun VerseFloatingBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = VerseTheme.colors
    val shape = VerseTheme.shapes.round
    Row(
        modifier = modifier
            .height(VerseTheme.sizes.floatingBarHeight)
            .background(colors.surfaceRaised, shape)
            .border(VerseTheme.sizes.borderWidth, colors.border, shape)
            .padding(horizontal = VerseTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VerseTheme.spacing.xxs),
        content = content,
    )
}

@Composable
fun VerseDivider(modifier: Modifier = Modifier, vertical: Boolean = false) {
    val thickness = VerseTheme.sizes.borderWidth
    val sizing = if (vertical) {
        Modifier.width(thickness).fillMaxHeight()
    } else {
        Modifier.height(thickness).fillMaxWidth()
    }
    Box(modifier = modifier.then(sizing).background(VerseTheme.colors.border))
}

@Composable
fun VerseListRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    content: @Composable (hovered: Boolean) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val colors = VerseTheme.colors
    val background = when {
        selected -> colors.surfaceSelected
        hovered -> colors.surfaceHover
        else -> Color.Transparent
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(VerseTheme.sizes.rowHeight)
            .padding(horizontal = VerseTheme.spacing.xs)
            .background(background, RoundedCornerShape(4.dp))
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content(hovered)
    }
}
