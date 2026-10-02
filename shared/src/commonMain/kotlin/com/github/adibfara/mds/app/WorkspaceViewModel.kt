package com.github.adibfara.mds.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.adibfara.mds.core.designsystem.theme.ThemeMode
import com.github.adibfara.mds.core.model.FilePath
import com.github.adibfara.mds.core.model.RecentSpace
import com.github.adibfara.mds.core.model.Space
import com.github.adibfara.mds.core.persistence.AppSettings
import com.github.adibfara.mds.core.persistence.SettingsStore
import com.github.adibfara.mds.core.persistence.StartupBehavior
import com.github.adibfara.mds.core.persistence.WorkspaceSessionRepository
import com.github.adibfara.mds.core.persistence.WorkspaceState
import com.github.adibfara.mds.core.platform.LocationPicker
import com.github.adibfara.mds.feature.space.SpaceRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppUiState(
    val isReady: Boolean = false,
    val settings: AppSettings = AppSettings(),
    val space: Space? = null,
    val sidebarVisible: Boolean = true,
    val sidebarWidth: Float = 260f,
) {
    val hasSpace: Boolean get() = space != null

    val recentSpaces: List<RecentSpace> get() = settings.recentSpaces

    val themeMode: ThemeMode get() = settings.themeMode
}

data class SessionRestore(val state: WorkspaceState, val initialFile: FilePath?)

class WorkspaceViewModel(
    private val settingsStore: SettingsStore,
    private val spaceRepository: SpaceRepository,
    private val sessionRepository: WorkspaceSessionRepository,
    private val locationPicker: LocationPicker,
) : ViewModel() {

    private val mutableState = MutableStateFlow(AppUiState())
    val state: StateFlow<AppUiState> = mutableState.asStateFlow()

    private val mutableRestores = MutableSharedFlow<SessionRestore>(
        replay = 1,
        extraBufferCapacity = 4,
    )
    val restores: SharedFlow<SessionRestore> = mutableRestores.asSharedFlow()

    fun start(launchArguments: LaunchArguments) {
        viewModelScope.launch {
            val settings = settingsStore.load()
            mutableState.value = mutableState.value.copy(settings = settings)

            viewModelScope.launch {
                settingsStore.settings.collect { updated ->
                    mutableState.value = mutableState.value.copy(settings = updated)
                }
            }

            val initialFile = launchArguments.initialFilePath
            when {
                initialFile != null && spaceRepository.isFile(initialFile) ->
                    openSingleFile(initialFile)

                initialFile != null && spaceRepository.isDirectory(initialFile) ->
                    openFolder(initialFile)

                else -> restoreLastSpace(settings)
            }
            mutableState.value = mutableState.value.copy(isReady = true)
        }
    }

    private suspend fun restoreLastSpace(settings: AppSettings) {
        if (settings.startupBehavior == StartupBehavior.EmptyWorkspace) return
        val lastId = settings.lastSpaceId ?: return
        val recent = settings.recentSpaces.firstOrNull { it.id == lastId } ?: return
        if (!spaceRepository.isDirectory(recent.location)) return
        attach(spaceRepository.openFolder(recent.location).space, initialFile = null)
    }

    fun openFolder(location: String) {
        viewModelScope.launch {
            attach(spaceRepository.openFolder(location).space, initialFile = null)
        }
    }

    fun openRecent(recent: RecentSpace) = openFolder(recent.location)

    fun forgetRecent(recent: RecentSpace) = settingsStore.forgetSpace(recent)

    fun openSingleFile(filePath: String) {
        viewModelScope.launch {
            val opened = spaceRepository.openSingleFile(filePath) ?: return@launch
            attach(opened.space, initialFile = opened.initialFile)
        }
    }

    fun chooseFolder() {
        viewModelScope.launch {
            val location = locationPicker.pickDirectory("Choose a space folder") ?: return@launch
            attach(spaceRepository.openFolder(location).space, initialFile = null)
        }
    }

    private suspend fun attach(space: Space, initialFile: FilePath?) {
        val restored = sessionRepository.attach(space)
        mutableState.value = mutableState.value.copy(
            space = space,
            sidebarVisible = restored.sidebarVisible,
            sidebarWidth = restored.sidebarWidth,
        )
        mutableRestores.emit(SessionRestore(state = restored, initialFile = initialFile))
    }

    fun closeSpace() {
        viewModelScope.launch {
            sessionRepository.flush()
            sessionRepository.detach()
            spaceRepository.close()
            mutableState.value = mutableState.value.copy(space = null)
        }
    }

    fun toggleSidebar() {
        val visible = !mutableState.value.sidebarVisible
        mutableState.value = mutableState.value.copy(sidebarVisible = visible)
        sessionRepository.update { it.copy(sidebarVisible = visible) }
    }

    fun resizeSidebar(delta: Float) {
        val updated = (mutableState.value.sidebarWidth + delta).coerceIn(160f, 520f)
        mutableState.value = mutableState.value.copy(sidebarWidth = updated)
        sessionRepository.update { it.copy(sidebarWidth = updated) }
    }

    fun setThemeMode(mode: ThemeMode) = settingsStore.update { it.copy(themeMode = mode) }

    fun toggleTheme() {
        val next = when (mutableState.value.settings.themeMode) {
            ThemeMode.Light -> ThemeMode.Dark
            ThemeMode.Dark -> ThemeMode.Light
            ThemeMode.System -> ThemeMode.Light
        }
        setThemeMode(next)
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) = settingsStore.update(transform)

    suspend fun flush() {
        sessionRepository.flush()
        settingsStore.flush()
    }
}
