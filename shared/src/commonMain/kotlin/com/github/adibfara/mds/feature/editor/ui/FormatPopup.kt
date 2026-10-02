package com.github.adibfara.mds.feature.editor.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.github.adibfara.mds.core.designsystem.component.MdsIcon
import com.github.adibfara.mds.core.designsystem.component.MdsIcons
import com.github.adibfara.mds.core.designsystem.component.MdsText
import com.github.adibfara.mds.core.designsystem.theme.MdsTheme
import com.github.adibfara.mds.core.markdown.FormatAction
import com.github.adibfara.mds.core.markdown.InlineFormat
import com.github.adibfara.mds.core.markdown.LineMarkerKind
import com.github.adibfara.mds.core.markdown.MdRange
import kotlin.math.roundToInt

private const val PopupFadeMillis = 130

internal enum class FormatPopupKind {
    Inline,
    LineMarker,
}

internal data class FormatPopupState(
    val kind: FormatPopupKind,
    val range: MdRange,
    val anchor: Rect,
    val formats: Set<InlineFormat>,
    val headingLevel: Int,
    val lineMarker: LineMarkerKind,
)

@Composable
internal fun FormatPopup(
    state: FormatPopupState,
    onHoverChange: (Boolean) -> Unit,
    onAction: (FormatAction) -> Unit,
) {
    val gap = with(LocalDensity.current) { MdsTheme.spacing.xs.roundToPx() }
    val provider = remember(state.anchor, gap) { AnchorPositionProvider(state.anchor, gap) }
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val appearance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        appearance.animateTo(1f, tween(durationMillis = PopupFadeMillis))
    }
    LaunchedEffect(hovered) { onHoverChange(hovered) }
    DisposableEffect(Unit) { onDispose { onHoverChange(false) } }

    Popup(
        popupPositionProvider = provider,
        properties = PopupProperties(focusable = false, clippingEnabled = false),
    ) {
        val colors = MdsTheme.colors
        Row(
            modifier = Modifier
                .graphicsLayer { alpha = appearance.value }
                .background(colors.surfaceRaised, MdsTheme.shapes.medium)
                .border(MdsTheme.sizes.borderWidth, colors.border, MdsTheme.shapes.medium)
                .hoverable(interactionSource)
                .padding(MdsTheme.spacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MdsTheme.spacing.xxs),
        ) {
            when (state.kind) {
                FormatPopupKind.Inline -> InlineFormatButtons(state = state, onAction = onAction)
                FormatPopupKind.LineMarker -> LineMarkerButtons(state = state, onAction = onAction)
            }
        }
    }
}

@Composable
private fun InlineFormatButtons(state: FormatPopupState, onAction: (FormatAction) -> Unit) {
    val typography = MdsTheme.typography
    FormatButton(
        selected = InlineFormat.Bold in state.formats,
        onClick = { onAction(FormatAction.ToggleInline(state.range, InlineFormat.Bold)) },
    ) { color ->
        MdsText(
            text = "B",
            style = typography.uiLabel.copy(fontWeight = FontWeight.Bold),
            color = color,
            maxLines = 1,
        )
    }
    FormatButton(
        selected = InlineFormat.Italic in state.formats,
        onClick = { onAction(FormatAction.ToggleInline(state.range, InlineFormat.Italic)) },
    ) { color ->
        MdsText(
            text = "I",
            style = typography.uiLabel.copy(fontStyle = FontStyle.Italic),
            color = color,
            maxLines = 1,
        )
    }
    FormatButton(
        selected = InlineFormat.Strikethrough in state.formats,
        onClick = { onAction(FormatAction.ToggleInline(state.range, InlineFormat.Strikethrough)) },
    ) { color ->
        MdsText(
            text = "S",
            style = typography.uiLabel.copy(textDecoration = TextDecoration.LineThrough),
            color = color,
            maxLines = 1,
        )
    }
    FormatButton(
        selected = InlineFormat.Code in state.formats,
        onClick = { onAction(FormatAction.ToggleInline(state.range, InlineFormat.Code)) },
    ) { color ->
        MdsIcon(
            icon = MdsIcons.Code,
            contentDescription = "Inline code",
            tint = color,
            size = MdsTheme.sizes.iconSmall,
        )
    }

    PopupSeparator()

    (1..3).forEach { level ->
        val active = state.headingLevel == level
        FormatButton(
            selected = active,
            onClick = {
                onAction(FormatAction.SetHeading(state.range.start, if (active) 0 else level))
            },
        ) { color ->
            MdsText(
                text = "H$level",
                style = typography.uiSmall.copy(fontWeight = FontWeight.SemiBold),
                color = color,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun LineMarkerButtons(state: FormatPopupState, onAction: (FormatAction) -> Unit) {
    val typography = MdsTheme.typography
    FormatButton(
        selected = state.lineMarker == LineMarkerKind.Task,
        onClick = {
            onAction(FormatAction.SetLineMarker(state.range.start, LineMarkerKind.Task))
        },
    ) { color ->
        MdsIcon(
            icon = MdsIcons.Check,
            contentDescription = "Checkbox",
            tint = color,
            size = MdsTheme.sizes.iconSmall,
        )
    }
    FormatButton(
        selected = state.lineMarker == LineMarkerKind.Bullet,
        onClick = {
            onAction(FormatAction.SetLineMarker(state.range.start, LineMarkerKind.Bullet))
        },
    ) { color ->
        MdsText(
            text = "•",
            style = typography.uiLabel.copy(fontWeight = FontWeight.Bold),
            color = color,
            maxLines = 1,
        )
    }
    FormatButton(
        selected = state.lineMarker == LineMarkerKind.Ordered,
        onClick = {
            onAction(FormatAction.SetLineMarker(state.range.start, LineMarkerKind.Ordered))
        },
    ) { color ->
        MdsIcon(
            icon = MdsIcons.ListOrdered,
            contentDescription = "Numbered list",
            tint = color,
            size = MdsTheme.sizes.iconSmall,
        )
    }
    FormatButton(
        selected = state.lineMarker == LineMarkerKind.None,
        onClick = {
            onAction(FormatAction.SetLineMarker(state.range.start, LineMarkerKind.None))
        },
    ) { color ->
        MdsText(
            text = "—",
            style = typography.uiLabel,
            color = color,
            maxLines = 1,
        )
    }
}

@Composable
private fun PopupSeparator() {
    Box(
        modifier = Modifier
            .width(MdsTheme.sizes.borderWidth)
            .height(MdsTheme.sizes.iconSmall)
            .background(MdsTheme.colors.border),
    )
}

@Composable
private fun FormatButton(
    selected: Boolean,
    onClick: () -> Unit,
    content: @Composable (Color) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val currentClick = rememberUpdatedState(onClick)
    val colors = MdsTheme.colors
    val background = when {
        selected -> colors.surfaceSelected
        hovered -> colors.surfaceHover
        else -> Color.Transparent
    }
    val contentColor = if (selected) colors.textPrimary else colors.textSecondary
    Box(
        modifier = Modifier
            .size(MdsTheme.sizes.iconButtonSmall)
            .background(background, MdsTheme.shapes.small)
            .hoverable(interactionSource)
            .pointerInput(Unit) {
                detectTapGestures { currentClick.value() }
            },
        contentAlignment = Alignment.Center,
    ) {
        content(contentColor)
    }
}

private class AnchorPositionProvider(
    private val anchor: Rect,
    private val gap: Int,
) : PopupPositionProvider {

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val centerX = anchorBounds.left + anchor.center.x.roundToInt()
        val x = (centerX - popupContentSize.width / 2)
            .coerceIn(0, maxOf(0, windowSize.width - popupContentSize.width))
        val above = anchorBounds.top + anchor.top.roundToInt() - popupContentSize.height - gap
        val below = anchorBounds.top + anchor.bottom.roundToInt() + gap
        val y = if (above >= 0) {
            above
        } else {
            below.coerceAtMost(maxOf(0, windowSize.height - popupContentSize.height))
        }
        return IntOffset(x, y)
    }
}
