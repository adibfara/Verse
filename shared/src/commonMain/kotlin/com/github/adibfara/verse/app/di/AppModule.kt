package com.github.adibfara.verse.app.di

import com.github.adibfara.verse.app.ExternalOpenRequests
import com.github.adibfara.verse.app.WorkspaceViewModel
import com.github.adibfara.verse.core.command.CommandRegistry
import com.github.adibfara.verse.core.common.DispatcherProvider
import com.github.adibfara.verse.core.common.platformDispatchers
import com.github.adibfara.verse.core.filesystem.SpaceFileSystemFactory
import com.github.adibfara.verse.core.filesystem.createSpaceFileSystemFactory
import com.github.adibfara.verse.core.persistence.FileStorage
import com.github.adibfara.verse.core.persistence.SettingsStore
import com.github.adibfara.verse.core.persistence.WorkspaceSessionRepository
import com.github.adibfara.verse.core.persistence.WorkspaceStore
import com.github.adibfara.verse.core.persistence.createFileStorage
import com.github.adibfara.verse.core.platform.AppPaths
import com.github.adibfara.verse.core.platform.LocationPicker
import com.github.adibfara.verse.core.platform.LocationResolver
import com.github.adibfara.verse.core.platform.SpeechRecognition
import com.github.adibfara.verse.core.platform.StoragePermission
import com.github.adibfara.verse.core.platform.SystemOpener
import com.github.adibfara.verse.core.platform.createAppPaths
import com.github.adibfara.verse.core.platform.createLocationPicker
import com.github.adibfara.verse.core.platform.createLocationResolver
import com.github.adibfara.verse.core.platform.createSpeechRecognition
import com.github.adibfara.verse.core.platform.createStoragePermission
import com.github.adibfara.verse.core.platform.createSystemOpener
import com.github.adibfara.verse.feature.editor.EditorViewModel
import com.github.adibfara.verse.feature.explorer.ExplorerViewModel
import com.github.adibfara.verse.feature.search.SearchViewModel
import com.github.adibfara.verse.feature.space.SpaceRepository
import com.github.adibfara.verse.feature.tabs.TabsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val ApplicationScopeQualifier = named("applicationScope")

val appModule = module {
    single<DispatcherProvider> { platformDispatchers() }
    single<AppPaths> { createAppPaths() }
    single<SystemOpener> { createSystemOpener() }
    single<LocationPicker> { createLocationPicker() }
    single<LocationResolver> { createLocationResolver() }
    single<StoragePermission> { createStoragePermission() }
    single<SpeechRecognition> { createSpeechRecognition() }
    single<FileStorage> { createFileStorage() }
    single<SpaceFileSystemFactory> { createSpaceFileSystemFactory() }

    single(ApplicationScopeQualifier) {
        CoroutineScope(SupervisorJob() + get<DispatcherProvider>().default)
    }

    single {
        SettingsStore(
            storage = get(),
            appPaths = get(),
            dispatchers = get(),
            scope = get(ApplicationScopeQualifier),
        )
    }

    single {
        WorkspaceStore(
            storage = get(),
            appPaths = get(),
            dispatchers = get(),
            scope = get(ApplicationScopeQualifier),
        )
    }

    single { WorkspaceSessionRepository(store = get()) }

    single {
        SpaceRepository(
            fileSystemFactory = get(),
            locationResolver = get(),
            settingsStore = get(),
        )
    }

    single { CommandRegistry() }

    single { ExternalOpenRequests() }

    viewModel {
        WorkspaceViewModel(
            settingsStore = get(),
            spaceRepository = get(),
            sessionRepository = get(),
            locationPicker = get(),
            appPaths = get(),
            storagePermission = get(),
        )
    }

    viewModel {
        ExplorerViewModel(
            spaceRepository = get(),
            sessionRepository = get(),
            settingsStore = get(),
            systemOpener = get(),
            dispatchers = get(),
        )
    }

    viewModel { TabsViewModel(sessionRepository = get()) }

    viewModel {
        SearchViewModel(
            spaceRepository = get(),
            registry = get(),
            dispatchers = get(),
        )
    }

    viewModel {
        EditorViewModel(
            spaceRepository = get(),
            settingsStore = get(),
            dispatchers = get(),
        )
    }
}
