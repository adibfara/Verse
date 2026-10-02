package com.github.adibfara.mds.feature.editor.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.adibfara.mds.core.markdown.MdBlock
import com.github.adibfara.mds.core.markdown.MdDocument
import com.github.adibfara.mds.core.markdown.MdRange
import com.github.adibfara.mds.feature.editor.ui.preview.MarkdownBlock

@Composable
fun MarkdownPreview(
    document: MdDocument,
    listState: LazyListState,
    onToggleTask: (MdRange, Boolean) -> Unit,
    onOpenImage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier, state = listState) {
        items(
            items = document.blocks,
            key = { block: MdBlock -> block.range.start },
        ) { block ->
            MarkdownBlock(
                block = block,
                onToggleTask = onToggleTask,
                onOpenImage = onOpenImage,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
