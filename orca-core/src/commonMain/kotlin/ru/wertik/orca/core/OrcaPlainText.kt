package ru.wertik.orca.core

private const val MAX_PLAIN_TEXT_DEPTH = 32

/**
 * Flattens the document to plain text in reading order.
 *
 * Blocks are separated by [blockSeparator]. Useful for search, share sheets,
 * accessibility exports, and analytics that need the document without markup.
 */
fun OrcaDocument.plainText(blockSeparator: String = "\n\n"): String {
    return blocks.joinToString(blockSeparator) { block -> block.plainText() }
}

/** Flattens a single block to plain text, including nested block content. */
fun OrcaBlock.plainText(): String = plainText(depth = 0)

/** Flattens inline content to plain text, mirroring rendered reading order. */
fun List<OrcaInline>.plainText(): String = orcaPlainText()

private fun OrcaBlock.plainText(depth: Int): String {
    if (depth > MAX_PLAIN_TEXT_DEPTH) return ""
    return when (this) {
        is OrcaBlock.Heading -> content.orcaPlainText()
        is OrcaBlock.Paragraph -> content.orcaPlainText()
        is OrcaBlock.CodeBlock -> code
        is OrcaBlock.Math -> source
        is OrcaBlock.Image -> alt.orEmpty()
        is OrcaBlock.ThematicBreak -> ""
        is OrcaBlock.HtmlBlock -> html
        is OrcaBlock.Quote -> blocks.joinBlocks(depth)
        is OrcaBlock.Admonition -> {
            val heading = title ?: type.name
            val body = blocks.joinBlocks(depth)
            if (body.isEmpty()) heading else "$heading\n$body"
        }
        is OrcaBlock.Details -> {
            val heading = summary.orcaPlainText()
            val body = blocks.joinBlocks(depth)
            if (body.isEmpty()) heading else "$heading\n$body"
        }
        is OrcaBlock.ListBlock -> items.joinToString("\n") { item -> item.blocks.joinBlocks(depth) }
        is OrcaBlock.Table -> {
            val rowsText = (listOf(header) + rows).joinToString("\n") { row ->
                row.joinToString("\t") { cell -> cell.content.orcaPlainText() }
            }
            rowsText
        }
        is OrcaBlock.DefinitionList -> items.joinToString("\n") { item ->
            val term = item.term.orcaPlainText()
            val definitions = item.definitions.joinToString("\n") { definition -> definition.joinBlocks(depth) }
            if (definitions.isEmpty()) term else "$term\n$definitions"
        }
        is OrcaBlock.Footnotes -> definitions.joinToString("\n") { definition ->
            val body = definition.blocks.joinBlocks(depth)
            if (body.isEmpty()) definition.label else "${definition.label}. $body"
        }
    }
}

private fun List<OrcaBlock>.joinBlocks(depth: Int): String {
    return asSequence()
        .map { block -> block.plainText(depth + 1) }
        .filter { text -> text.isNotEmpty() }
        .joinToString("\n")
}
