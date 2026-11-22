package com.github.adibfara.verse.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.github.adibfara.verse.core.designsystem.theme.VerseTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val MorphMillis = 220
private const val FadeMillis = 160

@Composable
fun VerseMorphMenu(
    entries: List<MenuEntry>,
    triggerIcon: ImageVector,
    onDismiss: () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { visible = true }
    val dismiss: () -> Unit = {
        if (visible) {
            visible = false
            scope.launch {
                delay(MorphMillis.toLong())
                onDismiss()
            }
        }
    }
    val colors = VerseTheme.colors
    val shape = VerseTheme.shapes.medium
    val width by animateDpAsState(
        targetValue = if (visible) VerseTheme.sizes.menuPanelWidth else VerseTheme.sizes.iconButton,
        animationSpec = tween(durationMillis = MorphMillis, easing = FastOutSlowInEasing),
    )
    val borderColor by animateColorAsState(
        targetValue = if (visible) colors.border else Color.Transparent,
        animationSpec = tween(durationMillis = MorphMillis),
    )

    Popup(
        popupPositionProvider = remember { TopEndPositionProvider() },
        onDismissRequest = dismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Column(
            modifier = Modifier
                .width(width)
                .clip(shape)
                .background(colors.surfaceRaised, shape)
                .border(VerseTheme.sizes.borderWidth, borderColor, shape),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.End)
                    .size(VerseTheme.sizes.iconButton)
                    .pointerInput(Unit) { detectTapGestures(onTap = { dismiss() }) },
                contentAlignment = Alignment.Center,
            ) {
                VerseIcon(
                    icon = triggerIcon,
                    contentDescription = "Close menu",
                    tint = colors.accent,
                )
            }
            AnimatedVisibility(
                visible = visible,
                enter = expandVertically(
                    animationSpec = tween(durationMillis = MorphMillis, easing = FastOutSlowInEasing),
                    expandFrom = Alignment.Top,
                ) + fadeIn(tween(durationMillis = FadeMillis, delayMillis = FadeMillis / 2)),
                exit = shrinkVertically(
                    animationSpec = tween(durationMillis = MorphMillis, easing = FastOutSlowInEasing),
                    shrinkTowards = Alignment.Top,
                ) + fadeOut(tween(durationMillis = FadeMillis)),
            ) {
                Column(modifier = Modifier.padding(bottom = VerseTheme.spacing.xs)) {
                    entries.forEach { entry ->
                        when (entry) {
                            is MenuEntry.Separator -> MenuSeparator()
                            is MenuEntry.Action -> MenuActionRow(entry = entry, onDismiss = dismiss)
                            is MenuEntry.Stepper -> MenuStepperRow(entry = entry)
                        }
                    }
                }
            }
        }
    }
}

private class TopEndPositionProvider : PopupPositionProvider {

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x = (anchorBounds.right - popupContentSize.width).coerceAtLeast(0)
        val y = anchorBounds.top.coerceAtMost(maxOf(0, windowSize.height - popupContentSize.height))
        return IntOffset(x, y)
    }
}
