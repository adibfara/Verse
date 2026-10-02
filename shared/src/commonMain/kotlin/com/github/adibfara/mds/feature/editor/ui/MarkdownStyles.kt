package com.github.adibfara.mds.feature.editor.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.github.adibfara.mds.core.designsystem.theme.MdsColors
import com.github.adibfara.mds.core.designsystem.theme.MdsTheme
import com.github.adibfara.mds.core.designsystem.theme.MdsTypography
import com.github.adibfara.mds.core.markdown.StyleKind

@Immutable
class MarkdownStyles(
    private val colors: MdsColors,
    private val typography: MdsTypography,
) {

    fun spanFor(kind: StyleKind): SpanStyle = when (kind) {
        StyleKind.Heading1 -> headingSpan(1)
        StyleKind.Heading2 -> headingSpan(2)
        StyleKind.Heading3 -> headingSpan(3)
        StyleKind.Heading4 -> headingSpan(4)
        StyleKind.Heading5 -> headingSpan(5)
        StyleKind.Heading6 -> headingSpan(6)
        StyleKind.Bold -> SpanStyle(fontWeight = FontWeight.Bold, color = colors.bold)
        StyleKind.Italic -> SpanStyle(fontStyle = FontStyle.Italic, color = colors.italic)
        StyleKind.Strikethrough -> SpanStyle(
            textDecoration = TextDecoration.LineThrough,
            color = colors.textSecondary,
        )

        StyleKind.InlineCode -> SpanStyle(
            fontFamily = FontFamily.Monospace,
            color = colors.codeText,
            background = colors.codeBackground,
        )

        StyleKind.CodeBlock -> SpanStyle(
            fontFamily = FontFamily.Monospace,
            color = colors.codeText,
        )

        StyleKind.Link -> SpanStyle(
            color = colors.link,
            textDecoration = TextDecoration.Underline,
        )

        StyleKind.Quote -> SpanStyle(
            color = colors.textSecondary,
            fontStyle = FontStyle.Italic,
        )

        StyleKind.ListMarker -> SpanStyle(color = colors.listMarker)
        StyleKind.TaskMarker -> SpanStyle(
            fontFamily = FontFamily.Monospace,
            color = colors.accent,
        )
    }

    private fun headingSpan(level: Int): SpanStyle {
        val style = typography.headingFor(level)
        return SpanStyle(
            fontSize = style.fontSize,
            fontWeight = style.fontWeight,
            color = colors.textPrimary,
        )
    }
}

@Composable
fun rememberMarkdownStyles(): MarkdownStyles {
    val colors = MdsTheme.colors
    val typography = MdsTheme.typography
    return remember(colors, typography) { MarkdownStyles(colors, typography) }
}
