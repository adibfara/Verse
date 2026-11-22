package com.github.adibfara.verse.core.platform

import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.CompletionInfo
import android.view.inputmethod.CorrectionInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import android.view.inputmethod.TextAttribute
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.PlatformTextInputInterceptor
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import com.github.adibfara.verse.core.markdown.ImeKey
import com.github.adibfara.verse.core.markdown.TextEdit
import com.github.adibfara.verse.core.markdown.imeBatchFixup
import com.github.adibfara.verse.core.markdown.imeKeyEdit
import com.github.adibfara.verse.core.markdown.textChangeBetween

private const val ImeLogTag = "VerseIme"

private fun imeLog(message: () -> String) {
    Log.i(ImeLogTag, message())
}

private fun String.visible(): String = takeLast(24).replace("\n", "⏎")

@Composable
actual fun ListAwareImeInput(
    mirror: ImeBufferMirror,
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    imeLog { "ListAwareImeInput composed enabled=$enabled" }
    if (!enabled) {
        content()
        return
    }
    val interceptor = remember(mirror) {
        PlatformTextInputInterceptor { request, nextHandler ->
            imeLog { "interceptStartInputMethod ${request::class.simpleName}" }
            nextHandler.startInputMethod(
                PlatformTextInputMethodRequest { outAttributes ->
                    imeLog { "connection created" }
                    ListAwareInputConnection(request.createInputConnection(outAttributes), mirror)
                },
            )
        }
    }
    InterceptPlatformTextInput(interceptor = interceptor, content = content)
}

private class ListAwareInputConnection(
    target: InputConnection,
    private val mirror: ImeBufferMirror,
) : InputConnectionWrapper(target, true) {

    private var depth = 0
    private var textBeforeBatch: String? = null
    private var fixing = false
    private var swallowedKeyUp: Int? = null

    private fun opened() {
        if (fixing) return
        if (depth == 0) textBeforeBatch = mirror.text
        depth++
    }

    private fun closed() {
        if (fixing || depth == 0) return
        depth--
        if (depth > 0) return
        mirror.applyingFixup = false
        val before = textBeforeBatch ?: return
        textBeforeBatch = null
        val after = mirror.text
        val fixup = imeBatchFixup(before, after, mirror.selection)
        imeLog { "batch closed before='${before.visible()}' after='${after.visible()}' fixup=${fixup != null}" }
        fixup?.let(::applyEdit)
    }

    private inline fun tracked(name: String, block: () -> Boolean): Boolean {
        imeLog { name }
        opened()
        try {
            return block()
        } finally {
            closed()
        }
    }

    private fun applyEdit(edit: TextEdit) {
        val change = textChangeBetween(mirror.text, edit.text)
        imeLog { "apply ${change.start}..${change.end} '${change.inserted.visible()}' caret=${edit.selection}" }
        fixing = true
        mirror.applyingFixup = true
        try {
            super.beginBatchEdit()
            super.finishComposingText()
            if (change.start != change.end || change.inserted.isNotEmpty()) {
                super.setSelection(change.start, change.end)
                super.commitText(change.inserted, 1)
            }
            super.setSelection(edit.selection.start, edit.selection.end)
            super.endBatchEdit()
        } finally {
            fixing = false
            if (depth == 0) mirror.applyingFixup = false
        }
        imeLog { "applied, mirror='${mirror.text.visible()}'" }
    }

    override fun beginBatchEdit(): Boolean {
        imeLog { "beginBatchEdit depth=$depth" }
        opened()
        return super.beginBatchEdit()
    }

    override fun endBatchEdit(): Boolean {
        val result = super.endBatchEdit()
        imeLog { "endBatchEdit depth=$depth" }
        closed()
        return result
    }

    override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean =
        tracked("commitText '${text?.toString()?.visible()}'") { super.commitText(text, newCursorPosition) }

    @RequiresApi(33)
    override fun commitText(text: CharSequence, newCursorPosition: Int, textAttribute: TextAttribute?): Boolean =
        tracked("commitText(attr) '${text.toString().visible()}'") {
            super.commitText(text, newCursorPosition, textAttribute)
        }

    override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean =
        tracked("setComposingText '${text?.toString()?.visible()}'") { super.setComposingText(text, newCursorPosition) }

    @RequiresApi(33)
    override fun setComposingText(text: CharSequence, newCursorPosition: Int, textAttribute: TextAttribute?): Boolean =
        tracked("setComposingText(attr) '${text.toString().visible()}'") {
            super.setComposingText(text, newCursorPosition, textAttribute)
        }

    override fun setComposingRegion(start: Int, end: Int): Boolean =
        tracked("setComposingRegion $start..$end") { super.setComposingRegion(start, end) }

    @RequiresApi(33)
    override fun setComposingRegion(start: Int, end: Int, textAttribute: TextAttribute?): Boolean =
        tracked("setComposingRegion(attr) $start..$end") { super.setComposingRegion(start, end, textAttribute) }

    @RequiresApi(34)
    override fun replaceText(
        start: Int,
        end: Int,
        text: CharSequence,
        newCursorPosition: Int,
        textAttribute: TextAttribute?,
    ): Boolean = tracked("replaceText $start..$end '${text.toString().visible()}'") {
        super.replaceText(start, end, text, newCursorPosition, textAttribute)
    }

    override fun finishComposingText(): Boolean =
        tracked("finishComposingText") { super.finishComposingText() }

    override fun commitCompletion(text: CompletionInfo?): Boolean =
        tracked("commitCompletion") { super.commitCompletion(text) }

    override fun commitCorrection(correctionInfo: CorrectionInfo?): Boolean =
        tracked("commitCorrection") { super.commitCorrection(correctionInfo) }

    override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean =
        tracked("deleteSurroundingText $beforeLength,$afterLength") {
            super.deleteSurroundingText(beforeLength, afterLength)
        }

    override fun deleteSurroundingTextInCodePoints(beforeLength: Int, afterLength: Int): Boolean =
        tracked("deleteSurroundingTextInCodePoints $beforeLength,$afterLength") {
            super.deleteSurroundingTextInCodePoints(beforeLength, afterLength)
        }

    override fun setSelection(start: Int, end: Int): Boolean =
        tracked("setSelection $start..$end") { super.setSelection(start, end) }

    override fun sendKeyEvent(event: KeyEvent?): Boolean {
        imeLog { "sendKeyEvent ${event?.action} ${event?.keyCode}" }
        if (event == null || fixing) return super.sendKeyEvent(event)
        val key = imeKeyOf(event) ?: return super.sendKeyEvent(event)
        if (event.action == KeyEvent.ACTION_UP) {
            if (swallowedKeyUp != event.keyCode) return super.sendKeyEvent(event)
            swallowedKeyUp = null
            return true
        }
        if (event.action != KeyEvent.ACTION_DOWN) return super.sendKeyEvent(event)
        val edit = imeKeyEdit(mirror.text, mirror.selection, key)
        imeLog { "key $key on '${mirror.text.visible()}' caret=${mirror.selection} edit=${edit != null}" }
        if (edit == null) return super.sendKeyEvent(event)
        applyEdit(edit)
        if (depth > 0) textBeforeBatch = null
        swallowedKeyUp = event.keyCode
        return true
    }

    override fun closeConnection() {
        imeLog { "closeConnection" }
        depth = 0
        textBeforeBatch = null
        mirror.applyingFixup = false
        swallowedKeyUp = null
        super.closeConnection()
    }
}

private fun imeKeyOf(event: KeyEvent): ImeKey? {
    val modified = event.isShiftPressed || event.isCtrlPressed || event.isAltPressed || event.isMetaPressed
    if (modified) return null
    return when (event.keyCode) {
        KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> ImeKey.Enter
        KeyEvent.KEYCODE_DEL -> ImeKey.Backspace
        else -> null
    }
}
