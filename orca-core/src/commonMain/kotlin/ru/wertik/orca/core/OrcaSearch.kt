package ru.wertik.orca.core

/**
 * Options controlling [findMatches].
 *
 * @property caseSensitive when `false` (default) matching ignores case.
 * @property wholeWord when `true`, matches must be bounded by non-alphanumeric characters.
 * @property limit maximum number of matches to return. Guards runaway scans on huge documents.
 * @property snippetRadius characters of context kept on each side of a match in [OrcaSearchMatch.snippet].
 */
data class OrcaSearchOptions(
    val caseSensitive: Boolean = false,
    val wholeWord: Boolean = false,
    val limit: Int = 200,
    val snippetRadius: Int = 32,
)

/**
 * One occurrence of a search query inside a document.
 *
 * [blockIndex] is the index of the top-level block containing the match. Orca renders exactly one
 * lazy-list item per top-level block, so the value can be passed straight to
 * `LazyListState.animateScrollToItem` to jump to the hit.
 *
 * @property range match position inside [blockText].
 * @property snippet single-line excerpt around the match, elided with `…` when truncated.
 * @property headingId anchor of the closest heading at or above the match, or `null`.
 * @property headingTitle plain-text title of that heading, or `null`.
 */
data class OrcaSearchMatch(
    val blockIndex: Int,
    val range: IntRange,
    val blockText: String,
    val snippet: String,
    val headingId: String? = null,
    val headingTitle: String? = null,
)

/**
 * Finds every occurrence of [query] in the document's textual content.
 *
 * The search runs over the plain-text projection of each top-level block (see [plainText]),
 * so emphasis, links, and other markup never split a match.
 *
 * Returns an empty list for a blank query.
 */
fun OrcaDocument.findMatches(
    query: String,
    options: OrcaSearchOptions = OrcaSearchOptions(),
): List<OrcaSearchMatch> {
    if (query.isBlank() || options.limit <= 0) return emptyList()

    val matches = mutableListOf<OrcaSearchMatch>()
    var headingId: String? = null
    var headingTitle: String? = null

    for ((index, block) in blocks.withIndex()) {
        if (block is OrcaBlock.Heading) {
            headingId = block.id?.takeIf { it.isNotEmpty() }
            headingTitle = block.content.orcaPlainText().trim().takeIf { it.isNotEmpty() }
        }

        val text = block.plainText()
        if (text.isEmpty()) continue

        var searchFrom = 0
        while (searchFrom <= text.length - query.length) {
            val start = text.indexOf(query, startIndex = searchFrom, ignoreCase = !options.caseSensitive)
            if (start < 0) break
            val end = start + query.length
            searchFrom = start + 1

            if (options.wholeWord && !isWholeWord(text, start, end)) continue

            matches += OrcaSearchMatch(
                blockIndex = index,
                range = start until end,
                blockText = text,
                snippet = buildSnippet(text, start, end, options.snippetRadius),
                headingId = headingId,
                headingTitle = headingTitle,
            )
            if (matches.size >= options.limit) return matches
        }
    }
    return matches
}

/** Counts occurrences of [query] without materializing snippets for every hit. */
fun OrcaDocument.countMatches(
    query: String,
    options: OrcaSearchOptions = OrcaSearchOptions(),
): Int = findMatches(query, options).size

private fun isWholeWord(text: String, start: Int, end: Int): Boolean {
    val before = text.getOrNull(start - 1)
    val after = text.getOrNull(end)
    return !(before?.isLetterOrDigit() ?: false) && !(after?.isLetterOrDigit() ?: false)
}

private fun buildSnippet(text: String, start: Int, end: Int, radius: Int): String {
    val safeRadius = radius.coerceAtLeast(0)
    val from = (start - safeRadius).coerceAtLeast(0)
    val to = (end + safeRadius).coerceAtMost(text.length)
    val core = text.substring(from, to)
        .replace('\n', ' ')
        .replace('\t', ' ')
        .trim()
    val prefix = if (from > 0) "…" else ""
    val suffix = if (to < text.length) "…" else ""
    return "$prefix$core$suffix"
}
