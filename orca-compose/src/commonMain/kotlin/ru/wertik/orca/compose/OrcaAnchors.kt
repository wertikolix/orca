package ru.wertik.orca.compose

import ru.wertik.orca.core.OrcaBlock
import ru.wertik.orca.core.OrcaDocument

/**
 * Maps heading anchor ids to their top-level block indices.
 *
 * Orca renders exactly one lazy-list item per top-level block, so the returned
 * indices can be passed straight to `LazyListState.animateScrollToItem` to build
 * a table of contents or scroll-spy UI. Combine with
 * `OrcaDocument.tableOfContents()` from `orca-core` for titles and levels.
 */
fun orcaHeadingBlockIndex(document: OrcaDocument): Map<String, Int> {
    return buildMap {
        document.blocks.forEachIndexed { index, block ->
            val heading = block as? OrcaBlock.Heading ?: return@forEachIndexed
            val id = heading.id?.takeIf { it.isNotEmpty() } ?: return@forEachIndexed
            if (id !in this) put(id, index)
        }
    }
}
