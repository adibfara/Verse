package com.github.adibfara.mds.feature.editor.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.adibfara.mds.core.designsystem.component.MdsIcon
import com.github.adibfara.mds.core.designsystem.component.MdsIcons
import com.github.adibfara.mds.core.designsystem.component.MdsText
import com.github.adibfara.mds.core.designsystem.theme.MdsTheme
import com.github.adibfara.mds.core.markdown.MdBlock
import com.github.adibfara.mds.core.markdown.MdListItem
import com.github.adibfara.mds.core.markdown.MdRange

@Composable
fun ListBlockRenderer(
    block: MdBlock.ListBlock,
    onToggleTask: (MdRange, Boolean) -> Unit,
    onOpenImage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(vertical = MdsTheme.spacing.xxs),
    ) {
        Spacer(modifier = Modifier.width(MdsTheme.spacing.sm))
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(MdsTheme.colors.indentGuide),
        )
        Column(modifier = Modifier.padding(start = MdsTheme.spacing.md)) {
            block.items.forEachIndexed { index, item ->
                ListItemRenderer(
                    item = item,
                    ordinal = index + 1,
                    ordered = block.ordered,
                    onToggleTask = onToggleTask,
                    onOpenImage = onOpenImage,
                )
            }
        }
    }
}

@Composable
private fun ListItemRenderer(
    item: MdListItem,
    ordinal: Int,
    ordered: Boolean,
    onToggleTask: (MdRange, Boolean) -> Unit,
    onOpenImage: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = MdsTheme.spacing.xxs),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier.width(22.dp).padding(top = 3.dp),
            contentAlignment = Alignment.TopStart,
        ) {
            when {
                item.checked != null && item.checkBoxRange != null -> TaskCheckbox(
                    checked = item.checked,
                    onToggle = { checked -> onToggleTask(item.checkBoxRange, checked) },
                )

                ordered -> MdsText(
                    text = "$ordinal.",
                    style = MdsTheme.typography.editorBody,
                    color = MdsTheme.colors.listMarker,
                )

                else -> MdsText(
                    text = "•",
                    style = MdsTheme.typography.editorBody,
                    color = MdsTheme.colors.listMarker,
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            item.content.forEach { child ->
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
private fun TaskCheckbox(checked: Boolean, onToggle: (Boolean) -> Unit) {
    val colors = MdsTheme.colors
    Box(
        modifier = Modifier
            .size(16.dp)
            .background(
                if (checked) colors.accent else colors.surfaceRaised,
                MdsTheme.shapes.small,
            )
            .border(
                MdsTheme.sizes.borderWidth,
                if (checked) colors.accent else colors.borderStrong,
                MdsTheme.shapes.small,
            )
            .clickable { onToggle(!checked) },
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            MdsIcon(
                icon = MdsIcons.Check,
                contentDescription = "Checked",
                tint = colors.textOnAccent,
                size = 12.dp,
            )
        }
    }
}
