package com.github.adibfara.verse.core.command

import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

@JvmInline
value class CommandId(val value: String)

enum class CommandCategory {
    File,
    Edit,
    View,
    Navigation,
    Workspace;

    val label: String
        get() = when (this) {
            File -> "File"
            Edit -> "Edit"
            View -> "View"
            Navigation -> "Navigation"
            Workspace -> "Workspace"
        }
}

enum class ShortcutKey {
    A, B, C, D, E, F, G, H, I, J, K, L, M,
    N, O, P, Q, R, S, T, U, V, W, X, Y, Z,
    Digit1, Digit2, Digit3, Digit4, Digit5, Digit6, Digit7, Digit8, Digit9,
    Comma, Period, Slash, Backslash, Equals, Minus, Plus,
    Tab, Escape, Enter, Delete, Backspace, Space,
    F2, F3,
    ArrowUp, ArrowDown, ArrowLeft, ArrowRight;

    val label: String
        get() = when (this) {
            Digit1 -> "1"
            Digit2 -> "2"
            Digit3 -> "3"
            Digit4 -> "4"
            Digit5 -> "5"
            Digit6 -> "6"
            Digit7 -> "7"
            Digit8 -> "8"
            Digit9 -> "9"
            Comma -> ","
            Period -> "."
            Slash -> "/"
            Backslash -> "\\"
            Equals -> "="
            Minus -> "-"
            Plus -> "+"
            ArrowUp -> "Up"
            ArrowDown -> "Down"
            ArrowLeft -> "Left"
            ArrowRight -> "Right"
            else -> name
        }
}

@Serializable
data class KeyBinding(
    val key: ShortcutKey,
    val control: Boolean = false,
    val shift: Boolean = false,
    val alt: Boolean = false,
) {
    val label: String
        get() = buildString {
            if (control) append("Ctrl+")
            if (alt) append("Alt+")
            if (shift) append("Shift+")
            append(key.label)
        }
}

data class Command(
    val id: CommandId,
    val title: String,
    val category: CommandCategory,
    val binding: KeyBinding? = null,
    val aliasBindings: List<KeyBinding> = emptyList(),
) {
    val allBindings: List<KeyBinding> get() = listOfNotNull(binding) + aliasBindings
}

object Commands {

    val NewNote = Command(
        id = CommandId("file.newNote"),
        title = "New note",
        category = CommandCategory.File,
        binding = KeyBinding(ShortcutKey.N, control = true),
    )

    val NewFolder = Command(
        id = CommandId("file.newFolder"),
        title = "New folder",
        category = CommandCategory.File,
        binding = KeyBinding(ShortcutKey.N, control = true, shift = true),
    )

    val Save = Command(
        id = CommandId("file.save"),
        title = "Save current file",
        category = CommandCategory.File,
        binding = KeyBinding(ShortcutKey.S, control = true),
    )

    val QuickOpen = Command(
        id = CommandId("navigation.quickOpen"),
        title = "Quick open by file name",
        category = CommandCategory.Navigation,
        binding = KeyBinding(ShortcutKey.O, control = true),
    )

    val SearchInFiles = Command(
        id = CommandId("navigation.searchInFiles"),
        title = "Search in files",
        category = CommandCategory.Navigation,
        binding = KeyBinding(ShortcutKey.F, control = true, shift = true),
    )

    val CommandPalette = Command(
        id = CommandId("navigation.commandPalette"),
        title = "Command palette",
        category = CommandCategory.Navigation,
        binding = KeyBinding(ShortcutKey.P, control = true),
    )

    val CloseTab = Command(
        id = CommandId("workspace.closeTab"),
        title = "Close current tab",
        category = CommandCategory.Workspace,
        binding = KeyBinding(ShortcutKey.W, control = true),
    )

    val NextTab = Command(
        id = CommandId("workspace.nextTab"),
        title = "Go to next tab",
        category = CommandCategory.Workspace,
        binding = KeyBinding(ShortcutKey.Tab, control = true),
    )

    val PreviousTab = Command(
        id = CommandId("workspace.previousTab"),
        title = "Go to previous tab",
        category = CommandCategory.Workspace,
        binding = KeyBinding(ShortcutKey.Tab, control = true, shift = true),
    )

    val ToggleLivePreview = Command(
        id = CommandId("view.toggleLivePreview"),
        title = "Toggle live and preview mode",
        category = CommandCategory.View,
        binding = KeyBinding(ShortcutKey.E, control = true),
    )

    val ToggleRawMode = Command(
        id = CommandId("view.toggleRawMode"),
        title = "Toggle raw markdown mode",
        category = CommandCategory.View,
        binding = KeyBinding(ShortcutKey.E, control = true, shift = true),
    )

    val ToggleSidebar = Command(
        id = CommandId("view.toggleSidebar"),
        title = "Toggle sidebar",
        category = CommandCategory.View,
        binding = KeyBinding(ShortcutKey.B, control = true),
    )

    val ToggleTheme = Command(
        id = CommandId("view.toggleTheme"),
        title = "Toggle light and dark theme",
        category = CommandCategory.View,
    )

    val ToggleFormattingToolbar = Command(
        id = CommandId("view.toggleFormattingToolbar"),
        title = "Toggle formatting toolbar",
        category = CommandCategory.View,
    )

    val IncreaseFontSize = Command(
        id = CommandId("view.increaseFontSize"),
        title = "Increase editor font size",
        category = CommandCategory.View,
        binding = KeyBinding(ShortcutKey.Equals, control = true),
        aliasBindings = listOf(
            KeyBinding(ShortcutKey.Equals, control = true, shift = true),
            KeyBinding(ShortcutKey.Plus, control = true),
            KeyBinding(ShortcutKey.Plus, control = true, shift = true),
        ),
    )

    val DecreaseFontSize = Command(
        id = CommandId("view.decreaseFontSize"),
        title = "Decrease editor font size",
        category = CommandCategory.View,
        binding = KeyBinding(ShortcutKey.Minus, control = true),
        aliasBindings = listOf(
            KeyBinding(ShortcutKey.Minus, control = true, shift = true),
        ),
    )

    val OpenSettings = Command(
        id = CommandId("workspace.openSettings"),
        title = "Open settings",
        category = CommandCategory.Workspace,
        binding = KeyBinding(ShortcutKey.Comma, control = true),
    )

    val CloseSpace = Command(
        id = CommandId("workspace.closeSpace"),
        title = "Close this space",
        category = CommandCategory.Workspace,
    )

    private val jumpToTab: List<Command> = listOf(
        ShortcutKey.Digit1,
        ShortcutKey.Digit2,
        ShortcutKey.Digit3,
        ShortcutKey.Digit4,
        ShortcutKey.Digit5,
        ShortcutKey.Digit6,
        ShortcutKey.Digit7,
        ShortcutKey.Digit8,
        ShortcutKey.Digit9,
    ).mapIndexed { index, key ->
        Command(
            id = jumpToTabId(index),
            title = "Go to tab ${index + 1}",
            category = CommandCategory.Workspace,
            binding = KeyBinding(key, control = true),
        )
    }

    fun jumpToTabId(index: Int) = CommandId("workspace.jumpToTab.$index")

    fun tabIndexOf(id: CommandId): Int? = id.value
        .removePrefix("workspace.jumpToTab.")
        .takeIf { it != id.value }
        ?.toIntOrNull()

    val all: List<Command> = listOf(
        NewNote,
        NewFolder,
        Save,
        QuickOpen,
        SearchInFiles,
        CommandPalette,
        CloseTab,
        NextTab,
        PreviousTab,
        ToggleLivePreview,
        ToggleRawMode,
        ToggleSidebar,
        ToggleTheme,
        ToggleFormattingToolbar,
        IncreaseFontSize,
        DecreaseFontSize,
        OpenSettings,
        CloseSpace,
    ) + jumpToTab
}
