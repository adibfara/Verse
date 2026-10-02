package com.github.adibfara.mds.core.platform

import androidx.compose.runtime.Composable

interface SystemOpener {
    fun openInDefaultApplication(absolutePath: String)
    fun revealInFileManager(absolutePath: String)
    fun openUrl(url: String)
}

interface AppPaths {
    val appDataDirectory: String
    val homeDirectory: String
}

interface LocationPicker {
    suspend fun pickDirectory(title: String): String?
    suspend fun pickFile(title: String): String?
}

enum class PlatformKind {
    Desktop,
    Mobile;

    val isDesktop: Boolean get() = this == Desktop
}

expect fun createSystemOpener(): SystemOpener

expect fun createAppPaths(): AppPaths

expect fun createLocationPicker(): LocationPicker

expect fun currentPlatformKind(): PlatformKind

@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
