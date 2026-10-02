package com.github.adibfara.mds.core.markdown

class MaskedDocument(
    val mask: MarkerMask,
    val styles: List<StyledRange>,
    val tasks: List<MdTask>,
    val links: List<MdLink>,
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
    )
}
