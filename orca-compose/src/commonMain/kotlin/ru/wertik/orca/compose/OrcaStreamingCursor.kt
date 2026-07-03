package ru.wertik.orca.compose

import ru.wertik.orca.core.OrcaBlock
import ru.wertik.orca.core.OrcaDocument
import ru.wertik.orca.core.OrcaInline

/**
 * Returns a copy of the document with [cursor] appended to the trailing edge of
 * the last text-bearing block. Applied after parsing so streaming sources stay
 * append-only for incremental parser sessions.
 */
internal fun OrcaDocument.withTrailingCursor(cursor: String): OrcaDocument {
    if (cursor.isEmpty()) return this
    val blocks = blocks
    if (blocks.isEmpty()) {
        return copy(blocks = listOf(OrcaBlock.Paragraph(content = listOf(OrcaInline.Text(cursor)))))
    }
    val updatedLast = appendCursorToBlock(blocks.last(), cursor)
    return if (updatedLast != null) {
        copy(blocks = blocks.dropLast(1) + updatedLast)
    } else {
        copy(blocks = blocks + OrcaBlock.Paragraph(content = listOf(OrcaInline.Text(cursor))))
    }
}

private fun appendCursorToBlock(block: OrcaBlock, cursor: String): OrcaBlock? {
    return when (block) {
        is OrcaBlock.Paragraph -> block.copy(content = block.content + OrcaInline.Text(cursor))
        is OrcaBlock.Heading -> block.copy(content = block.content + OrcaInline.Text(cursor))
        is OrcaBlock.CodeBlock -> block.copy(code = block.code + cursor)
        is OrcaBlock.Quote -> appendCursorToLast(block.blocks, cursor)?.let { block.copy(blocks = it) }
        is OrcaBlock.Admonition -> appendCursorToLast(block.blocks, cursor)?.let { block.copy(blocks = it) }
        is OrcaBlock.ListBlock -> {
            val lastItem = block.items.lastOrNull() ?: return null
            val updated = appendCursorToLast(lastItem.blocks, cursor) ?: return null
            block.copy(items = block.items.dropLast(1) + lastItem.copy(blocks = updated))
        }
        else -> null
    }
}

private fun appendCursorToLast(blocks: List<OrcaBlock>, cursor: String): List<OrcaBlock>? {
    val last = blocks.lastOrNull() ?: return null
    val updated = appendCursorToBlock(last, cursor) ?: return null
    return blocks.dropLast(1) + updated
}
