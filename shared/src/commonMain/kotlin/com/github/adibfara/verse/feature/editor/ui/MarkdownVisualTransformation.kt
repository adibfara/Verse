package com.github.adibfara.verse.feature.editor.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.github.adibfara.verse.core.markdown.MarkerMask
import com.github.adibfara.verse.core.markdown.MaskedDocument

class MarkdownVisualTransformation(
    private val masked: MaskedDocument,
    private val styles: MarkdownStyles,
) : VisualTransformation {

    private val transformed: TransformedText by lazy { build() }

    override fun filter(text: AnnotatedString): TransformedText {
        if (text.text != masked.mask.originalText) {
            return TransformedText(AnnotatedString(text.text), OffsetMapping.Identity)
        }
        return transformed
    }

    private fun build(): TransformedText {
        val mask = masked.mask
        val annotated = buildAnnotatedString {
            append(mask.transformedText)
            masked.styles.forEach { styled ->
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
