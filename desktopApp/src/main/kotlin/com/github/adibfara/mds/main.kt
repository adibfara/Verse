package com.github.adibfara.mds

import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.github.adibfara.mds.app.AppRoot
import com.github.adibfara.mds.app.LaunchArguments
import com.github.adibfara.mds.app.WindowChrome
import com.github.adibfara.mds.app.di.externalOpenRequests
import com.github.adibfara.mds.app.di.initializeDependencies
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDropEvent
import java.awt.dnd.DropTargetAdapter
import java.io.File

fun main(args: Array<String>) {
    initializeDependencies()

    val openRequests = externalOpenRequests()

    val initialFile = args.firstOrNull()
        ?.let(::File)
        ?.takeIf { it.exists() }
        ?.absolutePath

    application {
        val windowState = rememberWindowState(width = 1280.dp, height = 820.dp)
        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = "MDs",
            undecorated = true,
            resizable = true,
        ) {
            val isMaximized = windowState.placement == WindowPlacement.Maximized
            val chrome = remember(isMaximized) {
                WindowChrome(
                    isMaximized = isMaximized,
                    onMinimize = { windowState.isMinimized = true },
                    onToggleMaximize = {
                        windowState.placement = if (isMaximized) {
                            WindowPlacement.Floating
                        } else {
                            WindowPlacement.Maximized
                        }
                    },
                    onClose = ::exitApplication,
                    draggableArea = { content -> WindowDraggableArea { content() } },
                )
            }

            LaunchedEffect(window) {
                window.dropTarget = DropTarget(
                    window,
                    DnDConstants.ACTION_COPY,
                    object : DropTargetAdapter() {
                        override fun drop(event: DropTargetDropEvent) {
                            runCatching {
                                event.acceptDrop(DnDConstants.ACTION_COPY)
                                val dropped = event.transferable
                                    .getTransferData(DataFlavor.javaFileListFlavor)
                                @Suppress("UNCHECKED_CAST")
                                (dropped as? List<File>)
                                    ?.filter { it.isFile }
                                    ?.forEach { openRequests.request(it.absolutePath) }
                                event.dropComplete(true)
                            }.onFailure { event.dropComplete(false) }
                        }
                    },
                )
            }

            AppRoot(
                launchArguments = LaunchArguments(initialFilePath = initialFile),
                windowChrome = chrome,
            )
        }
    }
}
