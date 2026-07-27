package ru.wertik.orca.core

internal data class InlineGuardExtraction(
    val markdown: String,
    /** Raw source of every region that was pulled out, in placeholder order. */
    val rawRegions: List<String>,
    /** Deepest run of unmatched `[` that was found, or `0` when nothing was guarded. */
    val exceededDepth: Int,
)

private val RAW_TEXT_PLACEHOLDER_REGEX = Regex("""^\s*<!--orca:rawtext:(\d+)-->\s*$""")

/** Longest run of unmatched `[` a single block may contain before it is left unparsed. */
internal const val DEFAULT_MAX_INLINE_BRACKET_DEPTH = 512

/**
 * Guards the inline scanner against link-bracket bombs.
 *
 * The underlying parser resolves link openers by backtracking, which is quadratic in
 * the number of *unmatched* `[` inside one block: 3 200 of them take ~0.2 s, 12 800
 * take ~2.5 s, and 25 600 hang long enough to look like a deadlock. Balanced brackets
 * are cheap, so the guard tracks the depth of unclosed openers per block and, when a
 * block goes past [maxBracketDepth], swaps that block for a `<!--orca:rawtext:N-->`
 * placeholder. [resolveRawTextPlaceholders] turns the placeholder back into a plain
 * text paragraph after parsing, so no input is lost and the rest of the document is
 * still parsed normally.
 *
 * Fenced code and `$$` math blocks are skipped: their content never reaches the inline
 * scanner. Inline code spans are *not* skipped — an unterminated backtick would make
 * that unsound — so a paragraph carrying a bracket bomb inside a code span degrades to
 * plain text as well.
 */
internal fun extractInlineGuardedRegions(
    markdown: String,
    maxBracketDepth: Int = DEFAULT_MAX_INLINE_BRACKET_DEPTH,
): InlineGuardExtraction {
    // Cheap rejection: fewer openers in the whole document than the limit for a single
    // block means no block can ever reach it.
    if (!hasEnoughOpeners(markdown, maxBracketDepth)) {
        return InlineGuardExtraction(markdown = markdown, rawRegions = emptyList(), exceededDepth = 0)
    }

    val lines = markdown.split('\n')
    val bodyLines = ArrayList<String>(lines.size)
    val rawRegions = mutableListOf<String>()
    val region = mutableListOf<String>()

    var fence = ' '
    var mathOpen = false
    var pendingBrackets = 0
    var deepestInRegion = 0
    var deepestOverall = 0

    fun flushRegion() {
        if (region.isEmpty()) return
        if (deepestInRegion > maxBracketDepth) {
            deepestOverall = maxOf(deepestOverall, deepestInRegion)
            val indent = region.first().takeWhile { character -> character == ' ' }.take(MAX_PLACEHOLDER_INDENT)
            bodyLines += "$indent<!--orca:rawtext:${rawRegions.size}-->"
            rawRegions += region.joinToString("\n")
        } else {
            bodyLines += region
        }
        region.clear()
        pendingBrackets = 0
        deepestInRegion = 0
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
        var index = 0
        while (index < line.length) {
            when (line[index]) {
                '\\' -> index += 1
                '[' -> {
                    pendingBrackets += 1
                    if (pendingBrackets > deepestInRegion) deepestInRegion = pendingBrackets
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
        exceededDepth = deepestOverall,
    )
}

/** A placeholder indented four spaces would parse as code, so keep it in block range. */
private const val MAX_PLACEHOLDER_INDENT = 3

private fun hasEnoughOpeners(markdown: String, maxBracketDepth: Int): Boolean {
    var count = 0
    for (character in markdown) {
        if (character == '[') {
            count += 1
            if (count > maxBracketDepth) return true
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
