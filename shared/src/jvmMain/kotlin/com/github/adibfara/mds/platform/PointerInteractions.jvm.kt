package com.github.adibfara.mds.core.designsystem.component

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput

@OptIn(ExperimentalComposeUiApi::class)
actual fun Modifier.onSecondaryClick(key: Any?, onClick: (Offset) -> Unit): Modifier =
    pointerInput(key) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Press &&
                    event.button == PointerButton.Secondary
                ) {
                    val position = event.changes.firstOrNull()?.position
                    event.changes.forEach { it.consume() }
                    position?.let(onClick)
                }
            }
        }
    }

@OptIn(ExperimentalComposeUiApi::class)
actual fun Modifier.onTertiaryClick(key: Any?, onClick: () -> Unit): Modifier =
    pointerInput(key) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Press &&
                    event.button == PointerButton.Tertiary
                ) {
                    event.changes.forEach { it.consume() }
                    onClick()
                }
            }
        }
    }
