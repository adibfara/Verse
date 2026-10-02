package com.github.adibfara.mds.core.designsystem.theme

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

val LocalMdsColors: ProvidableCompositionLocal<MdsColors> =
    staticCompositionLocalOf { LightColors }

val LocalMdsTypography: ProvidableCompositionLocal<MdsTypography> =
    staticCompositionLocalOf { mdsTypography() }

val LocalMdsSpacing: ProvidableCompositionLocal<MdsSpacing> =
    staticCompositionLocalOf { MdsSpacing() }

val LocalMdsShapes: ProvidableCompositionLocal<MdsShapes> =
    staticCompositionLocalOf { MdsShapes() }

val LocalMdsSizes: ProvidableCompositionLocal<MdsSizes> =
    staticCompositionLocalOf { MdsSizes() }

object MdsTheme {
    val colors: MdsColors
        @Composable get() = LocalMdsColors.current

    val typography: MdsTypography
        @Composable get() = LocalMdsTypography.current

    val spacing: MdsSpacing
        @Composable get() = LocalMdsSpacing.current

    val shapes: MdsShapes
        @Composable get() = LocalMdsShapes.current

    val sizes: MdsSizes
        @Composable get() = LocalMdsSizes.current
}

@Composable
fun resolveTheme(mode: ThemeMode): ThemeDefinition = when (mode) {
    ThemeMode.Light -> LightThemeDefinition
    ThemeMode.Dark -> DarkThemeDefinition
    ThemeMode.System -> if (isSystemInDarkTheme()) DarkThemeDefinition else LightThemeDefinition
}

@Composable
fun MdsTheme(
    theme: ThemeDefinition,
    editorFontSize: Float = 15.5f,
    content: @Composable () -> Unit,
) {
    val colors = theme.colors
    val typography = remember(editorFontSize) { mdsTypography(editorFontSize) }
    val spacing = remember { MdsSpacing() }
    val shapes = remember { MdsShapes() }
    val sizes = remember { MdsSizes() }

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
        LocalMdsColors provides colors,
        LocalMdsTypography provides typography,
        LocalMdsSpacing provides spacing,
        LocalMdsShapes provides shapes,
        LocalMdsSizes provides sizes,
        LocalContentColor provides colors.textPrimary,
        LocalTextSelectionColors provides selectionColors,
    ) {
        MaterialTheme(colorScheme = materialScheme, content = content)
    }
}
