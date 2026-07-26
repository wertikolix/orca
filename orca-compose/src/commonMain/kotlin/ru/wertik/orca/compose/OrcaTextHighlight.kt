package ru.wertik.orca.compose

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle

/**
 * Search query highlighted inside rendered text.
 *
 * Pass an instance to `Orca(highlight = …)` to shade every occurrence of [query] with
 * [OrcaInlineStyle.searchMatch]. Highlighting applies to inline text: headings, paragraphs,
 * list items, table cells, definition terms, details summaries, and footnote bodies. Code blocks
 * keep their syntax colors untouched.
 *
 * Combine with `OrcaDocument.findMatches()` from `orca-core` to build match counters and
 * jump-to-match navigation; that API reports the top-level block index of every hit.
 *
 * @property query text to highlight. A blank query disables highlighting.
 * @property caseSensitive when `false` (default) matching ignores case.
 * @property wholeWord when `true`, only matches bounded by non-alphanumeric characters are shaded.
 */
@Immutable
data class OrcaTextHighlight(
    val query: String,
    val caseSensitive: Boolean = false,
    val wholeWord: Boolean = false,
) {
    /** Whether this highlight shades anything at all. */
    val isActive: Boolean get() = query.isNotBlank()
}

internal val LocalOrcaTextHighlight = compositionLocalOf<OrcaTextHighlight?> { null }

/** Applies [highlight] spans on top of already-styled text. */
internal fun AnnotatedString.withSearchHighlight(
    highlight: OrcaTextHighlight?,
    spanStyle: SpanStyle,
): AnnotatedString {
    if (highlight == null || !highlight.isActive) return this
    val query = highlight.query
    if (query.length > text.length) return this

    val ranges = mutableListOf<IntRange>()
    var searchFrom = 0
    while (searchFrom <= text.length - query.length) {
        val start = text.indexOf(query, startIndex = searchFrom, ignoreCase = !highlight.caseSensitive)
        if (start < 0) break
        val end = start + query.length
        searchFrom = start + 1
        if (highlight.wholeWord && !isWholeWordMatch(text, start, end)) continue
        ranges += start until end
    }
    if (ranges.isEmpty()) return this

    val builder = AnnotatedString.Builder(this)
    ranges.forEach { range -> builder.addStyle(spanStyle, range.first, range.last + 1) }
    return builder.toAnnotatedString()
}

private fun isWholeWordMatch(text: String, start: Int, end: Int): Boolean {
    val before = text.getOrNull(start - 1)
    val after = text.getOrNull(end)
    return before?.isLetterOrDigit() != true && after?.isLetterOrDigit() != true
}
