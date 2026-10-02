package com.github.adibfara.mds.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.github.adibfara.mds.core.designsystem.component.MdsIcon
import com.github.adibfara.mds.core.designsystem.component.MdsIcons
import com.github.adibfara.mds.core.designsystem.component.MdsText
import com.github.adibfara.mds.core.designsystem.theme.MdsTheme

@Composable
fun MobileBottomBar(
    tabCount: Int,
    onAdd: () -> Unit,
    onTabs: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MdsTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(MdsTheme.sizes.bottomBarHeight)
            .background(colors.sidebar),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BottomBarAction(
            icon = MdsIcons.Plus,
            label = "Add",
            onClick = onAdd,
        )
        BottomBarAction(
            icon = MdsIcons.Layers,
            label = if (tabCount > 0) "Tabs ($tabCount)" else "Tabs",
            onClick = onTabs,
        )
    }
}

@Composable
private fun BottomBarAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val colors = MdsTheme.colors
    Box(
        modifier = Modifier
            .size(MdsTheme.sizes.touchTarget)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(MdsTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MdsIcon(icon = icon, contentDescription = label, tint = colors.textSecondary)
            MdsText(text = label, style = MdsTheme.typography.uiLabel, color = colors.textSecondary)
        }
    }
}
