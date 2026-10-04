package com.github.adibfara.verse.core.markdown

class MaskedDocument(
    val mask: MarkerMask,
    val styles: List<StyledRange>,
    val tasks: List<MdTask>,
    val links: List<MdLink>,
    val codeBlocks: List<MdCodeBlock>,
    val rules: List<MdRange>,
    val quoteMarkers: List<MdRange>,
    val tables: List<MdTableGrid>,
) {
    fun transformedRangeOf(range: MdRange): MdRange {
        val start = mask.originalToTransformed(range.start)
        val end = mask.originalToTransformed(range.end)
        return MdRange(start, maxOf(start, end))
    }
}

fun maskDocument(
    document: MdDocument,
    edits: List<MaskEdit>,
    text: String,
    revealedLine: MdRange?,
): MaskedDocument {
    val revision = textRevisionBetween(document.text, text)
    val shiftedEdits = if (revision == null) {
        edits
    } else {
        edits.mapNotNull { edit -> revision.shift(edit.range)?.let { edit.copy(range = it) } }
    }
    return MaskedDocument(
        mask = buildMaskedText(text, shiftedEdits, revealedLine),
        styles = if (revision == null) {
            document.styles
        } else {
            document.styles.mapNotNull { styled ->
                revision.stretch(styled.range)?.let { styled.copy(range = it) }
            }
        },
        tasks = if (revision == null) {
            document.tasks
        } else {
            document.tasks.mapNotNull { task ->
                revision.shift(task.range)?.let { task.copy(range = it) }
            }
        },
        links = if (revision == null) {
            document.links
        } else {
            document.links.mapNotNull { link ->
                revision.shift(link.range)?.let { link.copy(range = it) }
            }
        },
        codeBlocks = if (revision == null) {
            document.codeBlocks
        } else {
            document.codeBlocks.mapNotNull { block ->
                val range = revision.stretch(block.range) ?: return@mapNotNull null
                val header = revision.shift(block.headerRange) ?: return@mapNotNull null
                block.copy(range = range, headerRange = header)
            }
        },
        rules = if (revision == null) {
            document.rules
        } else {
            document.rules.mapNotNull { revision.shift(it) }
        },
        quoteMarkers = if (revision == null) {
            document.quoteMarkers
        } else {
            document.quoteMarkers.mapNotNull { revision.shift(it) }
        },
        tables = if (revision == null) {
            document.tables
        } else {
            document.tables.mapNotNull { table -> revision.shiftTable(table) }
        },
    )
}

private fun TextRevision.shiftTable(table: MdTableGrid): MdTableGrid? {
    val range = stretch(table.range) ?: return null
    val delimiter = table.delimiterRange?.let { shift(it) ?: return null }
    return table.copy(
        range = range,
        headerRange = table.headerRange?.let { stretch(it) },
        rowRanges = table.rowRanges.mapNotNull { stretch(it) },
        dividers = table.dividers.mapNotNull { divider ->
            shift(divider.range)?.let { divider.copy(range = it) }
        },
        delimiterRange = delimiter,
    )
}
