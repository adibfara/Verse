package com.github.adibfara.mds.core.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import java.io.File
import java.lang.ref.WeakReference
import kotlinx.coroutines.CompletableDeferred

object AndroidPlatform {
    private var applicationContext: Context? = null
    private var activityRef: WeakReference<ComponentActivity>? = null

    fun install(context: Context) {
        applicationContext = context.applicationContext
    }

    fun installActivity(activity: ComponentActivity) {
        activityRef = WeakReference(activity)
    }

    internal fun require(): Context =
        applicationContext ?: error("AndroidPlatform.install() was not called")

    internal fun activityOrNull(): ComponentActivity? = activityRef?.get()
}

object AndroidPermissionBridge {
    private var pendingAllFiles: CompletableDeferred<Boolean>? = null
    private var pendingLegacy: CompletableDeferred<Boolean>? = null

    var launchAllFilesIntent: ((Intent) -> Unit)? = null
    var launchLegacyRequest: ((Array<String>) -> Unit)? = null

    suspend fun requestAllFilesAccess(): Boolean {
        val launcher = launchAllFilesIntent ?: return false
        val deferred = CompletableDeferred<Boolean>()
        pendingAllFiles = deferred
        val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
            data = Uri.parse("package:${AndroidPlatform.require().packageName}")
        }
        launcher(intent)
        return deferred.await()
    }

    fun completeAllFilesAccess() {
        pendingAllFiles?.complete(Environment.isExternalStorageManager())
        pendingAllFiles = null
    }

    suspend fun requestLegacyPermissions(): Boolean {
        val launcher = launchLegacyRequest ?: return false
        val deferred = CompletableDeferred<Boolean>()
        pendingLegacy = deferred
        launcher(
            arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ),
        )
        return deferred.await()
    }

    fun completeLegacyPermissions(granted: Boolean) {
        pendingLegacy?.complete(granted)
        pendingLegacy = null
    }
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

private class AndroidStoragePermission : StoragePermission {

    override fun isGranted(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        val context = AndroidPlatform.require()
        androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    override suspend fun request(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        AndroidPermissionBridge.requestAllFilesAccess()
    } else {
        AndroidPermissionBridge.requestLegacyPermissions()
    }
}

actual fun createSystemOpener(): SystemOpener = AndroidSystemOpener()

actual fun createAppPaths(): AppPaths = AndroidAppPaths()

actual fun createLocationPicker(): LocationPicker = AndroidLocationPicker()

actual fun createStoragePermission(): StoragePermission = AndroidStoragePermission()

actual fun currentPlatformKind(): PlatformKind = PlatformKind.Mobile

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}

@Composable
actual fun PlatformFolderPickerOverlay() {
    AndroidFolderPickerOverlay()
}
