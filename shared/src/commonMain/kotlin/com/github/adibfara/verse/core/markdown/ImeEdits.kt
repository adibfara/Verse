package com.github.adibfara.verse.core.markdown

enum class ImeKey {
    Enter,
    Backspace,
}

data class TextChange(val start: Int, val end: Int, val inserted: String)

fun textChangeBetween(before: String, after: String): TextChange {
    val limit = minOf(before.length, after.length)
    var prefix = 0
    while (prefix < limit && before[prefix] == after[prefix]) prefix++
    var suffix = 0
    while (suffix < limit - prefix &&
        before[before.length - 1 - suffix] == after[after.length - 1 - suffix]
    ) {
        suffix++
    }
    return TextChange(prefix, before.length - suffix, after.substring(prefix, after.length - suffix))
}

fun imeKeyEdit(text: String, selection: MdRange, key: ImeKey): TextEdit? {
    if (selection.start != selection.end) return null
    val caret = selection.start.coerceIn(0, text.length)
    val action = when (key) {
        ImeKey.Enter -> {
            if (lineMarkerKindAt(text, caret) == LineMarkerKind.None) return null
            FormatAction.ContinueList(caret)
        }
        ImeKey.Backspace -> {
            if (!caretAfterTaskMarker(text, caret)) return null
            FormatAction.DeleteTaskMarker(caret)
        }
    }
    val edit = applyFormatAction(text, MdRange(caret, caret), action)
    return edit.takeUnless { it.text == text && it.selection == selection }
}

fun imeBatchFixup(before: String, after: String, selection: MdRange): TextEdit? {
    if (before == after) return null
    val newline = insertedNewlineOffset(before, after)
    if (newline != null) {
        if (selection.start != selection.end) return null
        val edit = continueListAfterNewline(after, newline, selection)
        return edit.takeUnless { it.text == after }
    }
    return taskMarkerBackspaceEdit(before, after, selection)
}
