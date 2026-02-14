package ru.wertik.orca.core

import org.commonmark.Extension
import org.commonmark.ext.autolink.AutolinkExtension
import org.commonmark.ext.footnotes.FootnoteDefinition
import org.commonmark.ext.footnotes.FootnoteReference
import org.commonmark.ext.footnotes.FootnotesExtension
import org.commonmark.ext.footnotes.InlineFootnote
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
import org.commonmark.node.HtmlBlock
import org.commonmark.node.HtmlInline
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
        val frontMatterExtraction = extractFrontMatter(input)
        val root = parser.parse(frontMatterExtraction.markdown)
        val depthLimitReporter = DepthLimitReporter(onDepthLimitExceeded)
        val mapper = CommonmarkTreeMapper(
            maxTreeDepth = maxTreeDepth,
            depthLimitReporter = depthLimitReporter,
        )

        val rootChildren = root.childSequence().toList()
        val referencedFootnotes = rootChildren
            .filterIsInstance<FootnoteDefinition>()
            .map { definition -> mapper.mapFootnoteDefinition(definition, depth = 0) }
            .toList()
        val blocks = rootChildren
            .filterNot { child -> child is FootnoteDefinition }
            .mapNotNull { child -> mapper.mapBlock(child, depth = 0) }
            .toMutableList()

        val allFootnotes = referencedFootnotes + mapper.consumeInlineFootnoteDefinitions()
        if (allFootnotes.isNotEmpty()) {
            blocks += OrcaBlock.Footnotes(definitions = allFootnotes)
        }

        return OrcaDocument(
            blocks = blocks,
            frontMatter = frontMatterExtraction.frontMatter,
        )
    }
}

private data class ParserCacheKey(
    val parser: Parser,
    val maxTreeDepth: Int,
)

private data class FrontMatterExtraction(
    val markdown: String,
    val frontMatter: OrcaFrontMatter?,
)

private enum class FrontMatterFormat {
    YAML,
    TOML,
}

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
    private val inlineFootnotes = mutableListOf<OrcaFootnoteDefinition>()

    fun consumeInlineFootnoteDefinitions(): List<OrcaFootnoteDefinition> = inlineFootnotes.toList()

    fun mapFootnoteDefinition(node: FootnoteDefinition, depth: Int): OrcaFootnoteDefinition {
        if (isDepthExceeded(depth)) {
            return OrcaFootnoteDefinition(
                label = node.label,
                blocks = emptyList(),
            )
        }

        return OrcaFootnoteDefinition(
            label = node.label,
            blocks = node.childSequence()
                .mapNotNull { child -> mapBlock(child, depth + 1) }
                .toList(),
        )
    }

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

            is HtmlBlock -> OrcaBlock.HtmlBlock(
                html = node.literal.trimEnd('\n'),
            )

            is FootnoteDefinition -> null

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
                ),
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

            is FootnoteReference -> sequenceOf(
                OrcaInline.FootnoteReference(label = node.label),
            )

            is InlineFootnote -> {
                val label = "$INLINE_FOOTNOTE_LABEL_PREFIX${inlineFootnotes.size + 1}"
                val content = mapInlineContainer(node, depth + 1)
                inlineFootnotes += OrcaFootnoteDefinition(
                    label = label,
                    blocks = if (content.isEmpty()) {
                        emptyList()
                    } else {
                        listOf(OrcaBlock.Paragraph(content = content))
                    },
                )
                sequenceOf(OrcaInline.FootnoteReference(label = label))
            }

            is HtmlInline -> sequenceOf(
                OrcaInline.HtmlInline(html = node.literal),
            )

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
private const val INLINE_FOOTNOTE_LABEL_PREFIX = "__inline_footnote_"
private const val YAML_FRONT_MATTER_DELIMITER = "---"
private const val YAML_FRONT_MATTER_ALT_END = "..."
private const val TOML_FRONT_MATTER_DELIMITER = "+++"

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
            is OrcaInline.FootnoteReference -> "[${inline.label}]"
            is OrcaInline.HtmlInline -> htmlInlineToPlainText(inline.html)
        }
    }
}

private fun htmlInlineToPlainText(html: String): String {
    return decodeBasicHtmlEntities(
        html
            .replace(BR_TAG_REGEX, "\n")
            .replace(HTML_TAG_REGEX, ""),
    )
}

private fun decodeBasicHtmlEntities(text: String): String {
    return text
        .replace("&nbsp;", " ")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
}

private fun extractFrontMatter(input: String): FrontMatterExtraction {
    val normalized = input
        .replace("\r\n", "\n")
        .replace('\r', '\n')

    return extractDelimitedFrontMatter(
        markdown = normalized,
        openingDelimiter = YAML_FRONT_MATTER_DELIMITER,
        format = FrontMatterFormat.YAML,
    ) ?: extractDelimitedFrontMatter(
        markdown = normalized,
        openingDelimiter = TOML_FRONT_MATTER_DELIMITER,
        format = FrontMatterFormat.TOML,
    ) ?: FrontMatterExtraction(
        markdown = normalized,
        frontMatter = null,
    )
}

private fun extractDelimitedFrontMatter(
    markdown: String,
    openingDelimiter: String,
    format: FrontMatterFormat,
): FrontMatterExtraction? {
    val lines = markdown.split('\n')
    if (lines.firstOrNull() != openingDelimiter) {
        return null
    }

    var closingIndex = -1
    for (lineIndex in 1 until lines.size) {
        val line = lines[lineIndex]
        val isClosing = when (format) {
            FrontMatterFormat.YAML -> line == YAML_FRONT_MATTER_DELIMITER || line == YAML_FRONT_MATTER_ALT_END
            FrontMatterFormat.TOML -> line == TOML_FRONT_MATTER_DELIMITER
        }
        if (isClosing) {
            closingIndex = lineIndex
            break
        }
    }

    if (closingIndex == -1) {
        return null
    }

    val raw = lines.subList(1, closingIndex).joinToString("\n").trimEnd()
    val entries = parseFrontMatterEntries(
        raw = raw,
        format = format,
    )
    val frontMatter = when (format) {
        FrontMatterFormat.YAML -> OrcaFrontMatter.Yaml(raw = raw, entries = entries)
        FrontMatterFormat.TOML -> OrcaFrontMatter.Toml(raw = raw, entries = entries)
    }
    val markdownBody = lines.drop(closingIndex + 1)
        .joinToString("\n")
        .trimStart('\n')

    return FrontMatterExtraction(
        markdown = markdownBody,
        frontMatter = frontMatter,
    )
}

private fun parseFrontMatterEntries(
    raw: String,
    format: FrontMatterFormat,
): Map<String, String> {
    val separator = when (format) {
        FrontMatterFormat.YAML -> ':'
        FrontMatterFormat.TOML -> '='
    }

    val entries = linkedMapOf<String, String>()
    raw.lineSequence().forEach { line ->
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return@forEach
        if (trimmed.startsWith("#")) return@forEach
        if (format == FrontMatterFormat.YAML && trimmed.startsWith("-")) return@forEach

        val separatorIndex = trimmed.indexOf(separator)
        if (separatorIndex <= 0) return@forEach

        val key = trimmed.substring(0, separatorIndex).trim().trim('\'', '"')
        val value = trimmed.substring(separatorIndex + 1).trim().trim('\'', '"')
        if (key.isNotEmpty()) {
            entries[key] = value
        }
    }
    return entries
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
        AutolinkExtension.create(),
        TablesExtension.create(),
        StrikethroughExtension.create(),
        TaskListItemsExtension.create(),
        FootnotesExtension.builder()
            .inlineFootnotes(true)
            .build(),
    )
    return Parser.builder()
        .extensions(extensions)
        .build()
}

private val HTML_TAG_REGEX = Regex("<[^>]+>")
private val BR_TAG_REGEX = Regex("(?i)<br\\s*/?>")
