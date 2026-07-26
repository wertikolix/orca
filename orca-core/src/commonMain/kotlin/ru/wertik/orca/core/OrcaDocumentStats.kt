package ru.wertik.orca.core

/**
 * Aggregate counts describing a parsed document.
 *
 * Produced by [stats]. Every counter includes nested content: a table inside a quote
 * inside a list item is still counted once as a table.
 */
data class OrcaDocumentStats(
    /** Number of top-level blocks, i.e. the number of rendered lazy-list items. */
    val blocks: Int = 0,
    /** Whitespace-separated words across all textual content. */
    val words: Int = 0,
    /** Characters across all textual content, including code and table cells. */
    val characters: Int = 0,
    val headings: Int = 0,
    val paragraphs: Int = 0,
    val codeBlocks: Int = 0,
    val quotes: Int = 0,
    val admonitions: Int = 0,
    val tables: Int = 0,
    val listItems: Int = 0,
    val tasks: Int = 0,
    val completedTasks: Int = 0,
    val links: Int = 0,
    val images: Int = 0,
    val mathBlocks: Int = 0,
    val inlineMath: Int = 0,
    val footnotes: Int = 0,
    /** Estimated reading time in minutes, rounded up, never below `1` for non-empty documents. */
    val readingMinutes: Int = 0,
) {
    /** Tasks that are still unchecked. */
    val openTasks: Int get() = (tasks - completedTasks).coerceAtLeast(0)

    /** Completion ratio in `0f..1f`, or `0f` when the document has no tasks. */
    val taskProgress: Float get() = if (tasks == 0) 0f else completedTasks.toFloat() / tasks.toFloat()
}

/**
 * Computes [OrcaDocumentStats] for the document.
 *
 * @param wordsPerMinute reading speed used for [OrcaDocumentStats.readingMinutes]. Defaults to 220.
 */
fun OrcaDocument.stats(wordsPerMinute: Int = 220): OrcaDocumentStats {
    val counter = StatsCounter()
    blocks.forEach { block -> counter.countBlock(block, depth = 0) }
    val speed = wordsPerMinute.coerceAtLeast(1)
    val minutes = if (counter.words == 0) {
        0
    } else {
        ((counter.words + speed - 1) / speed).coerceAtLeast(1)
    }
    return OrcaDocumentStats(
        blocks = blocks.size,
        words = counter.words,
        characters = counter.characters,
        headings = counter.headings,
        paragraphs = counter.paragraphs,
        codeBlocks = counter.codeBlocks,
        quotes = counter.quotes,
        admonitions = counter.admonitions,
        tables = counter.tables,
        listItems = counter.listItems,
        tasks = counter.tasks,
        completedTasks = counter.completedTasks,
        links = counter.links,
        images = counter.images,
        mathBlocks = counter.mathBlocks,
        inlineMath = counter.inlineMath,
        footnotes = counter.footnotes,
        readingMinutes = minutes,
    )
}

private const val MAX_STATS_DEPTH = 32

private class StatsCounter {
    var words = 0
    var characters = 0
    var headings = 0
    var paragraphs = 0
    var codeBlocks = 0
    var quotes = 0
    var admonitions = 0
    var tables = 0
    var listItems = 0
    var tasks = 0
    var completedTasks = 0
    var links = 0
    var images = 0
    var mathBlocks = 0
    var inlineMath = 0
    var footnotes = 0

    fun countBlock(block: OrcaBlock, depth: Int) {
        if (depth > MAX_STATS_DEPTH) return
        when (block) {
            is OrcaBlock.Heading -> {
                headings += 1
                countInlines(block.content)
            }

            is OrcaBlock.Paragraph -> {
                paragraphs += 1
                countInlines(block.content)
            }

            is OrcaBlock.CodeBlock -> {
                codeBlocks += 1
                countText(block.code)
            }

            is OrcaBlock.Math -> {
                mathBlocks += 1
                countText(block.source)
            }

            is OrcaBlock.Image -> {
                images += 1
                countText(block.alt.orEmpty())
            }

            is OrcaBlock.ThematicBreak -> Unit

            is OrcaBlock.HtmlBlock -> countText(block.html)

            is OrcaBlock.Quote -> {
                quotes += 1
                block.blocks.forEach { child -> countBlock(child, depth + 1) }
            }

            is OrcaBlock.Admonition -> {
                admonitions += 1
                block.title?.let { countText(it) }
                block.blocks.forEach { child -> countBlock(child, depth + 1) }
            }

            is OrcaBlock.Details -> {
                countInlines(block.summary)
                block.blocks.forEach { child -> countBlock(child, depth + 1) }
            }

            is OrcaBlock.ListBlock -> block.items.forEach { item ->
                listItems += 1
                when (item.taskState) {
                    OrcaTaskState.CHECKED -> {
                        tasks += 1
                        completedTasks += 1
                    }

                    OrcaTaskState.UNCHECKED -> tasks += 1
                    null -> Unit
                }
                item.blocks.forEach { child -> countBlock(child, depth + 1) }
            }

            is OrcaBlock.Table -> {
                tables += 1
                block.header.forEach { cell -> countInlines(cell.content) }
                block.rows.forEach { row -> row.forEach { cell -> countInlines(cell.content) } }
            }

            is OrcaBlock.DefinitionList -> block.items.forEach { item ->
                countInlines(item.term)
                item.definitions.forEach { definition ->
                    definition.forEach { child -> countBlock(child, depth + 1) }
                }
            }

            is OrcaBlock.Footnotes -> block.definitions.forEach { definition ->
                footnotes += 1
                definition.blocks.forEach { child -> countBlock(child, depth + 1) }
            }
        }
    }

    private fun countInlines(inlines: List<OrcaInline>, depth: Int = 0) {
        if (depth > MAX_STATS_DEPTH) return
        inlines.forEach { inline ->
            when (inline) {
                is OrcaInline.Text -> countText(inline.text)
                is OrcaInline.InlineCode -> countText(inline.code)
                is OrcaInline.Abbreviation -> countText(inline.text)
                is OrcaInline.HtmlInline -> Unit
                is OrcaInline.FootnoteReference -> Unit

                is OrcaInline.Math -> {
                    inlineMath += 1
                    countText(inline.source)
                }

                is OrcaInline.Image -> {
                    images += 1
                    countText(inline.alt.orEmpty())
                }

                is OrcaInline.Link -> {
                    links += 1
                    if (inline.content.isEmpty()) {
                        countText(inline.destination)
                    } else {
                        countInlines(inline.content, depth + 1)
                    }
                }

                is OrcaInline.Bold -> countInlines(inline.content, depth + 1)
                is OrcaInline.Italic -> countInlines(inline.content, depth + 1)
                is OrcaInline.Strikethrough -> countInlines(inline.content, depth + 1)
                is OrcaInline.Superscript -> countInlines(inline.content, depth + 1)
                is OrcaInline.Subscript -> countInlines(inline.content, depth + 1)
                is OrcaInline.Highlight -> countInlines(inline.content, depth + 1)
                is OrcaInline.Underline -> countInlines(inline.content, depth + 1)
            }
        }
    }

    private fun countText(text: String) {
        if (text.isEmpty()) return
        characters += text.length
        var inWord = false
        for (char in text) {
            if (char.isWhitespace()) {
                inWord = false
            } else if (!inWord) {
                inWord = true
                words += 1
            }
        }
    }
}
