package com.github.adibfara.mds.core.platform

import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.github.adibfara.mds.core.designsystem.component.MdsButton
import com.github.adibfara.mds.core.designsystem.component.MdsIcon
import com.github.adibfara.mds.core.designsystem.component.MdsIconButton
import com.github.adibfara.mds.core.designsystem.component.MdsIcons
import com.github.adibfara.mds.core.designsystem.component.MdsText
import com.github.adibfara.mds.core.designsystem.theme.MdsTheme
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal object AndroidFolderPickerBridge {

    data class Request(val title: String)

    private val mutableRequest = MutableStateFlow<Request?>(null)
    val request: StateFlow<Request?> = mutableRequest.asStateFlow()

    private var pending: CompletableDeferred<String?>? = null

    suspend fun pick(title: String): String? {
        val deferred = CompletableDeferred<String?>()
        pending = deferred
        mutableRequest.value = Request(title)
        return deferred.await()
    }

    fun complete(path: String?) {
        pending?.complete(path)
        pending = null
        mutableRequest.value = null
    }
}

internal class AndroidLocationPicker : LocationPicker {
    override suspend fun pickDirectory(title: String): String? = AndroidFolderPickerBridge.pick(title)
    override suspend fun pickFile(title: String): String? = null
}

private fun defaultStartDirectory(): File {
    val external = runCatching { Environment.getExternalStorageDirectory() }.getOrNull()
    return if (external != null && external.isDirectory && external.canRead()) {
        external
    } else {
        AndroidPlatform.require().filesDir
    }
}

@Composable
internal fun AndroidFolderPickerOverlay() {
    val request by AndroidFolderPickerBridge.request.collectAsState()
    val active = request
    if (active != null) {
        FolderBrowserScreen(
            title = active.title,
            onPick = { AndroidFolderPickerBridge.complete(it) },
            onCancel = { AndroidFolderPickerBridge.complete(null) },
        )
    }
}

@Composable
private fun FolderBrowserScreen(
    title: String,
    onPick: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var currentDir by remember { mutableStateOf(defaultStartDirectory()) }
    val colors = MdsTheme.colors

    val entries = remember(currentDir) {
        currentDir.listFiles { file -> file.isDirectory && !file.isHidden }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.window)) {
        Column(modifier = Modifier.fillMaxSize().safeContentPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(MdsTheme.sizes.toolbarHeight)
                    .padding(horizontal = MdsTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MdsIconButton(
                    icon = MdsIcons.ArrowLeft,
                    contentDescription = "Up",
                    enabled = currentDir.parentFile != null,
                    onClick = { currentDir.parentFile?.let { currentDir = it } },
                )
                MdsText(
                    text = currentDir.absolutePath,
                    style = MdsTheme.typography.uiLabel,
                    color = colors.textSecondary,
                    maxLines = 1,
                    modifier = Modifier.weight(1f).padding(horizontal = MdsTheme.spacing.sm),
                )
                MdsIconButton(
                    icon = MdsIcons.Close,
                    contentDescription = "Cancel",
                    onClick = onCancel,
                )
            }

            MdsText(
                text = title,
                style = MdsTheme.typography.uiSmall,
                color = colors.textMuted,
                modifier = Modifier.padding(horizontal = MdsTheme.spacing.sm),
            )

            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                items(items = entries, key = { it.absolutePath }) { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { currentDir = entry }
                            .padding(horizontal = MdsTheme.spacing.lg, vertical = MdsTheme.spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MdsIcon(icon = MdsIcons.Folder, contentDescription = null)
                        MdsText(
                            text = entry.name,
                            style = MdsTheme.typography.uiBody,
                            color = colors.textPrimary,
                            maxLines = 1,
                            modifier = Modifier.weight(1f).padding(start = MdsTheme.spacing.sm),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(MdsTheme.spacing.md),
                horizontalArrangement = Arrangement.End,
            ) {
                MdsButton(
                    label = "Use this folder",
                    onClick = { onPick(currentDir.absolutePath) },
                    primary = true,
                )
            }
        }
    }
}
