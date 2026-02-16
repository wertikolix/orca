package ru.wertik.orca.core

import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.findChildOfType
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.parser.LinkMap
import org.intellij.markdown.parser.MarkdownParser

private data class ResolvedLinkInfo(
    val labelNode: ASTNode,
    val destination: String,
    val title: String?,
)

private data class ParsedInlineFootnote(
    val content: String,
    val closingIndex: Int,
)

private val HTML_TAG_REGEX = Regex("</?[a-zA-Z][^>]*>")
private val BR_TAG_REGEX = Regex("(?i)<br\\s*/?>")

internal class IntellijTreeMapper(
    private val source: String,
    private val parser: MarkdownParser,
    private val linkMap: LinkMap,
    private val maxTreeDepth: Int,
    private val depthLimitReporter: DepthLimitReporter,
) {
    private val inlineFootnotes = mutableListOf<OrcaFootnoteDefinition>()

    fun consumeInlineFootnoteDefinitions(): List<OrcaFootnoteDefinition> = inlineFootnotes.toList()

    fun mapBlock(node: ASTNode, depth: Int): OrcaBlock? {
        if (isDepthExceeded(depth)) {
            return null
        }

        return when (node.type) {
            MarkdownElementTypes.PARAGRAPH -> mapParagraph(node, depth + 1)

            MarkdownElementTypes.UNORDERED_LIST -> OrcaBlock.ListBlock(
                ordered = false,
                items = node.children
                    .filter { child -> child.type == MarkdownElementTypes.LIST_ITEM }
                    .map { child -> mapListItem(child, depth + 1) },
            )

            MarkdownElementTypes.ORDERED_LIST -> OrcaBlock.ListBlock(
                ordered = true,
                items = node.children
                    .filter { child -> child.type == MarkdownElementTypes.LIST_ITEM }
                    .map { child -> mapListItem(child, depth + 1) },
                startNumber = node.extractOrderedListStart(source),
            )

            MarkdownElementTypes.BLOCK_QUOTE -> OrcaBlock.Quote(
                blocks = node.children
                    .mapNotNull { child -> mapBlock(child, depth + 1) },
            )

            MarkdownElementTypes.CODE_FENCE -> mapCodeFence(node)
            MarkdownElementTypes.CODE_BLOCK -> mapIndentedCodeBlock(node)
            MarkdownElementTypes.HTML_BLOCK -> mapHtmlBlock(node)
            GFMElementTypes.TABLE -> mapTable(node, depth + 1)
            MarkdownTokenTypes.HORIZONTAL_RULE -> OrcaBlock.ThematicBreak
            else -> mapHeading(node, depth + 1)
        }
    }

    private fun mapHeading(node: ASTNode, depth: Int): OrcaBlock.Heading? {
        val level = node.type.toHeadingLevelOrNull() ?: return null
        val contentHolder = node.findChildOfType(MarkdownTokenTypes.ATX_CONTENT)
            ?: node.findChildOfType(MarkdownTokenTypes.SETEXT_CONTENT)
            ?: return OrcaBlock.Heading(level = level, content = emptyList())
        val content = mapInlineContainer(
            container = contentHolder,
            depth = depth + 1,
            trimEdges = true,
        )
        return OrcaBlock.Heading(
            level = level,
            content = content,
        )
    }

    private fun mapParagraph(node: ASTNode, depth: Int): OrcaBlock {
        val content = mapInlineContainer(
            container = node,
            depth = depth + 1,
        )
        val standaloneImage = content.toStandaloneImageOrNull()
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

    private fun mapTable(tableNode: ASTNode, depth: Int): OrcaBlock.Table? {
        if (isDepthExceeded(depth)) {
            return null
        }

        val alignments = parseTableAlignments(
            tableNode.findChildOfType(GFMTokenTypes.TABLE_SEPARATOR)
                ?.getTextInNode(source)
                ?.toString()
                .orEmpty(),
        )
        val header = tableNode.children
            .firstOrNull { child -> child.type == GFMElementTypes.HEADER }
            ?.let { headerNode -> mapTableRow(headerNode, alignments, depth + 1) }
            .orEmpty()
        val rows = tableNode.children
            .filter { child -> child.type == GFMElementTypes.ROW }
            .map { rowNode -> mapTableRow(rowNode, alignments, depth + 1) }

        if (header.isEmpty() && rows.isEmpty()) {
            return null
        }

        return OrcaBlock.Table(
            header = header,
            rows = rows,
        )
    }

    private fun mapTableRow(
        rowNode: ASTNode,
        alignments: List<OrcaTableAlignment?>,
        depth: Int,
    ): List<OrcaTableCell> {
        if (isDepthExceeded(depth)) {
            return emptyList()
        }

        return rowNode.children
            .filter { child -> child.type == GFMTokenTypes.CELL }
            .mapIndexed { index, cellNode ->
                OrcaTableCell(
                    content = mapInlineContainer(
                        container = cellNode,
                        depth = depth + 1,
                        trimEdges = true,
                    ),
                    alignment = alignments.getOrNull(index),
                )
            }
    }

    private fun mapListItem(node: ASTNode, depth: Int): OrcaListItem {
        val taskState = node.findChildOfType(GFMTokenTypes.CHECK_BOX)?.toTaskState(source)
        val blocks = node.children
            .mapNotNull { child ->
                when (child.type) {
                    GFMTokenTypes.CHECK_BOX,
                    MarkdownTokenTypes.LIST_BULLET,
                    MarkdownTokenTypes.LIST_NUMBER,
                    MarkdownTokenTypes.WHITE_SPACE,
                    MarkdownTokenTypes.EOL,
                        -> null

                    else -> mapBlock(child, depth + 1)
                }
            }
        return OrcaListItem(
            blocks = blocks,
            taskState = taskState,
        )
    }

    private fun mapCodeFence(node: ASTNode): OrcaBlock.CodeBlock {
        val indentBefore = node.getTextInNode(source).commonPrefixWith(" ".repeat(10)).length
        val language = node.findChildOfType(MarkdownTokenTypes.FENCE_LANG)
            ?.getTextInNode(source)
            ?.toString()
            ?.takeLanguageOrNull()

        var state = 0
        var lastChildWasContent = false
        val builder = StringBuilder()
        val childrenToConsider = if (node.children.lastOrNull()?.type == MarkdownTokenTypes.CODE_FENCE_END) {
            node.children.dropLast(1)
        } else {
            node.children
        }

        for (child in childrenToConsider) {
            if (state == 1 && child.type in listOf(MarkdownTokenTypes.CODE_FENCE_CONTENT, MarkdownTokenTypes.EOL)) {
                val piece = if (child.type == MarkdownTokenTypes.EOL) {
                    "\n"
                } else {
                    child.getTextInNode(source)
                        .toString()
                        .trimIndentPrefix(indentBefore)
                }
                builder.append(piece)
                lastChildWasContent = child.type == MarkdownTokenTypes.CODE_FENCE_CONTENT
            }
            if (state == 0 && child.type == MarkdownTokenTypes.EOL) {
                state = 1
            }
        }
        if (lastChildWasContent) {
            builder.append('\n')
        }

        return OrcaBlock.CodeBlock(
            code = builder.toString().trimEnd('\n'),
            language = language,
        )
    }

    private fun mapIndentedCodeBlock(node: ASTNode): OrcaBlock.CodeBlock {
        val builder = StringBuilder()
        for (child in node.children) {
            when (child.type) {
                MarkdownTokenTypes.CODE_LINE -> builder.append(
                    child.getTextInNode(source)
                        .toString()
                        .trimIndentPrefix(4),
                )

                MarkdownTokenTypes.EOL -> builder.append('\n')
            }
        }

        return OrcaBlock.CodeBlock(
            code = builder.toString().trimEnd('\n'),
            language = null,
        )
    }

    private fun mapHtmlBlock(node: ASTNode): OrcaBlock.HtmlBlock {
        val html = buildString {
            node.children.forEach { child ->
                if (child.type == MarkdownTokenTypes.HTML_BLOCK_CONTENT || child.type == MarkdownTokenTypes.EOL) {
                    append(child.getTextInNode(source))
                }
            }
        }
        return OrcaBlock.HtmlBlock(html = html.trimEnd('\n'))
    }

    private fun mapInlineContainer(
        container: ASTNode,
        depth: Int,
        trimEdges: Boolean = false,
    ): List<OrcaInline> {
        return mapInlineNodes(
            nodes = container.children,
            depth = depth + 1,
            trimEdges = trimEdges,
        )
    }

    private fun mapInlineNodes(
        nodes: List<ASTNode>,
        depth: Int,
        trimEdges: Boolean = false,
    ): List<OrcaInline> {
        if (isDepthExceeded(depth)) {
            return emptyList()
        }

        val raw = mutableListOf<OrcaInline>()
        var index = 0
        var previousWasHardLineBreak = false
        while (index < nodes.size) {
            val node = nodes[index]
            val next = nodes.getOrNull(index + 1)

            if (next != null) {
                val maybeConsumed = consumeInlineFootnotePair(
                    currentNode = node,
                    nextNode = next,
                    depth = depth + 1,
                    output = raw,
                )
                if (maybeConsumed) {
                    previousWasHardLineBreak = false
                    index += 2
                    continue
                }
            }

            if (node.type == MarkdownTokenTypes.EOL && previousWasHardLineBreak) {
                previousWasHardLineBreak = false
                index += 1
                continue
            }

            raw += mapInline(node, depth + 1).toList()
            previousWasHardLineBreak = node.type == MarkdownTokenTypes.HARD_LINE_BREAK
            index += 1
        }

        val mergedRaw = raw.mergeAdjacentText()
        val withFootnotes = processFootnoteSyntax(mergedRaw, depth + 1)
            .mergeAdjacentText()
        val normalized = if (trimEdges) {
            withFootnotes.trimEdgeWhitespace()
        } else {
            withFootnotes
        }
        return normalized.mergeAdjacentText()
    }

    private fun mapInline(node: ASTNode, depth: Int): Sequence<OrcaInline> {
        if (isDepthExceeded(depth)) {
            return emptySequence()
        }

        return when (node.type) {
            MarkdownElementTypes.STRONG -> sequenceOf(
                OrcaInline.Bold(
                    content = mapInlineSlice(node, from = 2, to = -2, depth = depth + 1),
                ),
            )

            MarkdownElementTypes.EMPH -> sequenceOf(
                OrcaInline.Italic(
                    content = mapInlineSlice(node, from = 1, to = -1, depth = depth + 1),
                ),
            )

            GFMElementTypes.STRIKETHROUGH -> sequenceOf(
                OrcaInline.Strikethrough(
                    content = mapEqualDelimitedInline(node, delimiterType = GFMTokenTypes.TILDE, depth = depth + 1),
                ),
            )

            MarkdownElementTypes.CODE_SPAN -> sequenceOf(
                OrcaInline.InlineCode(code = mapCodeSpan(node)),
            )

            MarkdownElementTypes.INLINE_LINK,
            MarkdownElementTypes.FULL_REFERENCE_LINK,
            MarkdownElementTypes.SHORT_REFERENCE_LINK,
                -> mapInlineLink(node, depth + 1)?.let { link -> sequenceOf(link) } ?: mapInlineContainer(node, depth + 1).asSequence()

            MarkdownElementTypes.IMAGE -> mapInlineImage(node, depth + 1)
                ?.let { image -> sequenceOf(image) }
                ?: mapInlineContainer(node, depth + 1).asSequence()

            MarkdownElementTypes.AUTOLINK -> sequenceOf(mapAutolink(node))
            GFMTokenTypes.GFM_AUTOLINK -> sequenceOf(mapGfmAutolink(node))
            MarkdownTokenTypes.HTML_TAG -> sequenceOf(
                OrcaInline.HtmlInline(html = node.getTextInNode(source).toString()),
            )

            MarkdownTokenTypes.EOL,
            MarkdownTokenTypes.HARD_LINE_BREAK,
                -> sequenceOf(OrcaInline.Text("\n"))

            else -> {
                if (node.children.isNotEmpty()) {
                    mapInlineContainer(node, depth + 1).asSequence()
                } else {
                    sequenceOf(OrcaInline.Text(node.getTextInNode(source).toString()))
                }
            }
        }
    }

    private fun mapInlineSlice(
        node: ASTNode,
        from: Int,
        to: Int,
        depth: Int,
    ): List<OrcaInline> {
        val fromIndex = from.coerceIn(0, node.children.size)
        val toIndex = (node.children.size + to).coerceIn(fromIndex, node.children.size)
        return mapInlineNodes(
            nodes = node.children.subList(fromIndex, toIndex),
            depth = depth + 1,
        )
    }

    private fun mapEqualDelimitedInline(
        node: ASTNode,
        delimiterType: IElementType,
        depth: Int,
    ): List<OrcaInline> {
        if (node.children.isEmpty()) {
            return emptyList()
        }

        var left = 0
        var right = node.children.size - 1
        while (
            left <= right &&
            node.children[left].type == delimiterType &&
            node.children[right].type == delimiterType
        ) {
            left += 1
            right -= 1
            if (left >= right) break
        }

        if (right < left) {
            return emptyList()
        }

        return mapInlineNodes(
            nodes = node.children.subList(left, right + 1),
            depth = depth + 1,
        )
    }

    private fun mapInlineLink(node: ASTNode, depth: Int): OrcaInline.Link? {
        val info = resolveLinkInfo(node) ?: return null
        val content = mapInlineSlice(
            node = info.labelNode,
            from = 1,
            to = -1,
            depth = depth + 1,
        )
        return OrcaInline.Link(
            destination = info.destination,
            content = content,
        )
    }

    private fun mapInlineImage(node: ASTNode, depth: Int): OrcaInline.Image? {
        val linkNode = node.findChildOfType(MarkdownElementTypes.INLINE_LINK)
            ?: node.findChildOfType(MarkdownElementTypes.FULL_REFERENCE_LINK)
            ?: node.findChildOfType(MarkdownElementTypes.SHORT_REFERENCE_LINK)
            ?: return null
        val info = resolveLinkInfo(linkNode) ?: return null
        val alt = mapInlineSlice(
            node = info.labelNode,
            from = 1,
            to = -1,
            depth = depth + 1,
        ).toPlainText()
            .trim()
            .takeIf { value -> value.isNotEmpty() }

        return OrcaInline.Image(
            source = info.destination,
            alt = alt,
            title = info.title?.takeIf { value -> value.isNotBlank() },
        )
    }

    private fun resolveLinkInfo(node: ASTNode): ResolvedLinkInfo? {
        return when (node.type) {
            MarkdownElementTypes.INLINE_LINK -> {
                val label = node.findChildOfType(MarkdownElementTypes.LINK_TEXT) ?: return null
                val destination = node.findChildOfType(MarkdownElementTypes.LINK_DESTINATION)
                    ?.getTextInNode(source)
                    ?.let { text -> LinkMap.normalizeDestination(text, true).toString() }
                    ?: ""
                val title = node.findChildOfType(MarkdownElementTypes.LINK_TITLE)
                    ?.getTextInNode(source)
                    ?.let { text -> LinkMap.normalizeTitle(text).toString() }
                ResolvedLinkInfo(
                    labelNode = label,
                    destination = destination,
                    title = title,
                )
            }

            MarkdownElementTypes.FULL_REFERENCE_LINK,
            MarkdownElementTypes.SHORT_REFERENCE_LINK,
                -> {
                    val label = node.children.firstOrNull { child -> child.type == MarkdownElementTypes.LINK_LABEL }
                        ?: return null
                    val linkInfo = linkMap.getLinkInfo(label.getTextInNode(source)) ?: return null
                    val contentNode = node.children.firstOrNull { child -> child.type == MarkdownElementTypes.LINK_TEXT } ?: label
                    ResolvedLinkInfo(
                        labelNode = contentNode,
                        destination = linkInfo.destination.toString(),
                        title = linkInfo.title?.toString(),
                    )
                }

            else -> null
        }
    }

    private fun mapAutolink(node: ASTNode): OrcaInline.Link {
        val raw = node.getTextInNode(source).toString()
        val normalizedDestination = LinkMap.normalizeDestination(raw, false).toString()
        val content = raw.removePrefix("<").removeSuffix(">")
        return OrcaInline.Link(
            destination = normalizedDestination,
            content = listOf(OrcaInline.Text(content)),
        )
    }

    private fun mapGfmAutolink(node: ASTNode): OrcaInline.Link {
        val linkText = node.getTextInNode(source).toString()
        val destination = if (hasSchema(linkText)) {
            linkText
        } else {
            "http://$linkText"
        }
        return OrcaInline.Link(
            destination = destination,
            content = listOf(OrcaInline.Text(linkText)),
        )
    }

    private fun hasSchema(linkText: CharSequence): Boolean {
        val index = linkText.indexOf('/')
        if (index == -1) return false
        return index != 0 &&
            index + 1 < linkText.length &&
            linkText[index - 1] == ':' &&
            linkText[index + 1] == '/'
    }

    private fun mapCodeSpan(node: ASTNode): String {
        if (node.children.size <= 2) {
            return ""
        }

        val raw = node.children
            .subList(1, node.children.size - 1)
            .joinToString(separator = "") { child -> child.getTextInNode(source).toString() }
            .replace("\\r\\n?|\\n".toRegex(), " ")
        return if (raw.isBlank()) raw else raw.removeSurrounding(" ", " ")
    }

    private fun processFootnoteSyntax(
        inlines: List<OrcaInline>,
        depth: Int,
    ): List<OrcaInline> {
        if (isDepthExceeded(depth)) {
            return inlines
        }

        return inlines.flatMap { inline ->
            when (inline) {
                is OrcaInline.Text -> parseFootnotesFromText(
                    text = inline.text,
                    depth = depth + 1,
                )

                is OrcaInline.Bold -> listOf(
                    inline.copy(
                        content = processFootnoteSyntax(inline.content, depth + 1),
                    ),
                )

                is OrcaInline.Italic -> listOf(
                    inline.copy(
                        content = processFootnoteSyntax(inline.content, depth + 1),
                    ),
                )

                is OrcaInline.Strikethrough -> listOf(
                    inline.copy(
                        content = processFootnoteSyntax(inline.content, depth + 1),
                    ),
                )

                is OrcaInline.Link -> listOf(
                    inline.copy(
                        content = processFootnoteSyntax(inline.content, depth + 1),
                    ),
                )

                else -> listOf(inline)
            }
        }
    }

    private fun parseFootnotesFromText(
        text: String,
        depth: Int,
    ): List<OrcaInline> {
        val result = mutableListOf<OrcaInline>()
        val buffer = StringBuilder()
        var index = 0

        fun flushBuffer() {
            if (buffer.isNotEmpty()) {
                result += OrcaInline.Text(buffer.toString())
                buffer.clear()
            }
        }

        while (index < text.length) {
            if (text.startsWith("[^", startIndex = index)) {
                val closing = text.indexOf(']', startIndex = index + 2)
                if (closing > index + 2) {
                    val label = text.substring(index + 2, closing).trim()
                    if (label.isNotEmpty()) {
                        flushBuffer()
                        result += OrcaInline.FootnoteReference(label = label)
                        index = closing + 1
                        continue
                    }
                }
            }

            if (text.startsWith("^[", startIndex = index)) {
                val inlineFootnote = parseInlineFootnoteContent(text, start = index + 2)
                if (inlineFootnote != null) {
                    flushBuffer()
                    val label = "$INLINE_FOOTNOTE_LABEL_PREFIX${inlineFootnotes.size + 1}"
                    val content = mapInlineOnly(
                        markdown = inlineFootnote.content,
                        depth = depth + 1,
                    )
                    inlineFootnotes += OrcaFootnoteDefinition(
                        label = label,
                        blocks = if (content.isEmpty()) {
                            emptyList()
                        } else {
                            listOf(OrcaBlock.Paragraph(content = content))
                        },
                    )
                    result += OrcaInline.FootnoteReference(label = label)
                    index = inlineFootnote.closingIndex + 1
                    continue
                }
            }

            buffer.append(text[index])
            index += 1
        }

        flushBuffer()
        return result
    }

    private fun mapInlineOnly(
        markdown: String,
        depth: Int,
    ): List<OrcaInline> {
        if (markdown.isBlank()) {
            return emptyList()
        }
        if (isDepthExceeded(depth)) {
            return emptyList()
        }

        val inlineRoot = parser.parseInline(
            root = MarkdownElementTypes.PARAGRAPH,
            text = markdown,
            textStart = 0,
            textEnd = markdown.length,
        )
        val inlineMapper = IntellijTreeMapper(
            source = markdown,
            parser = parser,
            linkMap = LinkMap.buildLinkMap(inlineRoot, markdown),
            maxTreeDepth = maxTreeDepth,
            depthLimitReporter = depthLimitReporter,
        )
        return inlineMapper.mapInlineContainer(
            container = inlineRoot,
            depth = depth + 1,
        )
    }

    private fun parseInlineFootnoteContent(
        text: String,
        start: Int,
    ): ParsedInlineFootnote? {
        var index = start
        var nested = 1
        while (index < text.length) {
            when (text[index]) {
                '\\' -> {
                    index += if (index + 1 < text.length) 2 else 1
                    continue
                }

                '[' -> nested += 1
                ']' -> {
                    nested -= 1
                    if (nested == 0) {
                        return ParsedInlineFootnote(
                            content = text.substring(start, index),
                            closingIndex = index,
                        )
                    }
                }
            }
            index += 1
        }
        return null
    }

    private fun consumeInlineFootnotePair(
        currentNode: ASTNode,
        nextNode: ASTNode,
        depth: Int,
        output: MutableList<OrcaInline>,
    ): Boolean {
        if (nextNode.type != MarkdownElementTypes.SHORT_REFERENCE_LINK) {
            return false
        }

        val currentText = currentNode.getTextInNode(source).toString()
        if (!currentText.endsWith('^')) {
            return false
        }

        val labelNode = nextNode.findChildOfType(MarkdownElementTypes.LINK_LABEL) ?: return false
        if (linkMap.getLinkInfo(labelNode.getTextInNode(source)) != null) {
            return false
        }

        val prefix = currentText.dropLast(1)
        if (prefix.isNotEmpty()) {
            output += OrcaInline.Text(prefix)
        }

        val content = mapInlineSlice(
            node = labelNode,
            from = 1,
            to = -1,
            depth = depth + 1,
        )
        val label = "$INLINE_FOOTNOTE_LABEL_PREFIX${inlineFootnotes.size + 1}"
        inlineFootnotes += OrcaFootnoteDefinition(
            label = label,
            blocks = if (content.isEmpty()) {
                emptyList()
            } else {
                listOf(OrcaBlock.Paragraph(content = content))
            },
        )
        output += OrcaInline.FootnoteReference(label = label)
        return true
    }

    private fun isDepthExceeded(depth: Int): Boolean {
        if (depth <= maxTreeDepth) {
            return false
        }
        depthLimitReporter.report(depth)
        return true
    }
}

private fun IElementType.toHeadingLevelOrNull(): Int? {
    return when (this) {
        MarkdownElementTypes.ATX_1,
        MarkdownElementTypes.SETEXT_1,
            -> 1

        MarkdownElementTypes.ATX_2,
        MarkdownElementTypes.SETEXT_2,
            -> 2

        MarkdownElementTypes.ATX_3 -> 3
        MarkdownElementTypes.ATX_4 -> 4
        MarkdownElementTypes.ATX_5 -> 5
        MarkdownElementTypes.ATX_6 -> 6
        else -> null
    }
}

private fun ASTNode.extractOrderedListStart(source: String): Int {
    val firstItem = children.firstOrNull { child -> child.type == MarkdownElementTypes.LIST_ITEM } ?: return 1
    val numberToken = firstItem.findChildOfType(MarkdownTokenTypes.LIST_NUMBER) ?: return 1
    val value = numberToken.getTextInNode(source)
        .toString()
        .trim()
        .takeWhile { char -> char.isDigit() }
        .toIntOrNull()
    return value ?: 1
}

private fun ASTNode.toTaskState(source: String): OrcaTaskState? {
    val text = getTextInNode(source).toString()
    if (text.length < 2) return null
    return if (text[1] == 'x' || text[1] == 'X') {
        OrcaTaskState.CHECKED
    } else if (text[1].isWhitespace()) {
        OrcaTaskState.UNCHECKED
    } else {
        null
    }
}

private fun parseTableAlignments(separatorRow: String): List<OrcaTableAlignment?> {
    if (separatorRow.isBlank()) {
        return emptyList()
    }

    val cells = separatorRow.split('|')
    return cells.mapIndexedNotNull { index, cell ->
        if (cell.isBlank() && index !in 1 until cells.lastIndex) {
            return@mapIndexedNotNull null
        }
        val trimmed = cell.trim()
        when {
            trimmed.startsWith(':') && trimmed.endsWith(':') -> OrcaTableAlignment.CENTER
            trimmed.startsWith(':') -> OrcaTableAlignment.LEFT
            trimmed.endsWith(':') -> OrcaTableAlignment.RIGHT
            else -> null
        }
    }
}

private fun List<OrcaInline>.mergeAdjacentText(): List<OrcaInline> {
    if (isEmpty()) return emptyList()

    val result = mutableListOf<OrcaInline>()
    var textBuffer: StringBuilder? = null

    fun flush() {
        val current = textBuffer ?: return
        if (current.isNotEmpty()) {
            result += OrcaInline.Text(current.toString())
        }
        textBuffer = null
    }

    for (inline in this) {
        if (inline is OrcaInline.Text) {
            if (inline.text == "\n") {
                flush()
                result += inline
                continue
            }
            val buffer = textBuffer ?: StringBuilder().also { created -> textBuffer = created }
            buffer.append(inline.text)
        } else {
            flush()
            result += inline
        }
    }
    flush()
    return result
}

private fun List<OrcaInline>.trimEdgeWhitespace(): List<OrcaInline> {
    if (isEmpty()) return emptyList()
    val mutable = toMutableList()

    while (mutable.firstOrNull() is OrcaInline.Text && (mutable.first() as OrcaInline.Text).text.isEmpty()) {
        mutable.removeAt(0)
    }
    while (mutable.lastOrNull() is OrcaInline.Text && (mutable.last() as OrcaInline.Text).text.isEmpty()) {
        mutable.removeAt(mutable.lastIndex)
    }
    if (mutable.isEmpty()) {
        return emptyList()
    }

    val first = mutable.first()
    if (first is OrcaInline.Text) {
        val trimmed = first.text.trimStart()
        if (trimmed.isEmpty()) {
            mutable.removeAt(0)
        } else if (trimmed != first.text) {
            mutable[0] = OrcaInline.Text(trimmed)
        }
    }
    if (mutable.isEmpty()) {
        return emptyList()
    }

    val lastIndex = mutable.lastIndex
    val last = mutable[lastIndex]
    if (last is OrcaInline.Text) {
        val trimmed = last.text.trimEnd()
        if (trimmed.isEmpty()) {
            mutable.removeAt(lastIndex)
        } else if (trimmed != last.text) {
            mutable[lastIndex] = OrcaInline.Text(trimmed)
        }
    }
    return mutable
}

private fun List<OrcaInline>.toStandaloneImageOrNull(): OrcaInline.Image? {
    val significant = filter { inline ->
        inline !is OrcaInline.Text || inline.text.isNotBlank()
    }
    return significant.singleOrNull() as? OrcaInline.Image
}

private fun String.trimIndentPrefix(count: Int): String {
    if (count <= 0 || isEmpty()) return this
    var index = 0
    var remaining = count
    while (index < length && remaining > 0 && this[index] == ' ') {
        index += 1
        remaining -= 1
    }
    return substring(index)
}

private fun String.takeLanguageOrNull(): String? {
    val firstToken = trim().split(' ').firstOrNull()?.trim()
    return firstToken?.takeIf { it.isNotEmpty() }
}

private fun List<OrcaInline>.toPlainText(): String {
    return buildString {
        for (inline in this@toPlainText) {
            when (inline) {
                is OrcaInline.Text -> append(inline.text)
                is OrcaInline.Bold -> append(inline.content.toPlainText())
                is OrcaInline.Italic -> append(inline.content.toPlainText())
                is OrcaInline.Strikethrough -> append(inline.content.toPlainText())
                is OrcaInline.InlineCode -> append(inline.code)
                is OrcaInline.Link -> append(inline.content.toPlainText().ifEmpty { inline.destination })
                is OrcaInline.Image -> append(inline.alt ?: "")
                is OrcaInline.FootnoteReference -> append("[${inline.label}]")
                is OrcaInline.HtmlInline -> append(htmlInlineToPlainText(inline.html))
            }
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
        .replace("&amp;", "&")
        .replace("&nbsp;", " ")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
}
