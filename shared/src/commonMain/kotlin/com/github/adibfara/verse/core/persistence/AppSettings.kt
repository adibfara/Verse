package com.github.adibfara.verse.core.persistence

import com.github.adibfara.verse.core.command.CommandId
import com.github.adibfara.verse.core.command.KeyBinding
import com.github.adibfara.verse.core.designsystem.theme.ThemeMode
import com.github.adibfara.verse.core.model.EditorMode
import com.github.adibfara.verse.core.model.RecentSpace
import com.github.adibfara.verse.core.model.SortOrder
import com.github.adibfara.verse.core.model.SpaceId
import kotlinx.serialization.Serializable

const val EditorFontSizeStep = 1f
const val MinEditorFontSize = 9f
const val MaxEditorFontSize = 40f

@Serializable
enum class StartupBehavior {
    RestoreSession,
    OpenSpecificFile,
    EmptyWorkspace;

    val label: String
        get() = when (this) {
            RestoreSession -> "Restore my last session"
            OpenSpecificFile -> "Always open a specific file"
            EmptyWorkspace -> "Start with nothing open"
        }
}

@Serializable
enum class LockBehavior {
    AlwaysUnlocked,
    Remember,
    RememberPerFile;

    val label: String
        get() = when (this) {
            AlwaysUnlocked -> "Always unlocked"
            Remember -> "Remember last"
            RememberPerFile -> "Remember per file"
        }
}

@Serializable
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val recentSpaces: List<RecentSpace> = emptyList(),
    val lastSpaceId: SpaceId? = null,
    val startupBehavior: StartupBehavior = StartupBehavior.RestoreSession,
    val startupFilePath: String? = null,
    val editorFontSize: Float = 16.5f,
    val defaultEditorMode: EditorMode = EditorMode.Preview,
    val sortOrder: SortOrder = SortOrder.NameAscending,
    val autoSaveDelayMillis: Long = 800L,
    val showStatusBar: Boolean = true,
    val showFormattingToolbar: Boolean = true,
    val lockBehavior: LockBehavior = LockBehavior.RememberPerFile,
    val rememberLocked: Boolean = false,
    val keyBindingOverrides: Map<String, KeyBinding?> = emptyMap(),
) {
    val commandBindingOverrides: Map<CommandId, KeyBinding?>
        get() = keyBindingOverrides.mapKeys { (id, _) -> CommandId(id) }
}

fun AppSettings.withAdjustedEditorFontSize(delta: Float): AppSettings = copy(
    editorFontSize = (editorFontSize + delta).coerceIn(MinEditorFontSize, MaxEditorFontSize),
)

fun formatEditorFontSize(size: Float): String =
    if (size == size.toInt().toFloat()) size.toInt().toString() else size.toString()
