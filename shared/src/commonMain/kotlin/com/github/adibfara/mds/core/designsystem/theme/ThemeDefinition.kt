package com.github.adibfara.mds.core.designsystem.theme

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Serializable
enum class ThemeMode {
    Light,
    Dark,
    System;

    val label: String
        get() = when (this) {
            Light -> "Light"
            Dark -> "Dark"
            System -> "Match system"
        }
}

@Immutable
data class ThemeDefinition(
    val id: String,
    val name: String,
    val isDark: Boolean,
    val colors: MdsColors,
)

val LightThemeDefinition = ThemeDefinition(
    id = "mds-light",
    name = "Cream",
    isDark = false,
    colors = LightColors,
)

val DarkThemeDefinition = ThemeDefinition(
    id = "mds-dark",
    name = "Charcoal",
    isDark = true,
    colors = DarkColors,
)

val BuiltInThemes = listOf(LightThemeDefinition, DarkThemeDefinition)
