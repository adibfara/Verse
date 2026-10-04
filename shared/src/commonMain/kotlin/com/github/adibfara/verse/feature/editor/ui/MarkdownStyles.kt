package com.github.adibfara.verse.feature.editor.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.github.adibfara.verse.core.designsystem.theme.VerseColors
import com.github.adibfara.verse.core.designsystem.theme.VerseTheme
import com.github.adibfara.verse.core.designsystem.theme.VerseTypography
import com.github.adibfara.verse.core.markdown.StyleKind

@Immutable
class MarkdownStyles(
    private val colors: VerseColors,
    private val typography: VerseTypography,
) {

    private val spans: List<SpanStyle> = StyleKind.entries.map(::buildSpan)

    fun spanFor(kind: StyleKind): SpanStyle = spans[kind.ordinal]

    private fun buildSpan(kind: StyleKind): SpanStyle = when (kind) {
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
        StyleKind.TaskMarker -> SpanStyle(color = colors.textSecondary)
        StyleKind.TaskMarkerDone -> SpanStyle(color = colors.accent)
        StyleKind.TaskText -> SpanStyle(
            color = colors.textMuted,
            textDecoration = TextDecoration.LineThrough,
        )

        StyleKind.Table -> SpanStyle(
            fontFamily = FontFamily.Monospace,
            color = colors.textPrimary,
        )

        StyleKind.TableHeader -> SpanStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
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
    val colors = VerseTheme.colors
    val typography = VerseTheme.typography
    return remember(colors, typography) { MarkdownStyles(colors, typography) }
}
