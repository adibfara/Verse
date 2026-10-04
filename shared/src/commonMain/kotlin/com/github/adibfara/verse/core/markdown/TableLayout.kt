package com.github.adibfara.verse.core.markdown

internal const val TableGutterSpaces = 1

internal sealed interface MdTablePart {
    class Cell(val contentRange: MdRange) : MdTablePart
    class Divider(val range: MdRange) : MdTablePart
}

internal class MdTableRowSource(
    val range: MdRange,
    val isHeader: Boolean,
    val parts: List<MdTablePart>,
)

internal class MdTableSource(
    val range: MdRange,
    val rows: List<MdTableRowSource>,
    val delimiterRange: MdRange?,
)

internal class TableAlignment(
    val grids: List<MdTableGrid>,
    val edits: List<MaskEdit>,
)

internal fun alignTables(
    text: String,
    sources: List<MdTableSource>,
    markers: List<MdRange>,
): TableAlignment {
    if (sources.isEmpty()) return TableAlignment(emptyList(), emptyList())
    val sortedMarkers = markers.sortedBy { it.start }
    val grids = mutableListOf<MdTableGrid>()
    val edits = mutableListOf<MaskEdit>()
    sources.forEach { source ->
        alignTable(text, source, sortedMarkers, grids, edits)
    }
    return TableAlignment(grids, edits)
}

private fun alignTable(
    text: String,
    source: MdTableSource,
    markers: List<MdRange>,
    grids: MutableList<MdTableGrid>,
    edits: MutableList<MaskEdit>,
) {
    if (source.rows.isEmpty()) return
    val widths = columnWidths(source, markers)
    val rowDividers = source.rows.map { row -> dividersOf(row, widths, markers) }
    rowDividers.forEach { dividers ->
        dividers.forEach { divider ->
            edits.add(MaskEdit(divider.range, " ".repeat(divider.replacementLength)))
        }
    }

    val delimiter = source.delimiterRange?.let { range ->
        val start = if (range.start > 0 && text[range.start - 1] == '\n') range.start - 1 else range.start
        MdRange(start, range.end)
    }
    delimiter?.let { edits.add(MaskEdit(it, "")) }

    val headerIndex = source.rows.indexOfFirst { it.isHeader }
    val gridIndex = if (headerIndex >= 0) headerIndex else 0
    grids.add(
        MdTableGrid(
            range = source.range,
            headerRange = source.rows.getOrNull(headerIndex)?.range,
            rowRanges = source.rows.map { it.range },
            dividers = rowDividers[gridIndex],
            delimiterRange = delimiter,
        ),
    )
}

private fun columnWidths(source: MdTableSource, markers: List<MdRange>): IntArray {
    var columns = 0
    source.rows.forEach { row ->
        columns = maxOf(columns, row.parts.count { it is MdTablePart.Cell })
    }
    val widths = IntArray(columns)
    source.rows.forEach { row ->
        var column = 0
        row.parts.forEach { part ->
            if (part is MdTablePart.Cell) {
                widths[column] = maxOf(widths[column], visibleWidth(part.contentRange, markers))
                column++
            }
        }
    }
    return widths
}

private fun dividersOf(
    row: MdTableRowSource,
    widths: IntArray,
    markers: List<MdRange>,
): List<MdTableDivider> {
    val dividers = mutableListOf<MdTableDivider>()
    var column = 0
    var deficit = 0
    row.parts.forEach { part ->
        when (part) {
            is MdTablePart.Cell -> {
                val width = widths.getOrElse(column) { 0 }
                deficit = (width - visibleWidth(part.contentRange, markers)).coerceAtLeast(0)
                column++
            }

            is MdTablePart.Divider -> {
                dividers.add(
                    MdTableDivider(
                        range = part.range,
                        leadingSpaces = deficit,
                        gutterSpaces = TableGutterSpaces,
                    ),
                )
                deficit = 0
            }
        }
    }
    return dividers
}

private fun visibleWidth(range: MdRange, markers: List<MdRange>): Int {
    if (range.length <= 0) return 0
    var hidden = 0
    var index = firstMarkerEndingAfter(markers, range.start)
    while (index < markers.size && markers[index].start < range.end) {
        val marker = markers[index]
        val start = maxOf(marker.start, range.start)
        val end = minOf(marker.end, range.end)
        if (end > start) hidden += end - start
        index++
    }
    return (range.length - hidden).coerceAtLeast(0)
}

private fun firstMarkerEndingAfter(markers: List<MdRange>, offset: Int): Int {
    var low = 0
    var high = markers.size
    while (low < high) {
        val middle = (low + high) / 2
        if (markers[middle].start < offset) low = middle + 1 else high = middle
    }
    return if (low > 0 && markers[low - 1].end > offset) low - 1 else low
}
