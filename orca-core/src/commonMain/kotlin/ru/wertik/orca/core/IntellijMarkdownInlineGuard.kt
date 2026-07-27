package ru.wertik.orca.core

internal data class InlineGuardExtraction(
    val markdown: String,
    /** Raw source of every region that was pulled out, in placeholder order. */
    val rawRegions: List<String>,
    /** Deepest run of unmatched `[` that was found, or `0` when no block hit the limit. */
    val exceededBracketDepth: Int,
    /** Deepest block nesting that was found, or `0` when no block hit the limit. */
    val exceededNestingDepth: Int,
    /** How many blocks each limit accounted for. */
    val bracketGuardedBlocks: Int,
    val nestingGuardedBlocks: Int,
)

private val RAW_TEXT_PLACEHOLDER_REGEX = Regex("""^\s*<!--orca:rawtext:(\d+)-->\s*$""")

/** Longest run of unmatched `[` a single block may contain before it is left unparsed. */
internal const val DEFAULT_MAX_INLINE_BRACKET_DEPTH = 512

/**
 * Deepest block nesting (`>` markers plus list indentation) a single block may reach
 * before it is left unparsed.
 *
 * The block parser recurses per level and its cost grows far faster than the source:
 * 128 levels of nested list items parse in ~6 ms, 512 in ~100 ms, 2 048 in ~5 s, and
 * 4 096 either exhausts a 1 MB stack or never finishes. No real document nests past a
 * handful of levels, so the limit sits two orders of magnitude above real content.
 */
internal const val DEFAULT_MAX_BLOCK_NESTING_DEPTH = 128

/**
 * Guards the parser against blocks whose shape makes it superlinear.
 *
 * Two limits, one scan:
 *
 * - **Unmatched `[`.** Link openers are resolved by backtracking, which is quadratic in
 *   the number of unclosed openers inside one block: 3 200 take ~0.2 s, 12 800 take
 *   ~2.5 s, 25 600 hang long enough to look like a deadlock. Balanced brackets are cheap.
 * - **Block nesting.** Blockquote markers and list indentation each add a level of
 *   recursion in the block parser, whose cost grows far faster than the source and which
 *   can exhaust the stack before any AST depth limit is reached.
 *
 * A block past either limit is swapped for a `<!--orca:rawtext:N-->` placeholder;
 * [resolveRawTextPlaceholders] turns it back into a plain text paragraph after parsing,
 * so no input is lost and the rest of the document is still parsed normally.
 *
 * Fenced code and `$$` math blocks are skipped: their content never reaches either
 * scanner. Inline code spans are *not* skipped — an unterminated backtick would make
 * that unsound — so a paragraph carrying a bracket bomb inside a code span degrades to
 * plain text as well.
 */
internal fun extractInlineGuardedRegions(
    markdown: String,
    maxBracketDepth: Int = DEFAULT_MAX_INLINE_BRACKET_DEPTH,
    maxNestingDepth: Int = DEFAULT_MAX_BLOCK_NESTING_DEPTH,
): InlineGuardExtraction {
    // Cheap rejection in one pass: a document without enough openers, enough quote
    // markers or a deep enough indent cannot contain a block that reaches either limit.
    if (!mayExceedGuardLimits(markdown, maxBracketDepth, maxNestingDepth)) {
        return unguarded(markdown)
    }

    val lines = markdown.split('\n')
    val bodyLines = ArrayList<String>(lines.size)
    val rawRegions = mutableListOf<String>()
    val region = mutableListOf<String>()

    var fence = ' '
    var mathOpen = false
    var pendingBrackets = 0
    var deepestBracketsInRegion = 0
    var deepestNestingInRegion = 0
    var deepestBrackets = 0
    var deepestNesting = 0
    var bracketGuardedBlocks = 0
    var nestingGuardedBlocks = 0

    fun flushRegion() {
        if (region.isEmpty()) return
        val overBrackets = deepestBracketsInRegion > maxBracketDepth
        val overNesting = deepestNestingInRegion > maxNestingDepth
        if (overBrackets || overNesting) {
            if (overBrackets) {
                deepestBrackets = maxOf(deepestBrackets, deepestBracketsInRegion)
                bracketGuardedBlocks += 1
            }
            if (overNesting) {
                deepestNesting = maxOf(deepestNesting, deepestNestingInRegion)
                nestingGuardedBlocks += 1
            }
            val indent = region.first().takeWhile { character -> character == ' ' }.take(MAX_PLACEHOLDER_INDENT)
            bodyLines += "$indent<!--orca:rawtext:${rawRegions.size}-->"
            rawRegions += region.joinToString("\n")
        } else {
            bodyLines += region
        }
        region.clear()
        pendingBrackets = 0
        deepestBracketsInRegion = 0
        deepestNestingInRegion = 0
    }

    for (line in lines) {
        val trimmed = line.trim()
        val isFenceLine = trimmed.startsWith("```") || trimmed.startsWith("~~~")
        if (isFenceLine) {
            val marker = trimmed[0]
            fence = when {
                fence == ' ' -> marker
                fence == marker -> ' '
                else -> fence
            }
            region += line
            continue
        }
        if (fence != ' ') {
            region += line
            continue
        }
        if (trimmed == "$$") {
            mathOpen = !mathOpen
            region += line
            continue
        }
        if (mathOpen) {
            region += line
            continue
        }
        if (trimmed.isEmpty()) {
            flushRegion()
            bodyLines += line
            continue
        }

        region += line
        val nesting = blockNestingDepth(line)
        if (nesting > deepestNestingInRegion) deepestNestingInRegion = nesting
        var index = 0
        while (index < line.length) {
            when (line[index]) {
                '\\' -> index += 1
                '[' -> {
                    pendingBrackets += 1
                    if (pendingBrackets > deepestBracketsInRegion) deepestBracketsInRegion = pendingBrackets
                }
                ']' -> if (pendingBrackets > 0) pendingBrackets -= 1
            }
            index += 1
        }
    }
    flushRegion()

    return InlineGuardExtraction(
        markdown = bodyLines.joinToString("\n"),
        rawRegions = rawRegions,
        exceededBracketDepth = deepestBrackets,
        exceededNestingDepth = deepestNesting,
        bracketGuardedBlocks = bracketGuardedBlocks,
        nestingGuardedBlocks = nestingGuardedBlocks,
    )
}

private fun unguarded(markdown: String) = InlineGuardExtraction(
    markdown = markdown,
    rawRegions = emptyList(),
    exceededBracketDepth = 0,
    exceededNestingDepth = 0,
    bracketGuardedBlocks = 0,
    nestingGuardedBlocks = 0,
)

/** Columns of indentation one nesting level of a list is assumed to take. */
private const val LIST_INDENT_COLUMNS = 2
private const val TAB_COLUMNS = 4

/**
 * Levels of block nesting a line opens: one per blockquote marker, plus the list level
 * its indentation puts a list marker on.
 *
 * Indented lines without a list marker are ignored on purpose — those are indented code,
 * which costs the parser nothing.
 */
private fun blockNestingDepth(line: String): Int {
    var index = 0
    var depth = 0
    var columns = 0
    while (index < line.length) {
        when (line[index]) {
            ' ' -> columns += 1
            '\t' -> columns += TAB_COLUMNS
            '>' -> {
                depth += 1
                columns = 0
            }
            else -> return depth + if (startsListMarker(line, index)) columns / LIST_INDENT_COLUMNS + 1 else 0
        }
        index += 1
    }
    return depth
}

private fun startsListMarker(line: String, from: Int): Boolean {
    var index = from
    val marker = line[index]
    if (marker == '-' || marker == '+' || marker == '*') {
        index += 1
    } else {
        var digits = 0
        while (index < line.length && digits < 10 && line[index].isDigit()) {
            index += 1
            digits += 1
        }
        if (digits == 0 || digits > 9 || index >= line.length) return false
        val delimiter = line[index]
        if (delimiter != '.' && delimiter != ')') return false
        index += 1
    }
    return index >= line.length || line[index].isWhitespace()
}

/** A placeholder indented four spaces would parse as code, so keep it in block range. */
private const val MAX_PLACEHOLDER_INDENT = 3

private fun mayExceedGuardLimits(
    markdown: String,
    maxBracketDepth: Int,
    maxNestingDepth: Int,
): Boolean {
    var openers = 0
    var quoteMarkers = 0
    var indentColumns = 0
    val indentLimit = maxNestingDepth * LIST_INDENT_COLUMNS
    for (character in markdown) {
        when (character) {
            '[' -> {
                openers += 1
                if (openers > maxBracketDepth) return true
            }
            '>' -> {
                quoteMarkers += 1
                if (quoteMarkers > maxNestingDepth) return true
            }
            ' ' -> {
                indentColumns += 1
                if (indentColumns > indentLimit) return true
            }
            '\t' -> {
                indentColumns += TAB_COLUMNS
                if (indentColumns > indentLimit) return true
            }
            else -> indentColumns = 0
        }
    }
    return false
}

/**
 * Replaces the placeholders left by [extractInlineGuardedRegions] with paragraphs
 * holding the untouched source text. Walks nested blocks because a guarded region may
 * sit inside `<details>`, a quote, a list item, or a footnote definition.
 */
internal fun resolveRawTextPlaceholders(
    blocks: List<OrcaBlock>,
    rawRegions: List<String>,
): List<OrcaBlock> {
    if (rawRegions.isEmpty()) return blocks
    return blocks.map { block -> resolveRawTextPlaceholder(block, rawRegions) }
}

private fun resolveRawTextPlaceholder(
    block: OrcaBlock,
    rawRegions: List<String>,
): OrcaBlock {
    return when (block) {
        is OrcaBlock.HtmlBlock -> {
            val match = RAW_TEXT_PLACEHOLDER_REGEX.matchEntire(block.html)
            val raw = match?.groupValues?.get(1)?.toIntOrNull()?.let(rawRegions::getOrNull)
            if (raw == null) block else OrcaBlock.Paragraph(content = listOf(OrcaInline.Text(raw)))
        }
        is OrcaBlock.Quote -> block.copy(blocks = resolveRawTextPlaceholders(block.blocks, rawRegions))
        is OrcaBlock.Admonition -> block.copy(blocks = resolveRawTextPlaceholders(block.blocks, rawRegions))
        is OrcaBlock.Details -> block.copy(blocks = resolveRawTextPlaceholders(block.blocks, rawRegions))
        is OrcaBlock.ListBlock -> block.copy(
            items = block.items.map { item ->
                item.copy(blocks = resolveRawTextPlaceholders(item.blocks, rawRegions))
            },
        )
        is OrcaBlock.Footnotes -> block.copy(
            definitions = block.definitions.map { definition ->
                definition.copy(blocks = resolveRawTextPlaceholders(definition.blocks, rawRegions))
            },
        )
        is OrcaBlock.DefinitionList -> block.copy(
            items = block.items.map { item ->
                item.copy(
                    definitions = item.definitions.map { definition ->
                        resolveRawTextPlaceholders(definition, rawRegions)
                    },
                )
            },
        )
        else -> block
    }
}
