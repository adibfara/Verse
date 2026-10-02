package com.github.adibfara.mds.feature.editor.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.github.adibfara.mds.core.markdown.MarkerMask
import com.github.adibfara.mds.core.markdown.MdDocument
import com.github.adibfara.mds.core.markdown.MdRange
import com.github.adibfara.mds.core.markdown.MaskEdit
import com.github.adibfara.mds.core.markdown.buildMaskedText

class MarkdownVisualTransformation(
    private val document: MdDocument,
    private val revealedLine: MdRange?,
    private val styles: MarkdownStyles,
) : VisualTransformation {

    private var cachedSource: String? = null
    private var cachedResult: TransformedText? = null

    override fun filter(text: AnnotatedString): TransformedText {
        cachedResult?.takeIf { cachedSource == text.text }?.let { return it }

        val result = if (document.text != text.text) {
            TransformedText(AnnotatedString(text.text), OffsetMapping.Identity)
        } else {
            transform(text.text)
        }
        cachedSource = text.text
        cachedResult = result
        return result
    }

    private fun transform(source: String): TransformedText {
        val edits = document.markers.map { MaskEdit(it, "") } + document.decorations
        val mask = buildMaskedText(source, edits, revealedLine)
        val transformed = mask.transformedText

        val annotated = buildAnnotatedString {
            append(transformed)
            document.styles.forEach { styled ->
                val start = mask.originalToTransformed(styled.range.start)
                val end = mask.originalToTransformed(styled.range.end)
                if (end > start) {
                    addStyle(styles.spanFor(styled.kind), start, end)
                }
            }
        }

        return TransformedText(annotated, MaskOffsetMapping(mask))
    }
}

private class MaskOffsetMapping(private val mask: MarkerMask) : OffsetMapping {

    override fun originalToTransformed(offset: Int): Int = mask.originalToTransformed(offset)

    override fun transformedToOriginal(offset: Int): Int = mask.transformedToOriginal(offset)
}
