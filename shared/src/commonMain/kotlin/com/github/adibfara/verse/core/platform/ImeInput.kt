package com.github.adibfara.verse.core.platform

import androidx.compose.runtime.Composable
import com.github.adibfara.verse.core.markdown.MdRange

class ImeBufferMirror {
    var text: String = ""
        private set
    var selection: MdRange = MdRange(0, 0)
        private set
    var applyingFixup: Boolean = false

    fun update(text: String, selectionStart: Int, selectionEnd: Int) {
        this.text = text
        selection = MdRange(minOf(selectionStart, selectionEnd), maxOf(selectionStart, selectionEnd))
    }
}

@Composable
expect fun ListAwareImeInput(
    mirror: ImeBufferMirror,
    enabled: Boolean,
    content: @Composable () -> Unit,
)
