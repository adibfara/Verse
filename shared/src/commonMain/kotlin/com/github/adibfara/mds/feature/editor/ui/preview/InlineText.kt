package com.github.adibfara.mds.feature.editor.ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import com.github.adibfara.mds.core.designsystem.component.MdsText
import com.github.adibfara.mds.core.designsystem.theme.MdsColors
import com.github.adibfara.mds.core.designsystem.theme.MdsTheme
import com.github.adibfara.mds.core.markdown.InlineStyle
import com.github.adibfara.mds.core.markdown.MdInline

fun buildInlineText(inlines: List<MdInline>, colors: MdsColors): AnnotatedString =
    buildAnnotatedString { appendInlines(inlines, colors) }

private fun androidx.compose.ui.text.AnnotatedString.Builder.appendInlines(
    inlines: List<MdInline>,
    colors: MdsColors,
) {
    inlines.forEach { inline ->
        when (inline) {
            is MdInline.Text -> append(inline.text)

            is MdInline.LineBreak -> append("\n")

            is MdInline.Code -> withStyle(
                SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    color = colors.codeText,
                    background = colors.codeBackground,
                ),
            ) { append(inline.code) }

            is MdInline.Styled -> withStyle(spanFor(inline.style, colors)) {
                appendInlines(inline.children, colors)
            }

            is MdInline.Link -> withLink(LinkAnnotation.Url(inline.target)) {
                withStyle(
                    SpanStyle(
                        color = colors.link,
                        textDecoration = TextDecoration.Underline,
                    ),
                ) { appendInlines(inline.children, colors) }
            }

            is MdInline.Image -> Unit
        }
    }
}

private fun spanFor(style: InlineStyle, colors: MdsColors): SpanStyle = when (style) {
    InlineStyle.Bold -> SpanStyle(fontWeight = FontWeight.Bold, color = colors.bold)
    InlineStyle.Italic -> SpanStyle(fontStyle = FontStyle.Italic, color = colors.italic)
    InlineStyle.Strikethrough -> SpanStyle(
        textDecoration = TextDecoration.LineThrough,
        color = colors.textSecondary,
    )
}

fun imagesIn(inlines: List<MdInline>): List<MdInline.Image> = inlines.flatMap { inline ->
    when (inline) {
        is MdInline.Image -> listOf(inline)
        is MdInline.Styled -> imagesIn(inline.children)
        is MdInline.Link -> imagesIn(inline.children)
        else -> emptyList()
    }
}

@Composable
fun InlineText(
    inlines: List<MdInline>,
    modifier: Modifier = Modifier,
    style: TextStyle = MdsTheme.typography.editorBody,
) {
    val colors = MdsTheme.colors
    val text = buildInlineText(inlines, colors)
    if (text.isEmpty()) return
    MdsText(text = text, modifier = modifier, style = style, color = colors.textPrimary)
}
