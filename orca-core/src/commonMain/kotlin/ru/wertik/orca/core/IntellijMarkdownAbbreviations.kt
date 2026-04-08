package ru.wertik.orca.core

internal data class AbbreviationExtraction(
    val markdown: String,
    val abbreviations: Map<String, String>,
)

private val ABBREVIATION_DEFINITION_REGEX = Regex("""^\*\[([^\]]+)]:\s*(.+)$""")

/**
 * Extract abbreviation definitions from markdown source.
 *
 * Abbreviation syntax (PHP Markdown Extra):
 * ```
 * *[HTML]: Hyper Text Markup Language
 * *[CSS]: Cascading Style Sheets
 * ```
 *
 * Definitions are removed from the markdown body. The returned map
 * is keyed by abbreviation text, valued by the expansion. Abbreviation
 * replacement in inline content is handled separately after parsing.
 */
internal fun extractAbbreviations(markdown: String): AbbreviationExtraction {
    val lines = markdown.split('\n')
    val bodyLines = mutableListOf<String>()
    val abbreviations = linkedMapOf<String, String>()

    for (line in lines) {
        val match = ABBREVIATION_DEFINITION_REGEX.matchEntire(line.trim())
        if (match != null) {
            val abbr = match.groupValues[1]
            val title = match.groupValues[2].trim()
            if (abbr.isNotEmpty() && title.isNotEmpty()) {
                abbreviations[abbr] = title
            }
        } else {
            bodyLines += line
        }
    }

    return AbbreviationExtraction(
        markdown = bodyLines.joinToString("\n"),
        abbreviations = abbreviations,
    )
}

/**
 * Replace abbreviation occurrences in parsed inline nodes.
 *
 * Walks through all [OrcaInline.Text] nodes and replaces whole-word matches
 * of abbreviation keys with [OrcaInline.Abbreviation] nodes.
 */
internal fun applyAbbreviations(
    blocks: List<OrcaBlock>,
    abbreviations: Map<String, String>,
): List<OrcaBlock> {
    if (abbreviations.isEmpty()) return blocks
    return blocks.map { block -> applyAbbreviationsToBlock(block, abbreviations) }
}

private fun applyAbbreviationsToBlock(
    block: OrcaBlock,
    abbreviations: Map<String, String>,
): OrcaBlock {
    return when (block) {
        is OrcaBlock.Heading -> block.copy(
            content = applyAbbreviationsToInlines(block.content, abbreviations),
        )
        is OrcaBlock.Paragraph -> block.copy(
            content = applyAbbreviationsToInlines(block.content, abbreviations),
        )
        is OrcaBlock.ListBlock -> block.copy(
            items = block.items.map { item ->
                item.copy(
                    blocks = applyAbbreviations(item.blocks, abbreviations),
                )
            },
        )
        is OrcaBlock.Quote -> block.copy(
            blocks = applyAbbreviations(block.blocks, abbreviations),
        )
        is OrcaBlock.Admonition -> block.copy(
            blocks = applyAbbreviations(block.blocks, abbreviations),
        )
        is OrcaBlock.Table -> block.copy(
            header = block.header.map { cell ->
                cell.copy(content = applyAbbreviationsToInlines(cell.content, abbreviations))
            },
            rows = block.rows.map { row ->
                row.map { cell ->
                    cell.copy(content = applyAbbreviationsToInlines(cell.content, abbreviations))
                }
            },
        )
        is OrcaBlock.Footnotes -> block.copy(
            definitions = block.definitions.map { def ->
                def.copy(blocks = applyAbbreviations(def.blocks, abbreviations))
            },
        )
        is OrcaBlock.DefinitionList -> block.copy(
            items = block.items.map { item ->
                item.copy(
                    term = applyAbbreviationsToInlines(item.term, abbreviations),
                    definitions = item.definitions.map { defBlocks ->
                        applyAbbreviations(defBlocks, abbreviations)
                    },
                )
            },
        )
        is OrcaBlock.Details -> block.copy(
            summary = applyAbbreviationsToInlines(block.summary, abbreviations),
            blocks = applyAbbreviations(block.blocks, abbreviations),
        )
        is OrcaBlock.CodeBlock,
        is OrcaBlock.Image,
        is OrcaBlock.ThematicBreak,
        is OrcaBlock.HtmlBlock,
            -> block
    }
}

private fun applyAbbreviationsToInlines(
    inlines: List<OrcaInline>,
    abbreviations: Map<String, String>,
): List<OrcaInline> {
    return inlines.flatMap { inline ->
        applyAbbreviationsToInline(inline, abbreviations)
    }
}

private fun applyAbbreviationsToInline(
    inline: OrcaInline,
    abbreviations: Map<String, String>,
): List<OrcaInline> {
    return when (inline) {
        is OrcaInline.Text -> replaceAbbreviationsInText(inline.text, abbreviations)
        is OrcaInline.Bold -> listOf(
            inline.copy(content = applyAbbreviationsToInlines(inline.content, abbreviations)),
        )
        is OrcaInline.Italic -> listOf(
            inline.copy(content = applyAbbreviationsToInlines(inline.content, abbreviations)),
        )
        is OrcaInline.Strikethrough -> listOf(
            inline.copy(content = applyAbbreviationsToInlines(inline.content, abbreviations)),
        )
        is OrcaInline.Superscript -> listOf(
            inline.copy(content = applyAbbreviationsToInlines(inline.content, abbreviations)),
        )
        is OrcaInline.Subscript -> listOf(
            inline.copy(content = applyAbbreviationsToInlines(inline.content, abbreviations)),
        )
        is OrcaInline.Link -> listOf(
            inline.copy(content = applyAbbreviationsToInlines(inline.content, abbreviations)),
        )
        is OrcaInline.InlineCode,
        is OrcaInline.Image,
        is OrcaInline.FootnoteReference,
        is OrcaInline.HtmlInline,
        is OrcaInline.Abbreviation,
            -> listOf(inline)
    }
}

/**
 * Replace abbreviation matches in a plain text string.
 * Uses word boundary matching to avoid partial replacements.
 * Longer abbreviations are matched first to handle overlapping cases.
 */
private fun replaceAbbreviationsInText(
    text: String,
    abbreviations: Map<String, String>,
): List<OrcaInline> {
    if (text.isEmpty() || abbreviations.isEmpty()) {
        return listOf(OrcaInline.Text(text))
    }

    // Build a regex matching any abbreviation, longest first.
    val sortedKeys = abbreviations.keys.sortedByDescending { it.length }
    val pattern = sortedKeys.joinToString("|") { key -> Regex.escape(key) }
    val regex = Regex("""(?<!\w)($pattern)(?!\w)""")

    val result = mutableListOf<OrcaInline>()
    var lastEnd = 0

    for (match in regex.findAll(text)) {
        if (match.range.first > lastEnd) {
            result += OrcaInline.Text(text.substring(lastEnd, match.range.first))
        }
        val abbr = match.value
        val title = abbreviations[abbr]
        if (title != null) {
            result += OrcaInline.Abbreviation(text = abbr, title = title)
        } else {
            result += OrcaInline.Text(abbr)
        }
        lastEnd = match.range.last + 1
    }

    if (lastEnd < text.length) {
        result += OrcaInline.Text(text.substring(lastEnd))
    }

    return if (result.isEmpty()) listOf(OrcaInline.Text(text)) else result
}
