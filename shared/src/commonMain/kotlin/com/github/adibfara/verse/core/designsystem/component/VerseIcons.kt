package com.github.adibfara.verse.core.designsystem.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private fun strokeIcon(name: String, build: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.9f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = build,
        )
    }.build()

private fun filledIcon(name: String, build: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = SolidColor(Color.Black),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.9f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = build,
        )
    }.build()

private fun PathBuilder.segment(fromX: Float, fromY: Float, toX: Float, toY: Float) {
    moveTo(fromX, fromY)
    lineTo(toX, toY)
}

private fun PathBuilder.circle(centerX: Float, centerY: Float, radius: Float) {
    val control = radius * 0.5523f
    moveTo(centerX + radius, centerY)
    curveTo(
        centerX + radius, centerY + control,
        centerX + control, centerY + radius,
        centerX, centerY + radius,
    )
    curveTo(
        centerX - control, centerY + radius,
        centerX - radius, centerY + control,
        centerX - radius, centerY,
    )
    curveTo(
        centerX - radius, centerY - control,
        centerX - control, centerY - radius,
        centerX, centerY - radius,
    )
    curveTo(
        centerX + control, centerY - radius,
        centerX + radius, centerY - control,
        centerX + radius, centerY,
    )
    close()
}

private fun PathBuilder.dot(centerX: Float, centerY: Float) {
    moveTo(centerX, centerY)
    lineTo(centerX, centerY + 0.05f)
}

private fun PathBuilder.documentOutline() {
    moveTo(6f, 3f)
    lineTo(14f, 3f)
    lineTo(19f, 8f)
    lineTo(19f, 21f)
    lineTo(6f, 21f)
    close()
    moveTo(14f, 3f)
    lineTo(14f, 8f)
    lineTo(19f, 8f)
}

private fun PathBuilder.folderOutline() {
    moveTo(3f, 6f)
    lineTo(9f, 6f)
    lineTo(11f, 9f)
    lineTo(21f, 9f)
    lineTo(21f, 19f)
    lineTo(3f, 19f)
    close()
}

object VerseIcons {

    val ChevronRight: ImageVector by lazy { strokeIcon("ChevronRight") {
        moveTo(9.5f, 6f)
        lineTo(15.5f, 12f)
        lineTo(9.5f, 18f)
    } }

    val ChevronDown: ImageVector by lazy { strokeIcon("ChevronDown") {
        moveTo(6f, 9.5f)
        lineTo(12f, 15.5f)
        lineTo(18f, 9.5f)
    } }

    val Close: ImageVector by lazy { strokeIcon("Close") {
        segment(18f, 6f, 6f, 18f)
        segment(6f, 6f, 18f, 18f)
    } }

    val Plus: ImageVector by lazy { strokeIcon("Plus") {
        segment(12f, 5f, 12f, 19f)
        segment(5f, 12f, 19f, 12f)
    } }

    val Minus: ImageVector by lazy {
        strokeIcon("Minus") {
            segment(5f, 12f, 19f, 12f)
        }
    }

    val Check: ImageVector by lazy { strokeIcon("Check") {
        moveTo(20f, 6f)
        lineTo(9f, 17f)
        lineTo(4f, 12f)
    } }

    val ArrowLeft: ImageVector by lazy { strokeIcon("ArrowLeft") {
        segment(19f, 12f, 5f, 12f)
        moveTo(12f, 19f)
        lineTo(5f, 12f)
        lineTo(12f, 5f)
    } }

    val ArrowRight: ImageVector by lazy { strokeIcon("ArrowRight") {
        segment(5f, 12f, 19f, 12f)
        moveTo(12f, 5f)
        lineTo(19f, 12f)
        lineTo(12f, 19f)
    } }

    val Folder: ImageVector by lazy { strokeIcon("Folder") {
        folderOutline()
    } }

    val FolderPlus: ImageVector by lazy { strokeIcon("FolderPlus") {
        folderOutline()
        segment(12f, 11f, 12f, 17f)
        segment(9f, 14f, 15f, 14f)
    } }

    val FileText: ImageVector by lazy { strokeIcon("FileText") {
        documentOutline()
        segment(9f, 13f, 16f, 13f)
        segment(9f, 17f, 16f, 17f)
    } }

    val FilePlus: ImageVector by lazy { strokeIcon("FilePlus") {
        documentOutline()
        segment(12.5f, 12f, 12.5f, 18f)
        segment(9.5f, 15f, 15.5f, 15f)
    } }

    val Image: ImageVector by lazy { strokeIcon("Image") {
        moveTo(3f, 5f)
        lineTo(21f, 5f)
        lineTo(21f, 19f)
        lineTo(3f, 19f)
        close()
        circle(8.5f, 9.5f, 1.6f)
        moveTo(4f, 18f)
        lineTo(10f, 12f)
        lineTo(14f, 16f)
        lineTo(17f, 13f)
        lineTo(20.5f, 16.5f)
    } }

    val Search: ImageVector by lazy { strokeIcon("Search") {
        circle(11f, 11f, 6f)
        segment(15.5f, 15.5f, 20.5f, 20.5f)
    } }

    val SortLines: ImageVector by lazy { strokeIcon("SortLines") {
        segment(4f, 7f, 10f, 7f)
        segment(4f, 12f, 14f, 12f)
        segment(4f, 17f, 19f, 17f)
    } }

    val CollapseAll: ImageVector by lazy { strokeIcon("CollapseAll") {
        moveTo(7f, 10f)
        lineTo(12f, 5f)
        lineTo(17f, 10f)
        moveTo(7f, 14f)
        lineTo(12f, 19f)
        lineTo(17f, 14f)
    } }

    val PanelLeft: ImageVector by lazy { strokeIcon("PanelLeft") {
        moveTo(3f, 4.5f)
        lineTo(21f, 4.5f)
        lineTo(21f, 19.5f)
        lineTo(3f, 19.5f)
        close()
        segment(9.5f, 4.5f, 9.5f, 19.5f)
    } }

    val Sliders: ImageVector by lazy { strokeIcon("Sliders") {
        segment(4f, 21f, 4f, 14f)
        segment(4f, 10f, 4f, 3f)
        segment(12f, 21f, 12f, 12f)
        segment(12f, 8f, 12f, 3f)
        segment(20f, 21f, 20f, 16f)
        segment(20f, 12f, 20f, 3f)
        segment(1.5f, 14f, 6.5f, 14f)
        segment(9.5f, 8f, 14.5f, 8f)
        segment(17.5f, 16f, 22.5f, 16f)
    } }

    val MoreVertical: ImageVector by lazy { strokeIcon("MoreVertical") {
        dot(12f, 5.5f)
        dot(12f, 12f)
        dot(12f, 18.5f)
    } }

    val Eye: ImageVector by lazy { strokeIcon("Eye") {
        moveTo(2.5f, 12f)
        curveTo(5.5f, 6.5f, 9f, 4.5f, 12f, 4.5f)
        curveTo(15f, 4.5f, 18.5f, 6.5f, 21.5f, 12f)
        curveTo(18.5f, 17.5f, 15f, 19.5f, 12f, 19.5f)
        curveTo(9f, 19.5f, 5.5f, 17.5f, 2.5f, 12f)
        close()
        circle(12f, 12f, 3f)
    } }

    val Pencil: ImageVector by lazy { strokeIcon("Pencil") {
        moveTo(3f, 21f)
        lineTo(4f, 16f)
        lineTo(16f, 4f)
        lineTo(20f, 8f)
        lineTo(8f, 20f)
        close()
        segment(14f, 6f, 18f, 10f)
    } }

    val Code: ImageVector by lazy { strokeIcon("Code") {
        moveTo(8f, 18f)
        lineTo(3f, 12f)
        lineTo(8f, 6f)
        moveTo(16f, 6f)
        lineTo(21f, 12f)
        lineTo(16f, 18f)
    } }

    val Trash: ImageVector by lazy { strokeIcon("Trash") {
        segment(3.5f, 6.5f, 20.5f, 6.5f)
        moveTo(8.5f, 6.5f)
        lineTo(8.5f, 4f)
        lineTo(15.5f, 4f)
        lineTo(15.5f, 6.5f)
        moveTo(5.5f, 6.5f)
        lineTo(5.5f, 20f)
        lineTo(18.5f, 20f)
        lineTo(18.5f, 6.5f)
        segment(10f, 10.5f, 10f, 16.5f)
        segment(14f, 10.5f, 14f, 16.5f)
    } }

    val Copy: ImageVector by lazy { strokeIcon("Copy") {
        moveTo(8.5f, 8.5f)
        lineTo(20f, 8.5f)
        lineTo(20f, 20f)
        lineTo(8.5f, 20f)
        close()
        moveTo(15.5f, 8.5f)
        lineTo(15.5f, 4f)
        lineTo(4f, 4f)
        lineTo(4f, 15.5f)
        lineTo(8.5f, 15.5f)
    } }

    val ExternalLink: ImageVector by lazy { strokeIcon("ExternalLink") {
        moveTo(14.5f, 3.5f)
        lineTo(20.5f, 3.5f)
        lineTo(20.5f, 9.5f)
        segment(10f, 14f, 20.5f, 3.5f)
        moveTo(18f, 13f)
        lineTo(18f, 20.5f)
        lineTo(3.5f, 20.5f)
        lineTo(3.5f, 6f)
        lineTo(11f, 6f)
    } }

    val MoveTo: ImageVector by lazy { strokeIcon("MoveTo") {
        folderOutline()
        segment(9f, 14f, 16f, 14f)
        moveTo(13f, 11f)
        lineTo(16f, 14f)
        lineTo(13f, 17f)
    } }

    val Sun: ImageVector by lazy { strokeIcon("Sun") {
        circle(12f, 12f, 4f)
        segment(12f, 2f, 12f, 4.5f)
        segment(12f, 19.5f, 12f, 22f)
        segment(2f, 12f, 4.5f, 12f)
        segment(19.5f, 12f, 22f, 12f)
        segment(5f, 5f, 6.8f, 6.8f)
        segment(17.2f, 17.2f, 19f, 19f)
        segment(19f, 5f, 17.2f, 6.8f)
        segment(6.8f, 17.2f, 5f, 19f)
    } }

    val Moon: ImageVector by lazy { strokeIcon("Moon") {
        moveTo(20f, 14.5f)
        curveTo(19f, 18.5f, 15.5f, 21f, 12f, 21f)
        curveTo(7f, 21f, 3f, 17f, 3f, 12f)
        curveTo(3f, 8f, 5.5f, 4.6f, 9.5f, 3.5f)
        curveTo(8.5f, 6f, 8.5f, 9f, 10.5f, 11.5f)
        curveTo(12.5f, 14f, 17f, 15.5f, 20f, 14.5f)
        close()
    } }

    val WindowMinimize: ImageVector by lazy { strokeIcon("WindowMinimize") {
        segment(5f, 12f, 19f, 12f)
    } }

    val WindowMaximize: ImageVector by lazy { strokeIcon("WindowMaximize") {
        moveTo(5f, 5f)
        lineTo(19f, 5f)
        lineTo(19f, 19f)
        lineTo(5f, 19f)
        close()
    } }

    val WindowRestore: ImageVector by lazy { strokeIcon("WindowRestore") {
        moveTo(4f, 8f)
        lineTo(16f, 8f)
        lineTo(16f, 20f)
        lineTo(4f, 20f)
        close()
        moveTo(8f, 8f)
        lineTo(8f, 4f)
        lineTo(20f, 4f)
        lineTo(20f, 16f)
        lineTo(16f, 16f)
    } }

    val Home: ImageVector by lazy { strokeIcon("Home") {
        moveTo(3.5f, 11f)
        lineTo(12f, 3.5f)
        lineTo(20.5f, 11f)
        moveTo(5.5f, 9.5f)
        lineTo(5.5f, 20f)
        lineTo(18.5f, 20f)
        lineTo(18.5f, 9.5f)
    } }

    val Menu: ImageVector by lazy { strokeIcon("Menu") {
        segment(4f, 6.5f, 20f, 6.5f)
        segment(4f, 12f, 20f, 12f)
        segment(4f, 17.5f, 20f, 17.5f)
    } }

    val Layers: ImageVector by lazy { strokeIcon("Layers") {
        moveTo(12f, 3.5f)
        lineTo(20.5f, 8f)
        lineTo(12f, 12.5f)
        lineTo(3.5f, 8f)
        close()
        moveTo(3.5f, 12.5f)
        lineTo(12f, 17f)
        lineTo(20.5f, 12.5f)
        moveTo(3.5f, 17f)
        lineTo(12f, 21.5f)
        lineTo(20.5f, 17f)
    } }

    val Heart: ImageVector by lazy { strokeIcon("Heart") {
        moveTo(12f, 20f)
        lineTo(4.5f, 12.6f)
        curveTo(2.6f, 10.7f, 2.6f, 7.6f, 4.5f, 5.8f)
        curveTo(6.3f, 4f, 9.2f, 4f, 11f, 5.8f)
        lineTo(12f, 6.8f)
        lineTo(13f, 5.8f)
        curveTo(14.8f, 4f, 17.7f, 4f, 19.5f, 5.8f)
        curveTo(21.4f, 7.6f, 21.4f, 10.7f, 19.5f, 12.6f)
        close()
    } }

    val HeartFilled: ImageVector by lazy { filledIcon("HeartFilled") {
        moveTo(12f, 20f)
        lineTo(4.5f, 12.6f)
        curveTo(2.6f, 10.7f, 2.6f, 7.6f, 4.5f, 5.8f)
        curveTo(6.3f, 4f, 9.2f, 4f, 11f, 5.8f)
        lineTo(12f, 6.8f)
        lineTo(13f, 5.8f)
        curveTo(14.8f, 4f, 17.7f, 4f, 19.5f, 5.8f)
        curveTo(21.4f, 7.6f, 21.4f, 10.7f, 19.5f, 12.6f)
        close()
    } }

    val Lock: ImageVector by lazy { strokeIcon("Lock") {
        moveTo(8f, 11f)
        lineTo(8f, 8f)
        curveTo(8f, 5.8f, 9.8f, 4f, 12f, 4f)
        curveTo(14.2f, 4f, 16f, 5.8f, 16f, 8f)
        lineTo(16f, 11f)
        moveTo(5f, 11f)
        lineTo(19f, 11f)
        lineTo(19f, 20f)
        lineTo(5f, 20f)
        close()
    } }

    val LockOpen: ImageVector by lazy { strokeIcon("LockOpen") {
        moveTo(8f, 11f)
        lineTo(8f, 7f)
        curveTo(8f, 4.8f, 9.8f, 3f, 12f, 3f)
        curveTo(13.9f, 3f, 15.4f, 4.1f, 16f, 5.6f)
        moveTo(5f, 11f)
        lineTo(19f, 11f)
        lineTo(19f, 20f)
        lineTo(5f, 20f)
        close()
    } }

    val Link: ImageVector by lazy { strokeIcon("Link") {
        moveTo(10.6f, 13.4f)
        curveTo(11.6f, 14.4f, 13.2f, 14.4f, 14.2f, 13.4f)
        lineTo(17.8f, 9.8f)
        curveTo(18.8f, 8.8f, 18.8f, 7.2f, 17.8f, 6.2f)
        curveTo(16.8f, 5.2f, 15.2f, 5.2f, 14.2f, 6.2f)
        lineTo(13.2f, 7.2f)
        moveTo(13.4f, 10.6f)
        curveTo(12.4f, 9.6f, 10.8f, 9.6f, 9.8f, 10.6f)
        lineTo(6.2f, 14.2f)
        curveTo(5.2f, 15.2f, 5.2f, 16.8f, 6.2f, 17.8f)
        curveTo(7.2f, 18.8f, 8.8f, 18.8f, 9.8f, 17.8f)
        lineTo(10.8f, 16.8f)
    } }

    val Columns: ImageVector by lazy { strokeIcon("Columns") {
        moveTo(4f, 4f)
        lineTo(20f, 4f)
        lineTo(20f, 20f)
        lineTo(4f, 20f)
        close()
        segment(12f, 4f, 12f, 20f)
    } }

    val Target: ImageVector by lazy { strokeIcon("Target") {
        circle(12f, 12f, 8.5f)
        circle(12f, 12f, 4f)
        dot(12f, 12f)
    } }

    val ListOrdered: ImageVector by lazy { strokeIcon("ListOrdered") {
        segment(10f, 6f, 20f, 6f)
        segment(10f, 12f, 20f, 12f)
        segment(10f, 18f, 20f, 18f)
        moveTo(4.5f, 4.5f)
        lineTo(4.5f, 7.5f)
        moveTo(4f, 4.5f)
        lineTo(5f, 4.5f)
        moveTo(4f, 10.5f)
        lineTo(5.3f, 10.5f)
        curveTo(5.3f, 10.5f, 4.2f, 11.6f, 4f, 12f)
        lineTo(5.3f, 12f)
    } }

    val Share: ImageVector by lazy { strokeIcon("Share") {
        circle(18f, 5f, 2.5f)
        circle(6f, 12f, 2.5f)
        circle(18f, 19f, 2.5f)
        segment(8.2f, 10.8f, 15.8f, 6.2f)
        segment(8.2f, 13.2f, 15.8f, 17.8f)
    } }

    val TextFormat: ImageVector by lazy { strokeIcon("TextFormat") {
        moveTo(6f, 5f)
        lineTo(6f, 19f)
        moveTo(6f, 5f)
        lineTo(10.5f, 5f)
        curveTo(12.5f, 5f, 12.5f, 11.5f, 10.5f, 11.5f)
        lineTo(6f, 11.5f)
        moveTo(6f, 11.5f)
        lineTo(11.5f, 11.5f)
        curveTo(13.8f, 11.5f, 13.8f, 19f, 11.5f, 19f)
        lineTo(6f, 19f)
    } }

    val Mic: ImageVector by lazy { strokeIcon("Mic") {
        moveTo(9.5f, 5.5f)
        curveTo(9.5f, 3.6f, 10.6f, 2.5f, 12f, 2.5f)
        curveTo(13.4f, 2.5f, 14.5f, 3.6f, 14.5f, 5.5f)
        lineTo(14.5f, 11.5f)
        curveTo(14.5f, 13.4f, 13.4f, 14.5f, 12f, 14.5f)
        curveTo(10.6f, 14.5f, 9.5f, 13.4f, 9.5f, 11.5f)
        close()
        moveTo(6f, 11f)
        curveTo(6f, 14.9f, 8.7f, 18f, 12f, 18f)
        curveTo(15.3f, 18f, 18f, 14.9f, 18f, 11f)
        segment(12f, 18f, 12f, 21.5f)
        segment(8.5f, 21.5f, 15.5f, 21.5f)
    } }
}
