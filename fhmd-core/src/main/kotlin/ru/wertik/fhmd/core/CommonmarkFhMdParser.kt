package ru.wertik.fhmd.core

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

class CommonmarkFhMdParser(
    private val parser: Parser = defaultParser(),
) : FhMdParser {

    override fun parse(input: String): FhMdDocument {
        val root = parser.parse(input)
        return FhMdDocument(
            blocks = root.childSequence()
                .mapNotNull { child -> mapBlock(child, depth = 0) }
                .toList(),
        )
    }

    private fun mapBlock(node: Node, depth: Int): FhMdBlock? {
        if (depth > MAX_TREE_DEPTH) {
            return null
        }

        return when (node) {
            is Heading -> FhMdBlock.Heading(
                level = node.level,
                content = mapInlineContainer(node, depth + 1),
            )

            is Paragraph -> mapParagraph(node, depth + 1)

            is BulletList -> FhMdBlock.ListBlock(
                ordered = false,
                items = node.childSequence()
                    .filterIsInstance<ListItem>()
                    .map { child -> mapListItem(child, depth + 1) }
                    .toList(),
            )

            is OrderedList -> FhMdBlock.ListBlock(
                ordered = true,
                items = node.childSequence()
                    .filterIsInstance<ListItem>()
                    .map { child -> mapListItem(child, depth + 1) }
                    .toList(),
                startNumber = node.markerStartNumber ?: 1,
            )

            is BlockQuote -> FhMdBlock.Quote(
                blocks = node.childSequence()
                    .mapNotNull { child -> mapBlock(child, depth + 1) }
                    .toList(),
            )

            is FencedCodeBlock -> FhMdBlock.CodeBlock(
                code = node.literal.trimEnd('\n'),
                language = node.info.takeLanguageOrNull(),
            )

            is IndentedCodeBlock -> FhMdBlock.CodeBlock(
                code = node.literal.trimEnd('\n'),
                language = null,
            )

            is ThematicBreak -> FhMdBlock.ThematicBreak

            is TableBlock -> mapTable(node, depth + 1)

            else -> null
        }
    }

    private fun mapParagraph(node: Paragraph, depth: Int): FhMdBlock {
        val content = mapInlineContainer(node, depth + 1)
        val standaloneImage = content.singleOrNull() as? FhMdInline.Image
        return if (standaloneImage == null) {
            FhMdBlock.Paragraph(content = content)
        } else {
            FhMdBlock.Image(
                source = standaloneImage.source,
                alt = standaloneImage.alt,
                title = standaloneImage.title,
            )
        }
    }

    private fun mapTable(tableBlock: TableBlock, depth: Int): FhMdBlock.Table? {
        if (depth > MAX_TREE_DEPTH) {
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

        return FhMdBlock.Table(
            header = header,
            rows = rows,
        )
    }

    private fun mapTableRow(tableRow: TableRow, depth: Int): List<FhMdTableCell> {
        if (depth > MAX_TREE_DEPTH) {
            return emptyList()
        }

        return tableRow.childSequence()
            .filterIsInstance<TableCell>()
            .map { tableCell -> mapTableCell(tableCell, depth + 1) }
            .toList()
    }

    private fun mapTableCell(tableCell: TableCell, depth: Int): FhMdTableCell {
        return FhMdTableCell(
            content = mapInlineContainer(tableCell, depth + 1),
            alignment = tableCell.alignment.toFhMdAlignmentOrNull(),
        )
    }

    private fun mapListItem(node: ListItem, depth: Int): FhMdListItem {
        val taskState = node.childSequence()
            .filterIsInstance<TaskListItemMarker>()
            .firstOrNull()
            ?.toTaskState()

        val mapped = node.childSequence()
            .filterNot { child -> child is TaskListItemMarker }
            .mapNotNull { child -> mapBlock(child, depth + 1) }
            .toList()
        return FhMdListItem(
            blocks = mapped,
            taskState = taskState,
        )
    }

    private fun mapInlineContainer(container: Node, depth: Int): List<FhMdInline> {
        if (depth > MAX_TREE_DEPTH) {
            return emptyList()
        }

        return container.childSequence()
            .flatMap { child -> mapInline(child, depth + 1) }
            .toList()
    }

    private fun mapInline(node: Node, depth: Int): Sequence<FhMdInline> {
        if (depth > MAX_TREE_DEPTH) {
            return emptySequence()
        }

        return when (node) {
            is Text -> sequenceOf(FhMdInline.Text(node.literal))
            is StrongEmphasis -> sequenceOf(FhMdInline.Bold(content = mapInlineContainer(node, depth + 1)))
            is Emphasis -> sequenceOf(FhMdInline.Italic(content = mapInlineContainer(node, depth + 1)))
            is Strikethrough -> sequenceOf(FhMdInline.Strikethrough(content = mapInlineContainer(node, depth + 1)))
            is Code -> sequenceOf(FhMdInline.InlineCode(code = node.literal))
            is Link -> sequenceOf(
                FhMdInline.Link(
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
                    FhMdInline.Image(
                        source = node.destination,
                        alt = altText,
                        title = node.title?.takeIf { it.isNotBlank() },
                    ),
                )
            }

            is SoftLineBreak,
            is HardLineBreak,
                -> sequenceOf(FhMdInline.Text("\n"))

            is Block -> emptySequence()
            else -> node.childSequence().flatMap { child -> mapInline(child, depth + 1) }
        }
    }
}

private const val MAX_TREE_DEPTH = 256

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

private fun List<FhMdInline>.toPlainText(): String {
    return joinToString(separator = "") { inline ->
        when (inline) {
            is FhMdInline.Text -> inline.text
            is FhMdInline.Bold -> inline.content.toPlainText()
            is FhMdInline.Italic -> inline.content.toPlainText()
            is FhMdInline.Strikethrough -> inline.content.toPlainText()
            is FhMdInline.InlineCode -> inline.code
            is FhMdInline.Link -> inline.content.toPlainText().ifEmpty { inline.destination }
            is FhMdInline.Image -> inline.alt ?: ""
        }
    }
}

private fun TaskListItemMarker.toTaskState(): FhMdTaskState {
    return if (isChecked) {
        FhMdTaskState.CHECKED
    } else {
        FhMdTaskState.UNCHECKED
    }
}

private fun TableCell.Alignment?.toFhMdAlignmentOrNull(): FhMdTableAlignment? {
    return when (this) {
        TableCell.Alignment.LEFT -> FhMdTableAlignment.LEFT
        TableCell.Alignment.CENTER -> FhMdTableAlignment.CENTER
        TableCell.Alignment.RIGHT -> FhMdTableAlignment.RIGHT
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
