package com.github.adibfara.verse.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

val LocalVerseColors: ProvidableCompositionLocal<VerseColors> =
    staticCompositionLocalOf { LightColors }

val LocalVerseTypography: ProvidableCompositionLocal<VerseTypography> =
    staticCompositionLocalOf { VerseTypography() }

val LocalVerseSpacing: ProvidableCompositionLocal<VerseSpacing> =
    staticCompositionLocalOf { VerseSpacing() }

val LocalVerseShapes: ProvidableCompositionLocal<VerseShapes> =
    staticCompositionLocalOf { VerseShapes() }

val LocalVerseSizes: ProvidableCompositionLocal<VerseSizes> =
    staticCompositionLocalOf { VerseSizes() }

object VerseTheme {
    val colors: VerseColors
        @Composable get() = LocalVerseColors.current

    val typography: VerseTypography
        @Composable get() = LocalVerseTypography.current

    val spacing: VerseSpacing
        @Composable get() = LocalVerseSpacing.current

    val shapes: VerseShapes
        @Composable get() = LocalVerseShapes.current

    val sizes: VerseSizes
        @Composable get() = LocalVerseSizes.current
}

@Composable
fun resolveTheme(mode: ThemeMode): ThemeDefinition = when (mode) {
    ThemeMode.Light -> LightThemeDefinition
    ThemeMode.Dark -> DarkThemeDefinition
    ThemeMode.System -> if (isSystemInDarkTheme()) DarkThemeDefinition else LightThemeDefinition
}

@Composable
fun VerseTheme(
    theme: ThemeDefinition,
    editorFontSize: Float = 16.5f,
    uiScale: Float = 1f,
    spacing: VerseSpacing = VerseSpacing(),
    sizes: VerseSizes = VerseSizes(),
    content: @Composable () -> Unit,
) {
    val colors = theme.colors
    val typography = remember(editorFontSize, uiScale) { VerseTypography(editorFontSize, uiScale) }
    val shapes = remember { VerseShapes() }

    val materialScheme = remember(colors, theme.isDark) {
        val base = if (theme.isDark) darkColorScheme() else lightColorScheme()
        base.copy(
            primary = colors.accent,
            onPrimary = colors.textOnAccent,
            background = colors.window,
            onBackground = colors.textPrimary,
            surface = colors.surface,
            onSurface = colors.textPrimary,
            surfaceVariant = colors.surfaceHover,
            onSurfaceVariant = colors.textSecondary,
            outline = colors.border,
            outlineVariant = colors.border,
            error = colors.danger,
        )
    }

    val selectionColors = remember(colors) {
        TextSelectionColors(
            handleColor = colors.accent,
            backgroundColor = colors.selectionBackground,
        )
    }

    CompositionLocalProvider(
        LocalVerseColors provides colors,
        LocalVerseTypography provides typography,
        LocalVerseSpacing provides spacing,
        LocalVerseShapes provides shapes,
        LocalVerseSizes provides sizes,
        LocalContentColor provides colors.textPrimary,
        LocalTextSelectionColors provides selectionColors,
    ) {
        MaterialTheme(colorScheme = materialScheme, content = content)
    }
}
