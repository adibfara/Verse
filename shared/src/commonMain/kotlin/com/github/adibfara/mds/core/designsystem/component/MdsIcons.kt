package com.github.adibfara.mds.core.designsystem.component

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

object MdsIcons {

    val ChevronRight: ImageVector = strokeIcon("ChevronRight") {
        moveTo(9.5f, 6f)
        lineTo(15.5f, 12f)
        lineTo(9.5f, 18f)
    }

    val ChevronDown: ImageVector = strokeIcon("ChevronDown") {
        moveTo(6f, 9.5f)
        lineTo(12f, 15.5f)
        lineTo(18f, 9.5f)
    }

    val Close: ImageVector = strokeIcon("Close") {
        segment(18f, 6f, 6f, 18f)
        segment(6f, 6f, 18f, 18f)
    }

    val Plus: ImageVector = strokeIcon("Plus") {
        segment(12f, 5f, 12f, 19f)
        segment(5f, 12f, 19f, 12f)
    }

    val Check: ImageVector = strokeIcon("Check") {
        moveTo(20f, 6f)
        lineTo(9f, 17f)
        lineTo(4f, 12f)
    }

    val ArrowLeft: ImageVector = strokeIcon("ArrowLeft") {
        segment(19f, 12f, 5f, 12f)
        moveTo(12f, 19f)
        lineTo(5f, 12f)
        lineTo(12f, 5f)
    }

    val ArrowRight: ImageVector = strokeIcon("ArrowRight") {
        segment(5f, 12f, 19f, 12f)
        moveTo(12f, 5f)
        lineTo(19f, 12f)
        lineTo(12f, 19f)
    }

    val Folder: ImageVector = strokeIcon("Folder") {
        folderOutline()
    }

    val FolderPlus: ImageVector = strokeIcon("FolderPlus") {
        folderOutline()
        segment(12f, 11f, 12f, 17f)
        segment(9f, 14f, 15f, 14f)
    }

    val FileText: ImageVector = strokeIcon("FileText") {
        documentOutline()
        segment(9f, 13f, 16f, 13f)
        segment(9f, 17f, 16f, 17f)
    }

    val FilePlus: ImageVector = strokeIcon("FilePlus") {
        documentOutline()
        segment(12.5f, 12f, 12.5f, 18f)
        segment(9.5f, 15f, 15.5f, 15f)
    }

    val Image: ImageVector = strokeIcon("Image") {
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
    }

    val Search: ImageVector = strokeIcon("Search") {
        circle(11f, 11f, 6f)
        segment(15.5f, 15.5f, 20.5f, 20.5f)
    }

    val SortLines: ImageVector = strokeIcon("SortLines") {
        segment(4f, 7f, 10f, 7f)
        segment(4f, 12f, 14f, 12f)
        segment(4f, 17f, 19f, 17f)
    }

    val CollapseAll: ImageVector = strokeIcon("CollapseAll") {
        moveTo(7f, 10f)
        lineTo(12f, 5f)
        lineTo(17f, 10f)
        moveTo(7f, 14f)
        lineTo(12f, 19f)
        lineTo(17f, 14f)
    }

    val PanelLeft: ImageVector = strokeIcon("PanelLeft") {
        moveTo(3f, 4.5f)
        lineTo(21f, 4.5f)
        lineTo(21f, 19.5f)
        lineTo(3f, 19.5f)
        close()
        segment(9.5f, 4.5f, 9.5f, 19.5f)
    }

    val Sliders: ImageVector = strokeIcon("Sliders") {
        segment(4f, 21f, 4f, 14f)
        segment(4f, 10f, 4f, 3f)
        segment(12f, 21f, 12f, 12f)
        segment(12f, 8f, 12f, 3f)
        segment(20f, 21f, 20f, 16f)
        segment(20f, 12f, 20f, 3f)
        segment(1.5f, 14f, 6.5f, 14f)
        segment(9.5f, 8f, 14.5f, 8f)
        segment(17.5f, 16f, 22.5f, 16f)
    }

    val MoreVertical: ImageVector = strokeIcon("MoreVertical") {
        dot(12f, 5.5f)
        dot(12f, 12f)
        dot(12f, 18.5f)
    }

    val Eye: ImageVector = strokeIcon("Eye") {
        moveTo(2.5f, 12f)
        curveTo(5.5f, 6.5f, 9f, 4.5f, 12f, 4.5f)
        curveTo(15f, 4.5f, 18.5f, 6.5f, 21.5f, 12f)
        curveTo(18.5f, 17.5f, 15f, 19.5f, 12f, 19.5f)
        curveTo(9f, 19.5f, 5.5f, 17.5f, 2.5f, 12f)
        close()
        circle(12f, 12f, 3f)
    }

    val Pencil: ImageVector = strokeIcon("Pencil") {
        moveTo(3f, 21f)
        lineTo(4f, 16f)
        lineTo(16f, 4f)
        lineTo(20f, 8f)
        lineTo(8f, 20f)
        close()
        segment(14f, 6f, 18f, 10f)
    }

    val Code: ImageVector = strokeIcon("Code") {
        moveTo(8f, 18f)
        lineTo(3f, 12f)
        lineTo(8f, 6f)
        moveTo(16f, 6f)
        lineTo(21f, 12f)
        lineTo(16f, 18f)
    }

    val Trash: ImageVector = strokeIcon("Trash") {
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
    }

    val Copy: ImageVector = strokeIcon("Copy") {
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
    }

    val ExternalLink: ImageVector = strokeIcon("ExternalLink") {
        moveTo(14.5f, 3.5f)
        lineTo(20.5f, 3.5f)
        lineTo(20.5f, 9.5f)
        segment(10f, 14f, 20.5f, 3.5f)
        moveTo(18f, 13f)
        lineTo(18f, 20.5f)
        lineTo(3.5f, 20.5f)
        lineTo(3.5f, 6f)
        lineTo(11f, 6f)
    }

    val MoveTo: ImageVector = strokeIcon("MoveTo") {
        folderOutline()
        segment(9f, 14f, 16f, 14f)
        moveTo(13f, 11f)
        lineTo(16f, 14f)
        lineTo(13f, 17f)
    }

    val Sun: ImageVector = strokeIcon("Sun") {
        circle(12f, 12f, 4f)
        segment(12f, 2f, 12f, 4.5f)
        segment(12f, 19.5f, 12f, 22f)
        segment(2f, 12f, 4.5f, 12f)
        segment(19.5f, 12f, 22f, 12f)
        segment(5f, 5f, 6.8f, 6.8f)
        segment(17.2f, 17.2f, 19f, 19f)
        segment(19f, 5f, 17.2f, 6.8f)
        segment(6.8f, 17.2f, 5f, 19f)
    }

    val Moon: ImageVector = strokeIcon("Moon") {
        moveTo(20f, 14.5f)
        curveTo(19f, 18.5f, 15.5f, 21f, 12f, 21f)
        curveTo(7f, 21f, 3f, 17f, 3f, 12f)
        curveTo(3f, 8f, 5.5f, 4.6f, 9.5f, 3.5f)
        curveTo(8.5f, 6f, 8.5f, 9f, 10.5f, 11.5f)
        curveTo(12.5f, 14f, 17f, 15.5f, 20f, 14.5f)
        close()
    }

    val WindowMinimize: ImageVector = strokeIcon("WindowMinimize") {
        segment(5f, 12f, 19f, 12f)
    }

    val WindowMaximize: ImageVector = strokeIcon("WindowMaximize") {
        moveTo(5f, 5f)
        lineTo(19f, 5f)
        lineTo(19f, 19f)
        lineTo(5f, 19f)
        close()
    }

    val WindowRestore: ImageVector = strokeIcon("WindowRestore") {
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
    }

    val Home: ImageVector = strokeIcon("Home") {
        moveTo(3.5f, 11f)
        lineTo(12f, 3.5f)
        lineTo(20.5f, 11f)
        moveTo(5.5f, 9.5f)
        lineTo(5.5f, 20f)
        lineTo(18.5f, 20f)
        lineTo(18.5f, 9.5f)
    }
}
