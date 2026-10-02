package com.github.adibfara.mds.feature.editor.ui

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.TextFieldValue
import com.github.adibfara.mds.core.designsystem.theme.MdsTheme
import com.github.adibfara.mds.core.markdown.MdDocument
import com.github.adibfara.mds.core.markdown.lineRangeAt

@Composable
fun LiveMarkdownField(
    value: TextFieldValue,
    document: MdDocument,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
) {
    val styles = rememberMarkdownStyles()
    val colors = MdsTheme.colors
    val revealedLine = remember(value.text, value.selection.start) {
        lineRangeAt(value.text, value.selection.start)
    }
    val transformation = remember(document, revealedLine, styles) {
        MarkdownVisualTransformation(
            document = document,
            revealedLine = revealedLine,
            styles = styles,
        )
    }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = MdsTheme.typography.editorBody.copy(color = colors.textPrimary),
        cursorBrush = SolidColor(colors.caret),
        visualTransformation = transformation,
    )
}

@Composable
fun RawMarkdownField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MdsTheme.colors
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = MdsTheme.typography.editorRaw.copy(color = colors.textPrimary),
        cursorBrush = SolidColor(colors.caret),
    )
}
