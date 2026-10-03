package com.github.adibfara.mds.core.designsystem.component

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
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
import com.github.adibfara.mds.core.designsystem.theme.MdsTheme

@Composable
fun MdsTextField(
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
    val colors = MdsTheme.colors
    var selection by remember {
        mutableStateOf(if (selectAllOnMount) TextRange(0, value.length) else TextRange(value.length))
    }
    BasicTextField(
        value = TextFieldValue(text = value, selection = selection),
        onValueChange = { fieldValue ->
            selection = fieldValue.selection
            onValueChange(fieldValue.text)
        },
        modifier = modifier
            .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
            .background(colors.surfaceRaised, MdsTheme.shapes.small)
            .border(
                width = MdsTheme.sizes.borderWidth,
                color = if (focused) colors.accent else colors.border,
                shape = MdsTheme.shapes.small,
            )
            .padding(horizontal = MdsTheme.spacing.sm, vertical = MdsTheme.spacing.sm),
        textStyle = MdsTheme.typography.uiBody.copy(color = colors.textPrimary),
        singleLine = singleLine,
        cursorBrush = SolidColor(colors.caret),
        interactionSource = interactionSource,
        keyboardOptions = KeyboardOptions(imeAction = if (onSubmit != null) ImeAction.Done else ImeAction.Default),
        keyboardActions = KeyboardActions(onDone = { onSubmit?.invoke() }),
        decorationBox = { innerTextField ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        MdsText(
                            text = placeholder,
                            style = MdsTheme.typography.uiBody,
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
fun MdsButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true,
    danger: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val colors = MdsTheme.colors
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
            .background(background, MdsTheme.shapes.small)
            .border(
                width = if (primary) 0.dp else MdsTheme.sizes.borderWidth,
                color = if (primary) Color.Transparent else colors.border,
                shape = MdsTheme.shapes.small,
            )
            .hoverable(interactionSource, enabled)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = MdsTheme.spacing.lg, vertical = MdsTheme.spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        MdsText(text = label, style = MdsTheme.typography.uiLabel, color = contentColor, maxLines = 1)
    }
}

@Composable
fun MdsDialog(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val colors = MdsTheme.colors
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = modifier
                .widthIn(min = 380.dp, max = 560.dp)
                .background(colors.surfaceRaised, MdsTheme.shapes.large)
                .border(MdsTheme.sizes.borderWidth, colors.border, MdsTheme.shapes.large)
                .padding(MdsTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(MdsTheme.spacing.lg),
        ) {
            MdsText(text = title, style = MdsTheme.typography.uiTitle, color = colors.textPrimary)
            content()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MdsTheme.spacing.sm, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                actions()
            }
        }
    }
}

@Composable
fun MdsSplitter(
    onDelta: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val colors = MdsTheme.colors
    Box(
        modifier = modifier
            .width(MdsTheme.sizes.splitterWidth)
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
                .width(MdsTheme.sizes.borderWidth)
                .fillMaxHeight()
                .background(if (hovered) colors.accent else colors.border),
        )
    }
}
