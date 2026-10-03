package com.github.adibfara.mds.core.platform

import androidx.compose.runtime.Composable
import java.awt.Desktop
import java.io.File
import java.net.URI
import javax.swing.JFileChooser
import javax.swing.UIManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private class JvmSystemOpener : SystemOpener {

    private val desktop: Desktop? =
        if (Desktop.isDesktopSupported()) Desktop.getDesktop() else null

    override fun openInDefaultApplication(absolutePath: String) {
        val file = File(absolutePath)
        if (!file.exists()) return
        runCatching { desktop?.open(file) }
    }

    override fun revealInFileManager(absolutePath: String) {
        val file = File(absolutePath)
        if (!file.exists()) return
        val revealed = runCatching {
            val osName = System.getProperty("os.name").orEmpty().lowercase()
            when {
                osName.contains("win") -> {
                    ProcessBuilder("explorer.exe", "/select,", file.absolutePath).start()
                    true
                }

                osName.contains("mac") -> {
                    ProcessBuilder("open", "-R", file.absolutePath).start()
                    true
                }

                else -> false
            }
        }.getOrDefault(false)
        if (!revealed) {
            runCatching { desktop?.open(file.parentFile ?: file) }
        }
    }

    override fun openUrl(url: String) {
        runCatching { desktop?.browse(URI(url)) }
    }

    override fun shareText(title: String, text: String) {
        runCatching {
            val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(java.awt.datatransfer.StringSelection(text), null)
        }
    }
}

private class JvmAppPaths : AppPaths {

    override val appDataDirectory: String by lazy {
        val osName = System.getProperty("os.name").orEmpty().lowercase()
        val base = when {
            osName.contains("win") -> System.getenv("APPDATA")
                ?: File(homeDirectory, "AppData/Roaming").path

            osName.contains("mac") -> File(homeDirectory, "Library/Application Support").path
            else -> System.getenv("XDG_CONFIG_HOME") ?: File(homeDirectory, ".config").path
        }
        File(base, "MDs").also { it.mkdirs() }.absolutePath
    }

    override val homeDirectory: String
        get() = System.getProperty("user.home").orEmpty()
}

private class JvmLocationPicker : LocationPicker {

    override suspend fun pickDirectory(title: String): String? =
        choose(title, JFileChooser.DIRECTORIES_ONLY)

    override suspend fun pickFile(title: String): String? =
        choose(title, JFileChooser.FILES_ONLY)

    private suspend fun choose(title: String, mode: Int): String? = withContext(Dispatchers.IO) {
        runCatching {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        }
        val chooser = JFileChooser().apply {
            dialogTitle = title
            fileSelectionMode = mode
            isMultiSelectionEnabled = false
        }
        val result = chooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) chooser.selectedFile?.absolutePath else null
    }
}

private class JvmStoragePermission : StoragePermission {
    override fun isGranted(): Boolean = true
    override suspend fun request(): Boolean = true
}

actual fun createSystemOpener(): SystemOpener = JvmSystemOpener()

actual fun createAppPaths(): AppPaths = JvmAppPaths()

actual fun createLocationPicker(): LocationPicker = JvmLocationPicker()

actual fun createStoragePermission(): StoragePermission = JvmStoragePermission()

actual fun currentPlatformKind(): PlatformKind = PlatformKind.Desktop

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) = Unit

@Composable
actual fun PlatformFolderPickerOverlay() = Unit
