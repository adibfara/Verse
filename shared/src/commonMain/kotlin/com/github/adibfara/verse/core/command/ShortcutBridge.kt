package com.github.adibfara.verse.core.command

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

private val keyMap: Map<Key, ShortcutKey> = mapOf(
    Key.A to ShortcutKey.A,
    Key.B to ShortcutKey.B,
    Key.C to ShortcutKey.C,
    Key.D to ShortcutKey.D,
    Key.E to ShortcutKey.E,
    Key.F to ShortcutKey.F,
    Key.G to ShortcutKey.G,
    Key.H to ShortcutKey.H,
    Key.I to ShortcutKey.I,
    Key.J to ShortcutKey.J,
    Key.K to ShortcutKey.K,
    Key.L to ShortcutKey.L,
    Key.M to ShortcutKey.M,
    Key.N to ShortcutKey.N,
    Key.O to ShortcutKey.O,
    Key.P to ShortcutKey.P,
    Key.Q to ShortcutKey.Q,
    Key.R to ShortcutKey.R,
    Key.S to ShortcutKey.S,
    Key.T to ShortcutKey.T,
    Key.U to ShortcutKey.U,
    Key.V to ShortcutKey.V,
    Key.W to ShortcutKey.W,
    Key.X to ShortcutKey.X,
    Key.Y to ShortcutKey.Y,
    Key.Z to ShortcutKey.Z,
    Key.One to ShortcutKey.Digit1,
    Key.Two to ShortcutKey.Digit2,
    Key.Three to ShortcutKey.Digit3,
    Key.Four to ShortcutKey.Digit4,
    Key.Five to ShortcutKey.Digit5,
    Key.Six to ShortcutKey.Digit6,
    Key.Seven to ShortcutKey.Digit7,
    Key.Eight to ShortcutKey.Digit8,
    Key.Nine to ShortcutKey.Digit9,
    Key.Comma to ShortcutKey.Comma,
    Key.Period to ShortcutKey.Period,
    Key.Slash to ShortcutKey.Slash,
    Key.Backslash to ShortcutKey.Backslash,
    Key.Equals to ShortcutKey.Equals,
    Key.NumPadEquals to ShortcutKey.Equals,
    Key.Minus to ShortcutKey.Minus,
    Key.NumPadSubtract to ShortcutKey.Minus,
    Key.Plus to ShortcutKey.Plus,
    Key.NumPadAdd to ShortcutKey.Plus,
    Key.Tab to ShortcutKey.Tab,
    Key.Escape to ShortcutKey.Escape,
    Key.Enter to ShortcutKey.Enter,
    Key.Delete to ShortcutKey.Delete,
    Key.Backspace to ShortcutKey.Backspace,
    Key.Spacebar to ShortcutKey.Space,
    Key.F2 to ShortcutKey.F2,
    Key.F3 to ShortcutKey.F3,
    Key.DirectionUp to ShortcutKey.ArrowUp,
    Key.DirectionDown to ShortcutKey.ArrowDown,
    Key.DirectionLeft to ShortcutKey.ArrowLeft,
    Key.DirectionRight to ShortcutKey.ArrowRight,
)

fun KeyEvent.toBinding(): KeyBinding? {
    if (type != KeyEventType.KeyDown) return null
    val mapped = keyMap[key] ?: return null
    return KeyBinding(
        key = mapped,
        control = isCtrlPressed || isMetaPressed,
        shift = isShiftPressed,
        alt = isAltPressed,
    )
}

fun Modifier.commandShortcuts(
    registry: CommandRegistry,
    onCommand: (CommandId) -> Boolean,
): Modifier = onPreviewKeyEvent { event ->
    val binding = event.toBinding() ?: return@onPreviewKeyEvent false
    if (!binding.control && !binding.alt && binding.key != ShortcutKey.Escape) {
        return@onPreviewKeyEvent false
    }
    val commandId = registry.resolve(binding) ?: return@onPreviewKeyEvent false
    onCommand(commandId)
}
