package com.github.adibfara.verse.core.platform

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
import com.github.adibfara.verse.core.designsystem.component.VerseButton
import com.github.adibfara.verse.core.designsystem.component.VerseIcon
import com.github.adibfara.verse.core.designsystem.component.VerseIconButton
import com.github.adibfara.verse.core.designsystem.component.VerseIcons
import com.github.adibfara.verse.core.designsystem.component.VerseText
import com.github.adibfara.verse.core.designsystem.theme.VerseTheme
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
    val colors = VerseTheme.colors

    val entries = remember(currentDir) {
        currentDir.listFiles { file -> file.isDirectory && !file.isHidden }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.window)) {
        Column(modifier = Modifier.fillMaxSize().safeContentPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(VerseTheme.sizes.toolbarHeight)
                    .padding(horizontal = VerseTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VerseIconButton(
                    icon = VerseIcons.ArrowLeft,
                    contentDescription = "Up",
                    enabled = currentDir.parentFile != null,
                    onClick = { currentDir.parentFile?.let { currentDir = it } },
                )
                VerseText(
                    text = currentDir.absolutePath,
                    style = VerseTheme.typography.uiLabel,
                    color = colors.textSecondary,
                    maxLines = 1,
                    modifier = Modifier.weight(1f).padding(horizontal = VerseTheme.spacing.sm),
                )
                VerseIconButton(
                    icon = VerseIcons.Close,
                    contentDescription = "Cancel",
                    onClick = onCancel,
                )
            }

            VerseText(
                text = title,
                style = VerseTheme.typography.uiSmall,
                color = colors.textMuted,
                modifier = Modifier.padding(horizontal = VerseTheme.spacing.sm),
            )

            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                items(items = entries, key = { it.absolutePath }) { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { currentDir = entry }
                            .padding(horizontal = VerseTheme.spacing.lg, vertical = VerseTheme.spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        VerseIcon(icon = VerseIcons.Folder, contentDescription = null)
                        VerseText(
                            text = entry.name,
                            style = VerseTheme.typography.uiBody,
                            color = colors.textPrimary,
                            maxLines = 1,
                            modifier = Modifier.weight(1f).padding(start = VerseTheme.spacing.sm),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(VerseTheme.spacing.md),
                horizontalArrangement = Arrangement.End,
            ) {
                VerseButton(
                    label = "Use this folder",
                    onClick = { onPick(currentDir.absolutePath) },
                    primary = true,
                )
            }
        }
    }
}
