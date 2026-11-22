package com.github.adibfara.verse.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.adibfara.verse.core.designsystem.theme.VerseTheme

@Composable
fun VerseTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    focusRequester: FocusRequester? = null,
    onSubmit: (() -> Unit)? = null,
    selectAllOnMount: Boolean = false,
    trailing: @Composable (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val colors = VerseTheme.colors
    var fieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = value,
                selection = if (selectAllOnMount) TextRange(0, value.length) else TextRange(value.length),
            ),
        )
    }
    var lastExternalValue by remember { mutableStateOf(value) }
    if (lastExternalValue != value) {
        lastExternalValue = value
        if (value != fieldValue.text) {
            fieldValue = TextFieldValue(text = value, selection = TextRange(value.length))
        }
    }
    val placeholderVisible = fieldValue.text.isEmpty()
    BasicTextField(
        value = fieldValue,
        onValueChange = { incomingValue ->
            fieldValue = incomingValue
            onValueChange(incomingValue.text)
        },
        modifier = modifier
            .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
            .background(colors.surfaceRaised, VerseTheme.shapes.small)
            .border(
                width = VerseTheme.sizes.borderWidth,
                color = if (focused) colors.accent else colors.border,
                shape = VerseTheme.shapes.small,
            )
            .padding(horizontal = VerseTheme.spacing.sm, vertical = VerseTheme.spacing.sm),
        textStyle = VerseTheme.typography.uiBody.copy(color = colors.textPrimary),
        singleLine = singleLine,
        cursorBrush = SolidColor(colors.caret),
        interactionSource = interactionSource,
        keyboardOptions = KeyboardOptions(imeAction = if (onSubmit != null) ImeAction.Done else ImeAction.Default),
        keyboardActions = KeyboardActions(onDone = { onSubmit?.invoke() }),
        decorationBox = { innerTextField ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    if (placeholderVisible) {
                        VerseText(
                            text = placeholder,
                            style = VerseTheme.typography.uiBody,
                            color = colors.textMuted,
                            maxLines = 1,
                        )
                    }
                    innerTextField()
                }
                trailing?.invoke()
            }
        },
    )
}

@Composable
fun VerseStepper(
    value: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
    decreaseEnabled: Boolean = true,
    increaseEnabled: Boolean = true,
    decreaseDescription: String = "Decrease",
    increaseDescription: String = "Increase",
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(VerseTheme.spacing.xs),
    ) {
        VerseIconButton(
            icon = VerseIcons.Minus,
            contentDescription = decreaseDescription,
            onClick = onDecrease,
            enabled = decreaseEnabled,
        )
        VerseText(
            text = value,
            modifier = Modifier.widthIn(min = VerseTheme.sizes.stepperValueWidth),
            style = VerseTheme.typography.uiLabel,
            color = VerseTheme.colors.textPrimary,
            maxLines = 1,
        )
        VerseIconButton(
            icon = VerseIcons.Plus,
            contentDescription = increaseDescription,
            onClick = onIncrease,
            enabled = increaseEnabled,
        )
    }
}

@Composable
fun VerseButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true,
    danger: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val colors = VerseTheme.colors
    val background = when {
        !enabled -> colors.surfaceHover
        danger && primary -> colors.danger
        primary -> colors.accent
        hovered -> colors.surfaceHover
        else -> colors.surfaceRaised
    }
    val contentColor = when {
        !enabled -> colors.textMuted
        primary -> colors.textOnAccent
        danger -> colors.danger
        else -> colors.textPrimary
    }
    Box(
        modifier = modifier
            .background(background, VerseTheme.shapes.small)
            .border(
                width = if (primary) 0.dp else VerseTheme.sizes.borderWidth,
                color = if (primary) Color.Transparent else colors.border,
                shape = VerseTheme.shapes.small,
            )
            .hoverable(interactionSource, enabled)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = VerseTheme.spacing.lg, vertical = VerseTheme.spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        VerseText(text = label, style = VerseTheme.typography.uiLabel, color = contentColor, maxLines = 1)
    }
}

@Composable
fun VerseDialog(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val colors = VerseTheme.colors
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = modifier
                .safeDrawingPadding()
                .padding(VerseTheme.spacing.lg)
                .widthIn(min = VerseTheme.sizes.dialogMinWidth, max = VerseTheme.sizes.dialogMaxWidth)
                .background(colors.surfaceRaised, VerseTheme.shapes.large)
                .border(VerseTheme.sizes.borderWidth, colors.border, VerseTheme.shapes.large)
                .padding(VerseTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(VerseTheme.spacing.lg),
        ) {
            VerseText(text = title, style = VerseTheme.typography.uiTitle, color = colors.textPrimary)
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
            ) {
                content()
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(VerseTheme.spacing.sm, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                actions()
            }
        }
    }
}

@Composable
fun VerseSplitter(
    onDelta: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val colors = VerseTheme.colors
    Box(
        modifier = modifier
            .width(VerseTheme.sizes.splitterWidth)
            .fillMaxHeight()
            .hoverable(interactionSource)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDelta(dragAmount.x)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(VerseTheme.sizes.borderWidth)
                .fillMaxHeight()
                .background(if (hovered) colors.accent else colors.border),
        )
    }
}
