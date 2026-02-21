package ru.wertik.orca.core

import org.intellij.markdown.parser.LinkMap
import org.intellij.markdown.parser.MarkdownParser

internal data class DefinitionListExtraction(
    val markdown: String,
    val definitionLists: List<DefinitionListSource>,
)

internal data class DefinitionListSource(
    val items: List<DefinitionListItemSource>,
    /** Line index in the original markdown where this definition list starts. */
    val startLine: Int,
)

internal data class DefinitionListItemSource(
    val term: String,
    val definitions: List<String>,
)

private val DEFINITION_LINE_REGEX = Regex("""^:\s+(.+)$""")

/**
 * Extract definition lists from markdown source.
 *
 * Definition list syntax (PHP Markdown Extra):
 * ```
 * Term
 * : Definition one
 * : Definition two
 * ```
 *
 * A term is any non-blank line that is NOT a definition line and is followed
 * by one or more definition lines (`: ...`). Multiple terms can share definitions,
 * and multiple definitions can follow a single term.
 *
 * The extracted definition lists are replaced with placeholder paragraphs
 * (`__orca_deflist_N__`) so the main parser skips them. After parsing, placeholders
 * are replaced with the actual [OrcaBlock.DefinitionList] nodes.
 */
internal fun extractDefinitionLists(markdown: String): DefinitionListExtraction {
    val lines = markdown.split('\n')
    val bodyLines = mutableListOf<String>()
    val definitionLists = mutableListOf<DefinitionListSource>()
    var index = 0

    while (index < lines.size) {
        val defList = tryParseDefinitionList(lines, index)
        if (defList != null) {
            val placeholder = "<!--orca:deflist:${definitionLists.size}-->"
            definitionLists += defList.first
            bodyLines += placeholder
            index = defList.second
        } else {
            bodyLines += lines[index]
            index += 1
        }
    }

    return DefinitionListExtraction(
        markdown = bodyLines.joinToString("\n"),
        definitionLists = definitionLists,
    )
}

/**
 * Try to parse a definition list starting at [startIndex].
 * Returns the parsed [DefinitionListSource] and the next line index to continue from,
 * or null if no definition list starts here.
 */
private fun tryParseDefinitionList(
    lines: List<String>,
    startIndex: Int,
): Pair<DefinitionListSource, Int>? {
    // We need at least a term line followed by a definition line.
    if (startIndex + 1 >= lines.size) return null

    val items = mutableListOf<DefinitionListItemSource>()
    var index = startIndex

    while (index < lines.size) {
        // Collect term lines (one or more non-blank, non-definition lines).
        val termLines = mutableListOf<String>()
        while (index < lines.size) {
            val line = lines[index]
            if (line.isBlank() || DEFINITION_LINE_REGEX.matches(line)) break
            termLines += line
            index += 1
        }

        // Skip optional blank line between term and definitions.
        if (index < lines.size && lines[index].isBlank()) {
            // Only skip if next non-blank line is a definition.
            if (index + 1 < lines.size && DEFINITION_LINE_REGEX.matches(lines[index + 1])) {
                index += 1
            }
        }

        // Collect definition lines.
        val definitions = mutableListOf<String>()
        while (index < lines.size) {
            val match = DEFINITION_LINE_REGEX.matchEntire(lines[index])
            if (match != null) {
                val defLines = mutableListOf(match.groupValues[1])
                index += 1
                // Collect continuation lines (indented by 2+ spaces or tab).
                while (index < lines.size) {
                    val cont = lines[index]
                    val stripped = stripDefinitionContinuation(cont)
                    if (stripped != null) {
                        defLines += stripped
                        index += 1
                    } else if (cont.isBlank() && index + 1 < lines.size) {
                        val next = lines[index + 1]
                        if (stripDefinitionContinuation(next) != null) {
                            defLines += ""
                            index += 1
                        } else {
                            break
                        }
                    } else {
                        break
                    }
                }
                definitions += defLines.joinToString("\n").trimEnd()
            } else {
                break
            }
        }

        if (termLines.isEmpty() || definitions.isEmpty()) {
            // If we already have items, this is the end of the definition list.
            // Return any consumed terms back to the body if they weren't part of a def list.
            if (items.isNotEmpty()) {
                // Put the unused term lines back by adjusting index.
                return Pair(
                    DefinitionListSource(items = items, startLine = startIndex),
                    index - termLines.size,
                )
            }
            return null
        }

        // Each term line becomes a separate item sharing the same definitions.
        for (termLine in termLines) {
            items += DefinitionListItemSource(
                term = termLine.trim(),
                definitions = definitions,
            )
        }

        // Skip blank lines between definition list items.
        while (index < lines.size && lines[index].isBlank()) {
            // Check if next non-blank content is another term+definition pair.
            val nextNonBlank = (index + 1 until lines.size).firstOrNull { i -> lines[i].isNotBlank() }
            if (nextNonBlank != null) {
                val afterThat = nextNonBlank + 1
                if (afterThat < lines.size && DEFINITION_LINE_REGEX.matches(lines[afterThat])) {
                    index += 1
                    continue
                }
                // Check if the non-blank line itself is a definition (term was already consumed).
                if (DEFINITION_LINE_REGEX.matches(lines[nextNonBlank])) {
                    index += 1
                    continue
                }
            }
            break
        }
    }

    if (items.isEmpty()) return null

    return Pair(
        DefinitionListSource(items = items, startLine = startIndex),
        index,
    )
}

private fun stripDefinitionContinuation(line: String): String? {
    if (line.startsWith("\t")) return line.removePrefix("\t")
    if (line.startsWith("  ")) return line.substring(2)
    return null
}

internal fun mapDefinitionList(
    parser: MarkdownParser,
    source: DefinitionListSource,
    maxTreeDepth: Int,
    depthLimitReporter: DepthLimitReporter,
): OrcaBlock.DefinitionList {
    val items = source.items.map { item ->
        val termInlines = parseInlinesFromMarkdown(
            parser = parser,
            markdown = item.term,
            maxTreeDepth = maxTreeDepth,
            depthLimitReporter = depthLimitReporter,
        )
        val definitions = item.definitions.map { defMarkdown ->
            parseBlocksFromMarkdown(
                parser = parser,
                markdown = defMarkdown,
                maxTreeDepth = maxTreeDepth,
                depthLimitReporter = depthLimitReporter,
            )
        }
        OrcaDefinitionListItem(
            term = termInlines,
            definitions = definitions,
        )
    }
    return OrcaBlock.DefinitionList(items = items)
}

private fun parseInlinesFromMarkdown(
    parser: MarkdownParser,
    markdown: String,
    maxTreeDepth: Int,
    depthLimitReporter: DepthLimitReporter,
): List<OrcaInline> {
    if (markdown.isBlank()) return emptyList()

    val root = parser.buildMarkdownTreeFromString(markdown)
    val mapper = IntellijTreeMapper(
        source = markdown,
        parser = parser,
        linkMap = LinkMap.buildLinkMap(root, markdown),
        maxTreeDepth = maxTreeDepth,
        depthLimitReporter = depthLimitReporter,
    )
    val blocks = root.children.mapNotNull { child -> mapper.mapBlock(child, depth = 0) }
    // Extract inlines from the first paragraph.
    val firstParagraph = blocks.firstOrNull() as? OrcaBlock.Paragraph
    return firstParagraph?.content ?: emptyList()
}

private fun parseBlocksFromMarkdown(
    parser: MarkdownParser,
    markdown: String,
    maxTreeDepth: Int,
    depthLimitReporter: DepthLimitReporter,
): List<OrcaBlock> {
    if (markdown.isBlank()) return emptyList()

    val root = parser.buildMarkdownTreeFromString(markdown)
    val mapper = IntellijTreeMapper(
        source = markdown,
        parser = parser,
        linkMap = LinkMap.buildLinkMap(root, markdown),
        maxTreeDepth = maxTreeDepth,
        depthLimitReporter = depthLimitReporter,
    )
    return root.children.mapNotNull { child -> mapper.mapBlock(child, depth = 0) }
}
