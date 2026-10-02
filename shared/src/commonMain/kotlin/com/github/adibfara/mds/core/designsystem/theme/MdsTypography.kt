package com.github.adibfara.mds.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

@Immutable
data class MdsTypography(
    val uiTitle: TextStyle,
    val uiBody: TextStyle,
    val uiLabel: TextStyle,
    val uiSmall: TextStyle,
    val tabTitle: TextStyle,
    val treeItem: TextStyle,
    val editorBody: TextStyle,
    val editorHeading1: TextStyle,
    val editorHeading2: TextStyle,
    val editorHeading3: TextStyle,
    val editorHeading4: TextStyle,
    val editorHeading5: TextStyle,
    val editorHeading6: TextStyle,
    val editorCode: TextStyle,
    val editorRaw: TextStyle,
) {
    fun headingFor(level: Int): TextStyle = when (level) {
        1 -> editorHeading1
        2 -> editorHeading2
        3 -> editorHeading3
        4 -> editorHeading4
        5 -> editorHeading5
        else -> editorHeading6
    }
}

fun mdsTypography(editorFontSize: Float = 15.5f): MdsTypography {
    val body = editorFontSize.sp
    return MdsTypography(
        uiTitle = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        uiBody = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Normal,
        ),
        uiLabel = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
        ),
        uiSmall = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Normal,
        ),
        tabTitle = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        ),
        treeItem = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Normal,
        ),
        editorBody = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontSize = body,
            lineHeight = 1.65.em,
        ),
        editorHeading1 = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontSize = body * 2.0f,
            fontWeight = FontWeight.Bold,
            lineHeight = 1.3.em,
        ),
        editorHeading2 = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontSize = body * 1.6f,
            fontWeight = FontWeight.Bold,
            lineHeight = 1.3.em,
        ),
        editorHeading3 = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontSize = body * 1.35f,
            fontWeight = FontWeight.Bold,
            lineHeight = 1.35.em,
        ),
        editorHeading4 = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontSize = body * 1.18f,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 1.4.em,
        ),
        editorHeading5 = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontSize = body * 1.08f,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 1.45.em,
        ),
        editorHeading6 = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontSize = body,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 1.5.em,
        ),
        editorCode = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = body * 0.92f,
            lineHeight = 1.5.em,
        ),
        editorRaw = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = body * 0.95f,
            lineHeight = 1.6.em,
        ),
    )
}
