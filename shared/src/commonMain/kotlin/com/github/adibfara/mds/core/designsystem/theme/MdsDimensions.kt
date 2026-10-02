package com.github.adibfara.mds.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class MdsSpacing(
    val none: Dp = 0.dp,
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
)

@Immutable
data class MdsShapes(
    val small: Shape = RoundedCornerShape(4.dp),
    val medium: Shape = RoundedCornerShape(6.dp),
    val large: Shape = RoundedCornerShape(10.dp),
    val round: Shape = RoundedCornerShape(50),
)

@Immutable
data class MdsSizes(
    val iconSmall: Dp = 14.dp,
    val icon: Dp = 16.dp,
    val iconLarge: Dp = 20.dp,
    val iconButton: Dp = 26.dp,
    val rowHeight: Dp = 26.dp,
    val tabHeight: Dp = 32.dp,
    val toolbarHeight: Dp = 38.dp,
    val statusBarHeight: Dp = 24.dp,
    val sidebarMinWidth: Dp = 160.dp,
    val sidebarMaxWidth: Dp = 520.dp,
    val sidebarDefaultWidth: Dp = 260.dp,
    val editorMaxWidth: Dp = 780.dp,
    val splitterWidth: Dp = 6.dp,
    val borderWidth: Dp = 1.dp,
    val indentStep: Dp = 14.dp,
)
