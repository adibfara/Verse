package com.github.adibfara.mds.feature.editor.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.adibfara.mds.core.designsystem.component.MdsText
import com.github.adibfara.mds.core.designsystem.theme.MdsTheme
import com.github.adibfara.mds.core.markdown.MdBlock

@Composable
fun CodeBlockRenderer(block: MdBlock.CodeBlock, modifier: Modifier = Modifier) {
    val colors = MdsTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = MdsTheme.spacing.sm)
            .background(colors.codeBackground, MdsTheme.shapes.medium)
            .padding(MdsTheme.spacing.md),
    ) {
        block.language?.let { language ->
            MdsText(
                text = language,
                style = MdsTheme.typography.uiSmall,
                color = colors.textMuted,
                modifier = Modifier.padding(bottom = MdsTheme.spacing.xs),
            )
        }
        MdsText(
            text = block.code,
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            style = MdsTheme.typography.editorCode,
            color = colors.codeText,
        )
    }
}
