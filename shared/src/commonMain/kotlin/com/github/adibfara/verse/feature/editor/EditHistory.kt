package com.github.adibfara.verse.feature.editor

import androidx.compose.ui.text.input.TextFieldValue

private const val HistoryCapacity = 200
private const val GroupWindowMillis = 1000L

class EditHistory(
    private val now: () -> Long,
    private val capacity: Int = HistoryCapacity,
    private val groupWindowMillis: Long = GroupWindowMillis,
) {
    private val undoStack = ArrayDeque<TextFieldValue>()
    private val redoStack = ArrayDeque<TextFieldValue>()
    private var groupOpen = false
    private var lastRecordedAt = 0L

    val canUndo: Boolean get() = undoStack.isNotEmpty()

    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun record(before: TextFieldValue, after: TextFieldValue, boundary: Boolean = false) {
        if (before.text == after.text) return
        val time = now()
        val continuesGroup = groupOpen &&
            !boundary &&
            time - lastRecordedAt <= groupWindowMillis &&
            !endsWithLineBreak(after)
        if (!continuesGroup) push(undoStack, before)
        redoStack.clear()
        groupOpen = !boundary
        lastRecordedAt = time
    }

    fun undo(current: TextFieldValue): TextFieldValue? {
        val target = undoStack.removeLastOrNull() ?: return null
        push(redoStack, current)
        groupOpen = false
        return target
    }

    fun redo(current: TextFieldValue): TextFieldValue? {
        val target = redoStack.removeLastOrNull() ?: return null
        push(undoStack, current)
        groupOpen = false
        return target
    }

    private fun push(stack: ArrayDeque<TextFieldValue>, value: TextFieldValue) {
        stack.addLast(TextFieldValue(text = value.text, selection = value.selection))
        if (stack.size > capacity) stack.removeFirst()
    }

    private fun endsWithLineBreak(value: TextFieldValue): Boolean {
        val caret = value.selection.min
        return caret > 0 && caret <= value.text.length && value.text[caret - 1] == '\n'
    }
}
