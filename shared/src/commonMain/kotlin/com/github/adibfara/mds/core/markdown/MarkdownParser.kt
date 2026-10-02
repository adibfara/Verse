package com.github.adibfara.mds.core.markdown

import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.parser.MarkdownParser as IntellijMarkdownParser

class MarkdownParser {

    private val flavour = GFMFlavourDescriptor()

    fun parse(text: String): MdDocument {
        if (text.isEmpty()) return MdDocument.Empty
        val tree = IntellijMarkdownParser(flavour).buildMarkdownTreeFromString(text)
        val blocks = BlockBuilder(text).build(tree.children)
        val collector = StyleCollector(text)
        collector.visit(tree)
        val tasks = findTaskMarkers(text)
        val styles = (collector.styles + taskStyles(text, tasks)).sortedBy { it.range.start }
        return MdDocument(
            text = text,
            blocks = blocks,
            styles = styles,
            markers = collector.markers.sortedBy { it.start },
            decorations = buildDecorations(text, styles, tasks),
            tasks = tasks,
            links = collector.links,
            stats = computeStats(text),
        )
    }
}

private const val BulletGlyph = "•"
private val TaskBoxReplacement = TaskBoxPlaceholder.toString()

private fun taskStyles(text: String, tasks: List<MdTask>): List<StyledRange> =
    tasks.flatMap { task ->
        if (!task.checked) {
            listOf(StyledRange(task.range, StyleKind.TaskMarker))
        } else {
            val textStart = skipSpacesFrom(text, task.range.end)
            val lineEnd = lineEndFrom(text, textStart)
            listOf(StyledRange(task.range, StyleKind.TaskMarkerDone)) +
                if (lineEnd > textStart) {
                    listOf(StyledRange(MdRange(textStart, lineEnd), StyleKind.TaskText))
                } else {
                    emptyList()
                }
        }
    }

fun documentWithTaskToggled(
    document: MdDocument,
    text: String,
    range: MdRange,
    checked: Boolean,
): MdDocument {
    if (document.tasks.none { it.range == range }) return document
    val lineEnd = lineEndFrom(text, range.end)
    val kept = document.styles.filterNot { styled ->
        val isMarker = styled.range == range &&
            (styled.kind == StyleKind.TaskMarker || styled.kind == StyleKind.TaskMarkerDone)
        val isOwnText = styled.kind == StyleKind.TaskText &&
            styled.range.start >= range.end &&
            styled.range.end <= lineEnd
        isMarker || isOwnText
    }
    val toggled = MdTask(range, checked)
    return document.copy(
        text = text,
        styles = (kept + taskStyles(text, listOf(toggled))).sortedBy { it.range.start },
        tasks = document.tasks.map { if (it.range == range) toggled else it },
        stats = computeStats(text),
    )
}

private fun skipSpacesFrom(text: String, start: Int): Int {
    var index = start.coerceIn(0, text.length)
    while (index < text.length && (text[index] == ' ' || text[index] == '	')) index++
    return index
}

private fun lineEndFrom(text: String, start: Int): Int {
    var index = start.coerceIn(0, text.length)
    while (index < text.length && text[index] != '\n') index++
    return index
}

private fun buildDecorations(
    text: String,
    styles: List<StyledRange>,
    tasks: List<MdTask>,
): List<MaskEdit> {
    val decorations = mutableListOf<MaskEdit>()

    styles.filter { it.kind == StyleKind.ListMarker }.forEach { styled ->
        val owningTask = tasks.firstOrNull { task ->
            task.range.start >= styled.range.end &&
                text.substring(styled.range.end, task.range.start).isBlank()
        }
        if (owningTask != null) {
            decorations.add(MaskEdit(MdRange(styled.range.start, owningTask.range.start), ""))
            return@forEach
        }
        val raw = text.substring(styled.range.start, styled.range.end)
        if (raw.isNotEmpty() && (raw[0] == '-' || raw[0] == '*' || raw[0] == '+')) {
            decorations.add(MaskEdit(styled.range, BulletGlyph + raw.drop(1)))
        }
    }

    tasks.forEach { task ->
        decorations.add(MaskEdit(task.range, TaskBoxReplacement))
    }

    return decorations
}

internal fun findTaskMarkerInLine(text: String, lineStart: Int, lineEnd: Int): MdTask? {
    var index = lineStart
    while (index < lineEnd && (text[index] == ' ' || text[index] == '	')) index++

    val bulletStart = index
    if (index < lineEnd && (text[index] == '-' || text[index] == '*' || text[index] == '+')) {
        index++
    } else {
        while (index < lineEnd && text[index].isDigit()) index++
        if (index == bulletStart) return null
        if (index >= lineEnd || (text[index] != '.' && text[index] != ')')) return null
        index++
    }

    if (index >= lineEnd || text[index] != ' ') return null
    while (index < lineEnd && text[index] == ' ') index++

    if (index + 2 >= lineEnd) return null
    if (text[index] != '[' || text[index + 2] != ']') return null
    val marker = text[index + 1]
    if (marker != ' ' && marker != 'x' && marker != 'X') return null
    return MdTask(MdRange(index, index + 3), checked = marker != ' ')
}

internal fun findTaskMarkers(text: String): List<MdTask> {
    val markers = mutableListOf<MdTask>()
    var lineStart = 0
    while (lineStart <= text.length) {
        var lineEnd = lineStart
        while (lineEnd < text.length && text[lineEnd] != '\n') lineEnd++
        findTaskMarkerInLine(text, lineStart, lineEnd)?.let(markers::add)
        if (lineEnd >= text.length) break
        lineStart = lineEnd + 1
    }
    return markers
}

private fun ASTNode.range(): MdRange = MdRange(startOffset, endOffset)

private fun ASTNode.textIn(source: String): String = source.substring(startOffset, endOffset)

private fun ASTNode.firstChildOfType(type: IElementType): ASTNode? =
    children.firstOrNull { it.type == type }

private fun ASTNode.descendantOfType(type: IElementType): ASTNode? {
    children.forEach { child ->
        if (child.type == type) return child
        child.descendantOfType(type)?.let { return it }
    }
    return null
}

private val HeadingTypes = mapOf(
    MarkdownElementTypes.ATX_1 to 1,
    MarkdownElementTypes.ATX_2 to 2,
    MarkdownElementTypes.ATX_3 to 3,
    MarkdownElementTypes.ATX_4 to 4,
    MarkdownElementTypes.ATX_5 to 5,
    MarkdownElementTypes.ATX_6 to 6,
    MarkdownElementTypes.SETEXT_1 to 1,
    MarkdownElementTypes.SETEXT_2 to 2,
)

private class BlockBuilder(private val source: String) {

    fun build(nodes: List<ASTNode>): List<MdBlock> = nodes.mapNotNull(::buildBlock)

    private fun buildBlock(node: ASTNode): MdBlock? {
        HeadingTypes[node.type]?.let { level ->
            val content = node.firstChildOfType(MarkdownTokenTypes.ATX_CONTENT)
                ?: node.firstChildOfType(MarkdownTokenTypes.SETEXT_CONTENT)
            return MdBlock.Heading(
                level = level,
                content = inlines(content?.children ?: node.children),
                range = node.range(),
            )
        }
        return when (node.type) {
            MarkdownElementTypes.PARAGRAPH -> MdBlock.Paragraph(inlines(node.children), node.range())

            MarkdownElementTypes.UNORDERED_LIST -> MdBlock.ListBlock(
                ordered = false,
                items = node.children
                    .filter { it.type == MarkdownElementTypes.LIST_ITEM }
                    .map(::buildListItem),
                range = node.range(),
            )

            MarkdownElementTypes.ORDERED_LIST -> MdBlock.ListBlock(
                ordered = true,
                items = node.children
                    .filter { it.type == MarkdownElementTypes.LIST_ITEM }
                    .map(::buildListItem),
                range = node.range(),
            )

            MarkdownElementTypes.CODE_FENCE -> MdBlock.CodeBlock(
                language = node.firstChildOfType(MarkdownTokenTypes.FENCE_LANG)
                    ?.textIn(source)
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() },
                code = node.children
                    .filter {
                        it.type == MarkdownTokenTypes.CODE_FENCE_CONTENT ||
                            it.type == MarkdownTokenTypes.EOL
                    }
                    .joinToString("") { it.textIn(source) }
                    .trim('\n'),
                range = node.range(),
            )

            MarkdownElementTypes.CODE_BLOCK -> MdBlock.CodeBlock(
                language = null,
                code = node.textIn(source).trimEnd().trimIndent(),
                range = node.range(),
            )

            MarkdownElementTypes.BLOCK_QUOTE -> MdBlock.Quote(
                children = build(node.children),
                range = node.range(),
            )

            GFMElementTypes.TABLE -> buildTable(node)

            MarkdownTokenTypes.HORIZONTAL_RULE -> MdBlock.HorizontalRule(node.range())

            else -> null
        }
    }

    private fun buildListItem(node: ASTNode): MdListItem {
        val marker = node.children
            .firstOrNull {
                it.type == MarkdownTokenTypes.LIST_BULLET || it.type == MarkdownTokenTypes.LIST_NUMBER
            }
            ?.textIn(source)
            ?.trim()
            .orEmpty()
        var lineEnd = node.startOffset
        while (lineEnd < source.length && source[lineEnd] != '\n') lineEnd++
        val task = findTaskMarkerInLine(source, node.startOffset, lineEnd)
        val content = build(node.children)
        return MdListItem(
            marker = marker,
            checked = task?.checked,
            checkBoxRange = task?.range,
            content = if (task == null) content else stripRange(content, task.range),
            range = node.range(),
        )
    }

    private fun stripRange(blocks: List<MdBlock>, range: MdRange): List<MdBlock> = blocks.map { block ->
        when (block) {
            is MdBlock.Paragraph -> block.copy(
                content = block.content.filterNot { inline ->
                    inline.range.start >= range.start && inline.range.end <= range.end
                },
            )

            else -> block
        }
    }

    private fun buildTable(node: ASTNode): MdBlock.Table {
        val header = node.children
            .firstOrNull { it.type == GFMElementTypes.HEADER }
            ?.let(::buildRow)
            .orEmpty()
        val rows = node.children
            .filter { it.type == GFMElementTypes.ROW }
            .map(::buildRow)
        return MdBlock.Table(header = header, rows = rows, range = node.range())
    }

    private fun buildRow(node: ASTNode): List<List<MdInline>> = node.children
        .filter { it.type == GFMTokenTypes.CELL }
        .map { cell -> inlines(cell.children).ifEmpty { listOf(textOf(cell)) } }

    private fun textOf(node: ASTNode): MdInline =
        MdInline.Text(node.textIn(source).trim(), node.range())

    fun inlines(nodes: List<ASTNode>): List<MdInline> {
        val result = mutableListOf<MdInline>()
        nodes.forEach { node -> appendInline(node, result) }
        return merged(result)
    }

    private fun appendInline(node: ASTNode, output: MutableList<MdInline>) {
        when (node.type) {
            MarkdownElementTypes.STRONG -> output.add(
                MdInline.Styled(InlineStyle.Bold, inlines(node.children), node.range()),
            )

            MarkdownElementTypes.EMPH -> output.add(
                MdInline.Styled(InlineStyle.Italic, inlines(node.children), node.range()),
            )

            GFMElementTypes.STRIKETHROUGH -> output.add(
                MdInline.Styled(InlineStyle.Strikethrough, inlines(node.children), node.range()),
            )

            MarkdownElementTypes.CODE_SPAN -> output.add(
                MdInline.Code(node.textIn(source).trim('`'), node.range()),
            )

            MarkdownElementTypes.IMAGE -> output.add(buildImage(node))

            MarkdownElementTypes.INLINE_LINK,
            MarkdownElementTypes.FULL_REFERENCE_LINK,
            MarkdownElementTypes.SHORT_REFERENCE_LINK,
            -> output.add(buildLink(node))

            MarkdownElementTypes.AUTOLINK,
            MarkdownTokenTypes.AUTOLINK,
            MarkdownTokenTypes.EMAIL_AUTOLINK,
            GFMTokenTypes.GFM_AUTOLINK,
            -> {
                val target = node.textIn(source).trim('<', '>')
                output.add(MdInline.Link(target, listOf(MdInline.Text(target, node.range())), node.range()))
            }

            MarkdownTokenTypes.HARD_LINE_BREAK -> output.add(MdInline.LineBreak(node.range()))

            MarkdownTokenTypes.EOL -> output.add(MdInline.Text(" ", node.range()))

            GFMTokenTypes.CHECK_BOX -> Unit

            else -> {
                if (node.children.isEmpty()) {
                    output.add(MdInline.Text(node.textIn(source), node.range()))
                } else {
                    node.children.forEach { child -> appendInline(child, output) }
                }
            }
        }
    }

    private fun buildImage(node: ASTNode): MdInline.Image {
        val destination = node.descendantOfType(MarkdownElementTypes.LINK_DESTINATION)
            ?.textIn(source)
            ?.trim('<', '>')
            .orEmpty()
        val alt = node.descendantOfType(MarkdownElementTypes.LINK_TEXT)
            ?.textIn(source)
            ?.trim('[', ']')
            .orEmpty()
        return MdInline.Image(target = destination, alt = alt, range = node.range())
    }

    private fun buildLink(node: ASTNode): MdInline.Link {
        val label = node.firstChildOfType(MarkdownElementTypes.LINK_TEXT)
            ?: node.firstChildOfType(MarkdownElementTypes.LINK_LABEL)
        val destination = node.descendantOfType(MarkdownElementTypes.LINK_DESTINATION)
            ?.textIn(source)
            ?.trim('<', '>')
            ?: label?.textIn(source)?.trim('[', ']').orEmpty()
        val children = label
            ?.children
            ?.filterNot {
                it.type == MarkdownTokenTypes.LBRACKET || it.type == MarkdownTokenTypes.RBRACKET
            }
            ?.let(::inlines)
            .orEmpty()
        return MdInline.Link(
            target = destination,
            children = children.ifEmpty {
                listOf(MdInline.Text(label?.textIn(source)?.trim('[', ']').orEmpty(), node.range()))
            },
            range = node.range(),
        )
    }

    private fun merged(items: List<MdInline>): List<MdInline> {
        val result = mutableListOf<MdInline>()
        items.forEach { item ->
            val previous = result.lastOrNull()
            if (item is MdInline.Text && previous is MdInline.Text) {
                result[result.lastIndex] = MdInline.Text(
                    text = previous.text + item.text,
                    range = MdRange(previous.range.start, item.range.end),
                )
            } else {
                result.add(item)
            }
        }
        return result.filterNot { it is MdInline.Text && it.text.isEmpty() }
    }
}

private class StyleCollector(private val source: String) {

    val styles = mutableListOf<StyledRange>()
    val markers = mutableListOf<MdRange>()
    val links = mutableListOf<MdLink>()

    fun visit(node: ASTNode, withinImage: Boolean = false) {
        HeadingTypes[node.type]?.let { level ->
            styles.add(StyledRange(node.range(), StyleKind.heading(level)))
            node.firstChildOfType(MarkdownTokenTypes.ATX_HEADER)?.let { header ->
                markers.add(MdRange(header.startOffset, skipSpaces(header.endOffset)))
            }
        }

        when (node.type) {
            MarkdownElementTypes.STRONG -> {
                styles.add(StyledRange(node.range(), StyleKind.Bold))
                markEmphasisTokens(node)
            }

            MarkdownElementTypes.EMPH -> {
                styles.add(StyledRange(node.range(), StyleKind.Italic))
                markEmphasisTokens(node)
            }

            GFMElementTypes.STRIKETHROUGH -> {
                styles.add(StyledRange(node.range(), StyleKind.Strikethrough))
                node.children
                    .filter { it.type == GFMTokenTypes.TILDE }
                    .forEach { markers.add(it.range()) }
            }

            MarkdownElementTypes.CODE_SPAN -> {
                styles.add(StyledRange(node.range(), StyleKind.InlineCode))
                node.children
                    .filter { it.type == MarkdownTokenTypes.BACKTICK }
                    .forEach { markers.add(it.range()) }
            }

            MarkdownElementTypes.CODE_FENCE -> {
                styles.add(StyledRange(node.range(), StyleKind.CodeBlock))
                node.children
                    .filter {
                        it.type == MarkdownTokenTypes.CODE_FENCE_START ||
                            it.type == MarkdownTokenTypes.CODE_FENCE_END ||
                            it.type == MarkdownTokenTypes.FENCE_LANG
                    }
                    .forEach { markers.add(it.range()) }
            }

            MarkdownElementTypes.CODE_BLOCK -> {
                styles.add(StyledRange(node.range(), StyleKind.CodeBlock))
            }

            MarkdownElementTypes.BLOCK_QUOTE -> {
                styles.add(StyledRange(node.range(), StyleKind.Quote))
            }

            MarkdownElementTypes.INLINE_LINK,
            MarkdownElementTypes.IMAGE,
            -> node.descendantOfType(MarkdownElementTypes.LINK_DESTINATION)?.let { destination ->
                styles.add(StyledRange(node.range(), StyleKind.Link))
                markLinkPunctuation(node)
                if (node.type == MarkdownElementTypes.INLINE_LINK && !withinImage) {
                    links.add(MdLink(node.range(), destination.textIn(source).trim('<', '>')))
                }
            }

            MarkdownTokenTypes.BLOCK_QUOTE -> markers.add(node.range())

            MarkdownTokenTypes.LIST_BULLET,
            MarkdownTokenTypes.LIST_NUMBER,
            -> styles.add(StyledRange(node.range(), StyleKind.ListMarker))

            else -> Unit
        }

        val descendingIntoImage = withinImage || node.type == MarkdownElementTypes.IMAGE
        node.children.forEach { child -> visit(child, descendingIntoImage) }
    }

    private fun markEmphasisTokens(node: ASTNode) {
        node.children
            .filter { it.type == MarkdownTokenTypes.EMPH }
            .forEach { markers.add(it.range()) }
    }

    private fun markLinkPunctuation(node: ASTNode) {
        node.children.forEach { child ->
            when (child.type) {
                MarkdownTokenTypes.EXCLAMATION_MARK,
                MarkdownTokenTypes.LPAREN,
                MarkdownTokenTypes.RPAREN,
                MarkdownTokenTypes.LBRACKET,
                MarkdownTokenTypes.RBRACKET,
                MarkdownTokenTypes.COLON,
                MarkdownElementTypes.LINK_DESTINATION,
                MarkdownElementTypes.LINK_TITLE,
                -> markers.add(child.range())

                MarkdownElementTypes.LINK_TEXT,
                MarkdownElementTypes.LINK_LABEL,
                -> child.children
                    .filter {
                        it.type == MarkdownTokenTypes.LBRACKET ||
                            it.type == MarkdownTokenTypes.RBRACKET
                    }
                    .forEach { markers.add(it.range()) }

                else -> Unit
            }
        }
    }

    private fun skipSpaces(from: Int): Int {
        var index = from
        while (index < source.length && (source[index] == ' ' || source[index] == '\t')) index++
        return index
    }
}
