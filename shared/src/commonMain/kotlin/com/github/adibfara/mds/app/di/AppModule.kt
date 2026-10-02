package com.github.adibfara.mds.app.di

import com.github.adibfara.mds.app.ExternalOpenRequests
import com.github.adibfara.mds.app.WorkspaceViewModel
import com.github.adibfara.mds.core.command.CommandRegistry
import com.github.adibfara.mds.core.common.DispatcherProvider
import com.github.adibfara.mds.core.common.platformDispatchers
import com.github.adibfara.mds.core.filesystem.SpaceFileSystemFactory
import com.github.adibfara.mds.core.filesystem.createSpaceFileSystemFactory
import com.github.adibfara.mds.core.persistence.FileStorage
import com.github.adibfara.mds.core.persistence.SettingsStore
import com.github.adibfara.mds.core.persistence.WorkspaceSessionRepository
import com.github.adibfara.mds.core.persistence.WorkspaceStore
import com.github.adibfara.mds.core.persistence.createFileStorage
import com.github.adibfara.mds.core.platform.AppPaths
import com.github.adibfara.mds.core.platform.LocationPicker
import com.github.adibfara.mds.core.platform.LocationResolver
import com.github.adibfara.mds.core.platform.StoragePermission
import com.github.adibfara.mds.core.platform.SystemOpener
import com.github.adibfara.mds.core.platform.createAppPaths
import com.github.adibfara.mds.core.platform.createLocationPicker
import com.github.adibfara.mds.core.platform.createLocationResolver
import com.github.adibfara.mds.core.platform.createStoragePermission
import com.github.adibfara.mds.core.platform.createSystemOpener
import com.github.adibfara.mds.feature.editor.EditorViewModel
import com.github.adibfara.mds.feature.explorer.ExplorerViewModel
import com.github.adibfara.mds.feature.search.SearchViewModel
import com.github.adibfara.mds.feature.space.SpaceRepository
import com.github.adibfara.mds.feature.tabs.TabsViewModel
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
