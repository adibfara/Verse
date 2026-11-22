package com.github.adibfara.verse.feature.editor.ui

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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
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
import com.github.adibfara.verse.core.designsystem.component.VerseIcon
import com.github.adibfara.verse.core.designsystem.component.VerseIcons
import com.github.adibfara.verse.core.designsystem.component.VerseText
import com.github.adibfara.verse.core.designsystem.theme.VerseTheme
import com.github.adibfara.verse.core.markdown.FormatAction
import com.github.adibfara.verse.core.markdown.InlineFormat
import com.github.adibfara.verse.core.markdown.LineMarkerKind
import com.github.adibfara.verse.core.markdown.MdRange
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
    val sourceText: String = "",
    val isLink: Boolean = false,
)

@Composable
internal fun FormatPopup(
    state: FormatPopupState,
    onAction: (FormatAction) -> Unit,
) {
    val gap = with(LocalDensity.current) { VerseTheme.spacing.xs.roundToPx() }
    val provider = remember(state.anchor, gap) { AnchorPositionProvider(state.anchor, gap) }
    val interactionSource = remember { MutableInteractionSource() }
    val appearance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        appearance.animateTo(1f, tween(durationMillis = PopupFadeMillis))
    }

    Popup(
        popupPositionProvider = provider,
        properties = PopupProperties(focusable = false, clippingEnabled = false),
    ) {
        val colors = VerseTheme.colors
        Row(
            modifier = Modifier
                .graphicsLayer { alpha = appearance.value }
                .background(colors.surfaceRaised, VerseTheme.shapes.medium)
                .border(VerseTheme.sizes.borderWidth, colors.border, VerseTheme.shapes.medium)
                .hoverable(interactionSource)
                .padding(VerseTheme.spacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(VerseTheme.spacing.xxs),
        ) {
            when (state.kind) {
                FormatPopupKind.Inline -> InlineFormatButtons(state = state, onAction = onAction)
                FormatPopupKind.LineMarker -> LineMarkerButtons(state = state, onAction = onAction)
            }
        }
    }
}

@Composable
internal fun InlineFormatButtons(state: FormatPopupState, onAction: (FormatAction) -> Unit) {
    val typography = VerseTheme.typography
    val clipboard = LocalClipboardManager.current
    FormatButton(
        selected = InlineFormat.Bold in state.formats,
        onClick = { onAction(FormatAction.ToggleInline(state.range, InlineFormat.Bold)) },
    ) { color ->
        VerseText(
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
        VerseText(
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
        VerseText(
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
        VerseIcon(
            icon = VerseIcons.Code,
            contentDescription = "Inline code",
            tint = color,
            size = VerseTheme.sizes.iconSmall,
        )
    }
    FormatButton(
        selected = state.isLink,
        onClick = { onAction(FormatAction.ToggleLink(state.range)) },
    ) { color ->
        VerseIcon(
            icon = VerseIcons.Link,
            contentDescription = "Link",
            tint = color,
            size = VerseTheme.sizes.iconSmall,
        )
    }

    PopupSeparator()

    FormatButton(
        selected = false,
        onClick = { clipboard.setText(AnnotatedString(state.sourceText)) },
    ) { color ->
        VerseIcon(
            icon = VerseIcons.Copy,
            contentDescription = "Copy",
            tint = color,
            size = VerseTheme.sizes.iconSmall,
        )
    }

    PopupSeparator()

    (1..3).forEach { level ->
        FormatButton(
            selected = state.headingLevel == level,
            onClick = { onAction(FormatAction.SetHeading(state.range.start, level)) },
        ) { color ->
            VerseText(
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
    val typography = VerseTheme.typography
    FormatButton(
        selected = state.lineMarker == LineMarkerKind.Bullet,
        onClick = {
            onAction(FormatAction.SetLineMarker(LineMarkerKind.Bullet))
        },
    ) { color ->
        VerseIcon(
            icon = VerseIcons.Bullet,
            contentDescription = "Bulleted list",
            tint = color,
            size = VerseTheme.sizes.iconSmall,
        )
    }
    FormatButton(
        selected = state.lineMarker == LineMarkerKind.Task,
        onClick = {
            onAction(FormatAction.SetLineMarker(LineMarkerKind.Task))
        },
    ) { color ->
        VerseIcon(
            icon = VerseIcons.CheckSquare,
            contentDescription = "Checkbox",
            tint = color,
            size = VerseTheme.sizes.iconSmall,
        )
    }
    FormatButton(
        selected = state.lineMarker == LineMarkerKind.Ordered,
        onClick = {
            onAction(FormatAction.SetLineMarker(LineMarkerKind.Ordered))
        },
    ) { color ->
        VerseIcon(
            icon = VerseIcons.Hash,
            contentDescription = "Numbered list",
            tint = color,
            size = VerseTheme.sizes.iconSmall,
        )
    }
    FormatButton(
        selected = state.lineMarker == LineMarkerKind.None,
        onClick = {
            onAction(FormatAction.SetLineMarker(LineMarkerKind.None))
        },
    ) { color ->
        VerseText(
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
            .width(VerseTheme.sizes.borderWidth)
            .height(VerseTheme.sizes.iconSmall)
            .background(VerseTheme.colors.border),
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
    val colors = VerseTheme.colors
    val background = if (hovered) colors.surfaceHover else Color.Transparent
    val contentColor = if (selected) colors.accent else colors.textSecondary
    Box(
        modifier = Modifier
            .size(VerseTheme.sizes.iconButtonSmall)
            .background(background, VerseTheme.shapes.small)
            .hoverable(interactionSource)
            .pointerInput(Unit) {
                detectTapGestures { currentClick.value() }
            },
        contentAlignment = Alignment.Center,
    ) {
        content(contentColor)
    }
}

internal class AnchorPositionProvider(
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
