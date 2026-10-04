package com.github.adibfara.mds.core.persistence

import com.github.adibfara.mds.core.designsystem.theme.ThemeMode
import com.github.adibfara.mds.core.model.EditorMode
import com.github.adibfara.mds.core.model.RecentSpace
import com.github.adibfara.mds.core.model.SortOrder
import com.github.adibfara.mds.core.model.SpaceId
import kotlinx.serialization.Serializable

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
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val recentSpaces: List<RecentSpace> = emptyList(),
    val lastSpaceId: SpaceId? = null,
    val startupBehavior: StartupBehavior = StartupBehavior.RestoreSession,
    val startupFilePath: String? = null,
    val editorFontSize: Float = 15.5f,
    val defaultEditorMode: EditorMode = EditorMode.Preview,
    val sortOrder: SortOrder = SortOrder.NameAscending,
    val autoSaveDelayMillis: Long = 800L,
    val showStatusBar: Boolean = true,
    val showFormattingToolbar: Boolean = true,
)
