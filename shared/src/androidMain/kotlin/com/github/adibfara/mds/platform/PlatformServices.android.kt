package com.github.adibfara.mds.core.platform

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import android.net.Uri
import java.io.File

object AndroidPlatform {
    private var applicationContext: Context? = null

    fun install(context: Context) {
        applicationContext = context.applicationContext
    }

    internal fun require(): Context =
        applicationContext ?: error("AndroidPlatform.install() was not called")
}

private class AndroidSystemOpener : SystemOpener {

    override fun openInDefaultApplication(absolutePath: String) {
        val context = AndroidPlatform.require()
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.fromFile(File(absolutePath))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { context.startActivity(intent) }
    }

    override fun revealInFileManager(absolutePath: String) = openInDefaultApplication(absolutePath)

    override fun openUrl(url: String) {
        val context = AndroidPlatform.require()
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }
}

private class AndroidAppPaths : AppPaths {

    override val appDataDirectory: String
        get() = AndroidPlatform.require().filesDir.absolutePath

    override val homeDirectory: String
        get() = AndroidPlatform.require().filesDir.absolutePath
}

private class AndroidLocationPicker : LocationPicker {
    override suspend fun pickDirectory(title: String): String? = null
    override suspend fun pickFile(title: String): String? = null
}

actual fun createSystemOpener(): SystemOpener = AndroidSystemOpener()

actual fun createAppPaths(): AppPaths = AndroidAppPaths()

actual fun createLocationPicker(): LocationPicker = AndroidLocationPicker()

actual fun currentPlatformKind(): PlatformKind = PlatformKind.Mobile

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}
