package com.github.adibfara.verse

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import java.awt.Window

private const val DwmWindowCornerPreferenceAttribute = 33
private const val DwmWindowCornerPreferenceRound = 2

private interface DwmLibrary : Library {
    fun DwmSetWindowAttribute(hwnd: Pointer, attribute: Int, preference: IntArray, size: Int): Int
}

fun Window.roundCornersOnWindows() {
    val osName = System.getProperty("os.name") ?: return
    if (!osName.contains("windows", ignoreCase = true)) return
    runCatching {
        val dwm = Native.load("dwmapi", DwmLibrary::class.java)
        dwm.DwmSetWindowAttribute(
            Native.getComponentPointer(this),
            DwmWindowCornerPreferenceAttribute,
            intArrayOf(DwmWindowCornerPreferenceRound),
            4,
        )
    }
}
