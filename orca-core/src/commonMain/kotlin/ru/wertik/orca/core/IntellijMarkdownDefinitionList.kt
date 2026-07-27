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

/** Shortest possible definition line: `:`, one whitespace character, one content character. */
private const val MIN_DEFINITION_LINE_LENGTH = 3

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
 *
 * Runs in linear time: definition lines are located up front ([DefinitionLineIndex])
 * and a list is only *attempted* on the single line that could open one binding to the
 * next definition line. Probing every line instead made documents without a definition
 * list (that is: almost every real document) quadratic in the number of lines.
 */
internal fun extractDefinitionLists(markdown: String): DefinitionListExtraction {
    if (!mayContainDefinitionLine(markdown)) {
        return DefinitionListExtraction(markdown = markdown, definitionLists = emptyList())
    }

    val lines = markdown.split('\n')
    val lineIndex = DefinitionLineIndex(lines)
    if (lineIndex.nextDefinitionFrom(0) < 0) {
        return DefinitionListExtraction(markdown = markdown, definitionLists = emptyList())
    }

    val bodyLines = ArrayList<String>(lines.size)
    val definitionLists = mutableListOf<DefinitionListSource>()
    var cursor = 0

    while (cursor < lines.size) {
        val definitionLine = lineIndex.nextDefinitionFrom(cursor)
        if (definitionLine < 0) {
            while (cursor < lines.size) {
                bodyLines += lines[cursor]
                cursor += 1
            }
            break
        }

        val startLine = definitionListStart(
            lines = lines,
            lineIndex = lineIndex,
            lowerBound = cursor,
            definitionLine = definitionLine,
        )
        val parsed = if (startLine < 0) null else tryParseDefinitionList(lines, lineIndex, startLine)
        if (parsed == null) {
            // No line up to and including this definition line can open a list: copy verbatim.
            while (cursor <= definitionLine) {
                bodyLines += lines[cursor]
                cursor += 1
            }
            continue
        }

        while (cursor < startLine) {
            bodyLines += lines[cursor]
            cursor += 1
        }
        bodyLines += "<!--orca:deflist:${definitionLists.size}-->"
        definitionLists += parsed.first
        cursor = maxOf(parsed.second, startLine + 1)
    }

    return DefinitionListExtraction(
        markdown = bodyLines.joinToString("\n"),
        definitionLists = definitionLists,
    )
}

/**
 * Cheap pre-scan of the raw source: a definition list needs a line starting with `:`
 * followed by whitespace. Documents without one skip line splitting altogether.
 *
 * Deliberately a superset of [DEFINITION_LINE_REGEX] (every regex match is a candidate
 * here too), so it can only ever cost one extra pass, never a missed list.
 */
private fun mayContainDefinitionLine(markdown: String): Boolean {
    var lineStart = 0
    while (lineStart < markdown.length) {
        if (markdown[lineStart] == ':' &&
            lineStart + 1 < markdown.length &&
            markdown[lineStart + 1].isWhitespace()
        ) {
            return true
        }
        val newline = markdown.indexOf('\n', startIndex = lineStart)
        if (newline < 0) return false
        lineStart = newline + 1
    }
    return false
}

/**
 * Per-line facts the scanner would otherwise recompute on every probe. Built in a
 * single pass; every lookup afterwards is O(1).
 */
private class DefinitionLineIndex(lines: List<String>) {
    private val size = lines.size

    /** Definition body (regex group 1) per line, or `null` when the line is not one. */
    private val bodies = arrayOfNulls<String>(lines.size)

    /** Smallest definition line index `>= i`, or `-1`; the slot at [size] is a sentinel. */
    private val nextDefinition = IntArray(lines.size + 1) { -1 }

    /** Smallest non-blank line index `>= i`, or `-1`; the slot at [size] is a sentinel. */
    private val nextNonBlank = IntArray(lines.size + 1) { -1 }

    init {
        for (index in lines.indices) {
            bodies[index] = definitionLineBody(lines[index])
        }
        for (index in lines.indices.reversed()) {
            nextDefinition[index] = if (bodies[index] != null) index else nextDefinition[index + 1]
            nextNonBlank[index] = if (lines[index].isNotBlank()) index else nextNonBlank[index + 1]
        }
    }

    fun isDefinition(index: Int): Boolean = index in 0 until size && bodies[index] != null

    fun definitionBodyAt(index: Int): String? = if (index in 0 until size) bodies[index] else null

    fun nextDefinitionFrom(index: Int): Int = nextDefinition[index.coerceIn(0, size)]

    fun nextNonBlankFrom(index: Int): Int = nextNonBlank[index.coerceIn(0, size)]
}

private fun definitionLineBody(line: String): String? {
    if (line.length < MIN_DEFINITION_LINE_LENGTH) return null
    if (line[0] != ':' || !line[1].isWhitespace()) return null
    return DEFINITION_LINE_REGEX.matchEntire(line)?.groupValues?.get(1)
}

/**
 * Line at which a definition list binding to [definitionLine] has to start, or `-1`
 * when no line in `[lowerBound, definitionLine]` can open one.
 *
 * A list starts at the first line of the term run preceding the definition line,
 * optionally separated from it by a single blank line. Any other start would either
 * find no term (blank or definition line) or land inside that same run, where the
 * scanner produces exactly what it produces for the run start.
 */
private fun definitionListStart(
    lines: List<String>,
    lineIndex: DefinitionLineIndex,
    lowerBound: Int,
    definitionLine: Int,
): Int {
    var candidate = definitionLine - 1
    if (candidate >= lowerBound && lines[candidate].isBlank()) candidate -= 1
    if (candidate < lowerBound) return -1
    if (lines[candidate].isBlank() || lineIndex.isDefinition(candidate)) return -1

    while (candidate - 1 >= lowerBound &&
        lines[candidate - 1].isNotBlank() &&
        !lineIndex.isDefinition(candidate - 1)
    ) {
        candidate -= 1
    }
    return candidate
}

/**
 * Try to parse a definition list starting at [startIndex].
 * Returns the parsed [DefinitionListSource] and the next line index to continue from,
 * or null if no definition list starts here.
 */
private fun tryParseDefinitionList(
    lines: List<String>,
    lineIndex: DefinitionLineIndex,
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
            if (line.isBlank() || lineIndex.isDefinition(index)) break
            termLines += line
            index += 1
        }

        // Skip optional blank line between term and definitions.
        if (index < lines.size && lines[index].isBlank()) {
            // Only skip if next non-blank line is a definition.
            if (index + 1 < lines.size && lineIndex.isDefinition(index + 1)) {
                index += 1
            }
        }

        // Collect definition lines.
        val definitions = mutableListOf<String>()
        while (index < lines.size) {
            val definitionBody = lineIndex.definitionBodyAt(index)
            if (definitionBody != null) {
                val defLines = mutableListOf(definitionBody)
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
            val nextNonBlank = lineIndex.nextNonBlankFrom(index + 1)
            if (nextNonBlank >= 0) {
                val afterThat = nextNonBlank + 1
                if (afterThat < lines.size && lineIndex.isDefinition(afterThat)) {
                    index += 1
                    continue
                }
                // Check if the non-blank line itself is a definition (term was already consumed).
                if (lineIndex.isDefinition(nextNonBlank)) {
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

/**
 * Strip one level of continuation indent (tab or 4+ spaces).
 * Returns the unindented line, or null if the line is not indented.
 *
 * Uses 4-space indent to match footnote continuation behaviour
 * ([stripFootnoteContinuationIndent]) and the PHP Markdown Extra spec.
 */
private fun stripDefinitionContinuation(line: String): String? {
    if (line.startsWith("\t")) return line.removePrefix("\t")
    if (line.startsWith("    ")) return line.substring(4)
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

internal fun parseInlinesFromMarkdown(
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
