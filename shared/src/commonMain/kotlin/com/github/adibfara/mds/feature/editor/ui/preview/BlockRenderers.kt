package com.github.adibfara.mds.feature.editor.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.adibfara.mds.core.designsystem.component.MdsDivider
import com.github.adibfara.mds.core.designsystem.theme.MdsTheme
import com.github.adibfara.mds.core.markdown.MdBlock
import com.github.adibfara.mds.core.markdown.MdInline
import com.github.adibfara.mds.core.markdown.MdRange

@Composable
fun MarkdownBlock(
    block: MdBlock,
    onToggleTask: (MdRange, Boolean) -> Unit,
    onOpenImage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (block) {
        is MdBlock.Heading -> HeadingBlock(block, modifier)
        is MdBlock.Paragraph -> ParagraphBlock(block, onOpenImage, modifier)
        is MdBlock.ListBlock -> ListBlockRenderer(block, onToggleTask, onOpenImage, modifier)
        is MdBlock.CodeBlock -> CodeBlockRenderer(block, modifier)
        is MdBlock.Quote -> QuoteBlock(block, onToggleTask, onOpenImage, modifier)
        is MdBlock.Table -> TableRenderer(block, modifier)
        is MdBlock.HorizontalRule -> MdsDivider(
            modifier = modifier.padding(vertical = MdsTheme.spacing.lg),
        )
    }
}

@Composable
private fun HeadingBlock(block: MdBlock.Heading, modifier: Modifier) {
    InlineText(
        inlines = block.content,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = MdsTheme.spacing.lg, bottom = MdsTheme.spacing.xs),
        style = MdsTheme.typography.headingFor(block.level),
    )
}

@Composable
private fun ParagraphBlock(
    block: MdBlock.Paragraph,
    onOpenImage: (String) -> Unit,
    modifier: Modifier,
) {
    val images = imagesIn(block.content)
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = MdsTheme.spacing.xs),
        verticalArrangement = Arrangement.spacedBy(MdsTheme.spacing.sm),
    ) {
        InlineText(inlines = block.content, modifier = Modifier.fillMaxWidth())
        images.forEach { image ->
            MarkdownImage(target = image.target, alt = image.alt, onOpenImage = onOpenImage)
        }
    }
}

@Composable
private fun QuoteBlock(
    block: MdBlock.Quote,
    onToggleTask: (MdRange, Boolean) -> Unit,
    onOpenImage: (String) -> Unit,
    modifier: Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(vertical = MdsTheme.spacing.xs),
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(MdsTheme.colors.quoteBar, MdsTheme.shapes.small),
        )
        Column(modifier = Modifier.padding(start = MdsTheme.spacing.md)) {
            block.children.forEach { child ->
                MarkdownBlock(
                    block = child,
                    onToggleTask = onToggleTask,
                    onOpenImage = onOpenImage,
                )
            }
        }
    }
}

@Composable
private fun TableRenderer(block: MdBlock.Table, modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = MdsTheme.spacing.sm)
            .horizontalScroll(rememberScrollState()),
    ) {
        TableRow(cells = block.header, isHeader = true)
        MdsDivider()
        block.rows.forEach { row ->
            TableRow(cells = row, isHeader = false)
            MdsDivider()
        }
    }
}

@Composable
private fun TableRow(cells: List<List<MdInline>>, isHeader: Boolean) {
    val style = if (isHeader) {
        MdsTheme.typography.editorBody.copy(fontWeight = FontWeight.SemiBold)
    } else {
        MdsTheme.typography.editorBody
    }
    Row(
        modifier = Modifier.padding(vertical = MdsTheme.spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(MdsTheme.spacing.lg),
    ) {
        cells.forEach { cell ->
            InlineText(inlines = cell, modifier = Modifier.width(180.dp), style = style)
        }
    }
}
