package ru.wertik.orca.core

import org.commonmark.Extension
import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TableBlock
import org.commonmark.ext.gfm.tables.TableBody
import org.commonmark.ext.gfm.tables.TableCell
import org.commonmark.ext.gfm.tables.TableHead
import org.commonmark.ext.gfm.tables.TableRow
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.ext.task.list.items.TaskListItemMarker
import org.commonmark.ext.task.list.items.TaskListItemsExtension
import org.commonmark.node.Block
import org.commonmark.node.BlockQuote
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Emphasis
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.Heading
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Image
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.Link
import org.commonmark.node.ListItem
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text
import org.commonmark.node.ThematicBreak
import org.commonmark.parser.Parser

class CommonmarkOrcaParser(
    private val parser: Parser = defaultParser(),
    private val maxTreeDepth: Int = DEFAULT_MAX_TREE_DEPTH,
    private val onDepthLimitExceeded: ((Int) -> Unit)? = null,
) : OrcaParser {

    init {
        require(maxTreeDepth > 0) { "maxTreeDepth must be greater than 0" }
    }

    override fun cacheKey(): Any = ParserCacheKey(parser, maxTreeDepth)

    override fun parse(input: String): OrcaDocument {
        val root = parser.parse(input)
        val depthLimitReporter = DepthLimitReporter(onDepthLimitExceeded)
        val mapper = CommonmarkTreeMapper(
            maxTreeDepth = maxTreeDepth,
            depthLimitReporter = depthLimitReporter,
        )
        return OrcaDocument(
            blocks = root.childSequence()
                .mapNotNull { child -> mapper.mapBlock(child, depth = 0) }
                .toList(),
        )
    }
}

private data class ParserCacheKey(
    val parser: Parser,
    val maxTreeDepth: Int,
)

private class DepthLimitReporter(
    private val callback: ((Int) -> Unit)?,
) {
    private var wasReported = false

    fun report(depth: Int) {
        if (wasReported) return
        wasReported = true
        callback?.invoke(depth)
    }
}

private class CommonmarkTreeMapper(
    private val maxTreeDepth: Int,
    private val depthLimitReporter: DepthLimitReporter,
) {
    fun mapBlock(node: Node, depth: Int): OrcaBlock? {
        if (isDepthExceeded(depth)) {
            return null
        }

        return when (node) {
            is Heading -> OrcaBlock.Heading(
                level = node.level,
                content = mapInlineContainer(node, depth + 1),
            )

            is Paragraph -> mapParagraph(node, depth + 1)

            is BulletList -> OrcaBlock.ListBlock(
                ordered = false,
                items = node.childSequence()
                    .filterIsInstance<ListItem>()
                    .map { child -> mapListItem(child, depth + 1) }
                    .toList(),
            )

            is OrderedList -> OrcaBlock.ListBlock(
                ordered = true,
                items = node.childSequence()
                    .filterIsInstance<ListItem>()
                    .map { child -> mapListItem(child, depth + 1) }
                    .toList(),
                startNumber = node.markerStartNumber ?: 1,
            )

            is BlockQuote -> OrcaBlock.Quote(
                blocks = node.childSequence()
                    .mapNotNull { child -> mapBlock(child, depth + 1) }
                    .toList(),
            )

            is FencedCodeBlock -> OrcaBlock.CodeBlock(
                code = node.literal.trimEnd('\n'),
                language = node.info.takeLanguageOrNull(),
            )

            is IndentedCodeBlock -> OrcaBlock.CodeBlock(
                code = node.literal.trimEnd('\n'),
                language = null,
            )

            is ThematicBreak -> OrcaBlock.ThematicBreak

            is TableBlock -> mapTable(node, depth + 1)

            else -> null
        }
    }

    private fun mapParagraph(node: Paragraph, depth: Int): OrcaBlock {
        val content = mapInlineContainer(node, depth + 1)
        val standaloneImage = content.singleOrNull() as? OrcaInline.Image
        return if (standaloneImage == null) {
            OrcaBlock.Paragraph(content = content)
        } else {
            OrcaBlock.Image(
                source = standaloneImage.source,
                alt = standaloneImage.alt,
                title = standaloneImage.title,
            )
        }
    }

    private fun mapTable(tableBlock: TableBlock, depth: Int): OrcaBlock.Table? {
        if (isDepthExceeded(depth)) {
            return null
        }

        val header = tableBlock.childSequence()
            .filterIsInstance<TableHead>()
            .flatMap { tableHead ->
                tableHead.childSequence()
                    .filterIsInstance<TableRow>()
            }
            .firstOrNull()
            ?.let { tableRow -> mapTableRow(tableRow, depth + 1) }
            .orEmpty()

        val rows = tableBlock.childSequence()
            .filterIsInstance<TableBody>()
            .flatMap { body ->
                body.childSequence()
                    .filterIsInstance<TableRow>()
                    .map { tableRow -> mapTableRow(tableRow, depth + 1) }
            }
            .toList()

        if (header.isEmpty() && rows.isEmpty()) {
            return null
        }

        return OrcaBlock.Table(
            header = header,
            rows = rows,
        )
    }

    private fun mapTableRow(tableRow: TableRow, depth: Int): List<OrcaTableCell> {
        if (isDepthExceeded(depth)) {
            return emptyList()
        }

        return tableRow.childSequence()
            .filterIsInstance<TableCell>()
            .map { tableCell -> mapTableCell(tableCell, depth + 1) }
            .toList()
    }

    private fun mapTableCell(tableCell: TableCell, depth: Int): OrcaTableCell {
        return OrcaTableCell(
            content = mapInlineContainer(tableCell, depth + 1),
            alignment = tableCell.alignment.toOrcaAlignmentOrNull(),
        )
    }

    private fun mapListItem(node: ListItem, depth: Int): OrcaListItem {
        val taskState = node.childSequence()
            .filterIsInstance<TaskListItemMarker>()
            .firstOrNull()
            ?.toTaskState()

        val mapped = node.childSequence()
            .filterNot { child -> child is TaskListItemMarker }
            .mapNotNull { child -> mapBlock(child, depth + 1) }
            .toList()
        return OrcaListItem(
            blocks = mapped,
            taskState = taskState,
        )
    }

    private fun mapInlineContainer(container: Node, depth: Int): List<OrcaInline> {
        if (isDepthExceeded(depth)) {
            return emptyList()
        }

        return container.childSequence()
            .flatMap { child -> mapInline(child, depth + 1) }
            .toList()
    }

    private fun mapInline(node: Node, depth: Int): Sequence<OrcaInline> {
        if (isDepthExceeded(depth)) {
            return emptySequence()
        }

        return when (node) {
            is Text -> sequenceOf(OrcaInline.Text(node.literal))
            is StrongEmphasis -> sequenceOf(OrcaInline.Bold(content = mapInlineContainer(node, depth + 1)))
            is Emphasis -> sequenceOf(OrcaInline.Italic(content = mapInlineContainer(node, depth + 1)))
            is Strikethrough -> sequenceOf(OrcaInline.Strikethrough(content = mapInlineContainer(node, depth + 1)))
            is Code -> sequenceOf(OrcaInline.InlineCode(code = node.literal))
            is Link -> sequenceOf(
                OrcaInline.Link(
                    destination = node.destination,
                    content = mapInlineContainer(node, depth + 1),
                )
            )

            is Image -> {
                val altText = mapInlineContainer(node, depth + 1)
                    .toPlainText()
                    .trim()
                    .takeIf { it.isNotEmpty() }
                sequenceOf(
                    OrcaInline.Image(
                        source = node.destination,
                        alt = altText,
                        title = node.title?.takeIf { it.isNotBlank() },
                    ),
                )
            }

            is SoftLineBreak,
            is HardLineBreak,
                -> sequenceOf(OrcaInline.Text("\n"))

            is Block -> emptySequence()
            else -> node.childSequence().flatMap { child -> mapInline(child, depth + 1) }
        }
    }

    private fun isDepthExceeded(depth: Int): Boolean {
        if (depth <= maxTreeDepth) {
            return false
        }
        depthLimitReporter.report(depth)
        return true
    }
}

private const val DEFAULT_MAX_TREE_DEPTH = 64

private fun Node.childSequence(): Sequence<Node> = sequence {
    var child = firstChild
    while (child != null) {
        yield(child)
        child = child.next
    }
}

private fun String.takeLanguageOrNull(): String? {
    val firstToken = trim().split(' ').firstOrNull()?.trim()
    return firstToken?.takeIf { it.isNotEmpty() }
}

private fun List<OrcaInline>.toPlainText(): String {
    return joinToString(separator = "") { inline ->
        when (inline) {
            is OrcaInline.Text -> inline.text
            is OrcaInline.Bold -> inline.content.toPlainText()
            is OrcaInline.Italic -> inline.content.toPlainText()
            is OrcaInline.Strikethrough -> inline.content.toPlainText()
            is OrcaInline.InlineCode -> inline.code
            is OrcaInline.Link -> inline.content.toPlainText().ifEmpty { inline.destination }
            is OrcaInline.Image -> inline.alt ?: ""
        }
    }
}

private fun TaskListItemMarker.toTaskState(): OrcaTaskState {
    return if (isChecked) {
        OrcaTaskState.CHECKED
    } else {
        OrcaTaskState.UNCHECKED
    }
}

private fun TableCell.Alignment?.toOrcaAlignmentOrNull(): OrcaTableAlignment? {
    return when (this) {
        TableCell.Alignment.LEFT -> OrcaTableAlignment.LEFT
        TableCell.Alignment.CENTER -> OrcaTableAlignment.CENTER
        TableCell.Alignment.RIGHT -> OrcaTableAlignment.RIGHT
        null -> null
    }
}

private fun defaultParser(): Parser {
    val extensions: List<Extension> = listOf(
        TablesExtension.create(),
        StrikethroughExtension.create(),
        TaskListItemsExtension.create(),
    )
    return Parser.builder()
        .extensions(extensions)
        .build()
}
