package com.github.adibfara.mds.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import com.github.adibfara.mds.core.designsystem.theme.MdsTheme

@Composable
fun MdsText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MdsTheme.typography.uiBody,
    color: Color = MdsTheme.colors.textPrimary,
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
fun MdsText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = MdsTheme.typography.uiBody,
    color: Color = MdsTheme.colors.textPrimary,
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
fun MdsIcon(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = MdsTheme.colors.textSecondary,
    size: Dp = MdsTheme.sizes.icon,
) {
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        tint = tint,
    )
}

@Composable
fun MdsIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    iconSize: Dp = MdsTheme.sizes.icon,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val colors = MdsTheme.colors
    val background = when {
        selected -> colors.surfaceSelected
        hovered && enabled -> colors.surfaceHover
        else -> Color.Transparent
    }
    val tint = when {
        !enabled -> colors.textMuted
        selected -> colors.textPrimary
        else -> colors.textSecondary
    }
    Box(
        modifier = modifier
            .size(MdsTheme.sizes.iconButton)
            .background(background, MdsTheme.shapes.small)
            .hoverable(interactionSource, enabled)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        MdsIcon(icon = icon, contentDescription = contentDescription, tint = tint, size = iconSize)
    }
}

@Composable
fun MdsDivider(modifier: Modifier = Modifier, vertical: Boolean = false) {
    val thickness = MdsTheme.sizes.borderWidth
    val sizing = if (vertical) {
        Modifier.width(thickness).fillMaxHeight()
    } else {
        Modifier.height(thickness).fillMaxWidth()
    }
    Box(modifier = modifier.then(sizing).background(MdsTheme.colors.border))
}

@Composable
fun MdsListRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    content: @Composable (hovered: Boolean) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val colors = MdsTheme.colors
    val background = when {
        selected -> colors.surfaceSelected
        hovered -> colors.surfaceHover
        else -> Color.Transparent
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(MdsTheme.sizes.rowHeight)
            .padding(horizontal = MdsTheme.spacing.xs)
            .background(background, RoundedCornerShape(4.dp))
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content(hovered)
    }
}
