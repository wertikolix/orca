package ru.wertik.orca.core

/**
 * Single entry of a document's table of contents.
 *
 * @property level Heading level (1–6).
 * @property title Plain-text heading title.
 * @property id Anchor slug usable with `#fragment` links, or `null`/empty when absent.
 */
data class OrcaTocEntry(
    val level: Int,
    val title: String,
    val id: String?,
)

/**
 * Extracts a table of contents from the document's top-level headings.
 *
 * The returned entries appear in document order. Because Orca renders one
 * top-level block per lazy-list item, the entries can be matched back to list
 * indices through the heading `id`.
 *
 * @param maxLevel Deepest heading level to include. Defaults to `6` (all levels).
 */
fun OrcaDocument.tableOfContents(maxLevel: Int = 6): List<OrcaTocEntry> {
    return blocks.mapNotNull { block ->
        val heading = block as? OrcaBlock.Heading ?: return@mapNotNull null
        if (heading.level > maxLevel) return@mapNotNull null
        OrcaTocEntry(
            level = heading.level,
            title = heading.content.orcaPlainText().trim(),
            id = heading.id,
        )
    }
}
