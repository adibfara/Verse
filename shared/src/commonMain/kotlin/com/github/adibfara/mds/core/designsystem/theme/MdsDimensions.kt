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
    val window: Shape = RoundedCornerShape(12.dp),
)

@Immutable
data class MdsSizes(
    val iconSmall: Dp = 14.dp,
    val icon: Dp = 16.dp,
    val iconLarge: Dp = 20.dp,
    val iconButton: Dp = 26.dp,
    val iconButtonSmall: Dp = 22.dp,
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
    val touchTarget: Dp = 48.dp,
    val bottomBarHeight: Dp = 56.dp,
    val mobileTopBarHeight: Dp = 56.dp,
    val editorBottomPadding: Dp = 160.dp,
    val floatingBarHeight: Dp = 44.dp,
    val floatingBarInset: Dp = 10.dp,
    val badgeMinWidth: Dp = 24.dp,
    val dialogMinWidth: Dp = 380.dp,
    val dialogMaxWidth: Dp = 560.dp,
    val searchPanelWidth: Dp = 720.dp,
    val searchResultsHeight: Dp = 420.dp,
)

const val MobileUiScale: Float = 1.2f

fun mobileSpacing(): MdsSpacing = MdsSpacing(
    xxs = 3.dp,
    xs = 6.dp,
    sm = 10.dp,
    md = 14.dp,
    lg = 18.dp,
    xl = 24.dp,
    xxl = 32.dp,
)

fun mobileSizes(): MdsSizes = MdsSizes(
    iconSmall = 16.dp,
    icon = 20.dp,
    iconLarge = 24.dp,
    iconButton = 40.dp,
    iconButtonSmall = 34.dp,
    rowHeight = 44.dp,
    tabHeight = 40.dp,
    toolbarHeight = 52.dp,
    statusBarHeight = 30.dp,
    indentStep = 18.dp,
    bottomBarHeight = 56.dp,
    mobileTopBarHeight = 64.dp,
    editorBottomPadding = 120.dp,
    floatingBarHeight = 52.dp,
    floatingBarInset = 12.dp,
    badgeMinWidth = 28.dp,
    dialogMinWidth = 260.dp,
)
