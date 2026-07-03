package ru.wertik.orca.core

/** Flattens inline content to plain text, mirroring rendered reading order. */
internal fun List<OrcaInline>.orcaPlainText(): String {
    return buildString {
        for (inline in this@orcaPlainText) {
            when (inline) {
                is OrcaInline.Text -> append(inline.text)
                is OrcaInline.Bold -> append(inline.content.orcaPlainText())
                is OrcaInline.Italic -> append(inline.content.orcaPlainText())
                is OrcaInline.Strikethrough -> append(inline.content.orcaPlainText())
                is OrcaInline.InlineCode -> append(inline.code)
                is OrcaInline.Math -> append("\$${inline.source}\$")
                is OrcaInline.Link -> append(inline.content.orcaPlainText().ifEmpty { inline.destination })
                is OrcaInline.Image -> append(inline.alt ?: "")
                is OrcaInline.FootnoteReference -> append("[${inline.label}]")
                is OrcaInline.Superscript -> append(inline.content.orcaPlainText())
                is OrcaInline.Subscript -> append(inline.content.orcaPlainText())
                is OrcaInline.Highlight -> append(inline.content.orcaPlainText())
                is OrcaInline.Underline -> append(inline.content.orcaPlainText())
                is OrcaInline.HtmlInline -> append(orcaHtmlInlineToPlainText(inline.html))
                is OrcaInline.Abbreviation -> append(inline.text)
            }
        }
    }
}

private val HTML_TAG_REGEX = Regex("</?[a-zA-Z][^>]*>")
private val BR_TAG_REGEX = Regex("(?i)<br\\s*/?>")

private fun orcaHtmlInlineToPlainText(html: String): String {
    return decodeBasicHtmlEntities(
        html
            .replace(BR_TAG_REGEX, "\n")
            .replace(HTML_TAG_REGEX, ""),
    )
}

private fun decodeBasicHtmlEntities(text: String): String {
    return text
        .replace("&amp;", "&")
        .replace("&nbsp;", " ")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
}

private val SLUG_STRIP_REGEX = Regex("[^\\w\\s-]")
private val SLUG_SPACES_REGEX = Regex("\\s+")

/** GitHub-style slug base for a heading's inline content (without dedup suffix). */
internal fun orcaHeadingSlugBase(content: List<OrcaInline>): String {
    return content.orcaPlainText()
        .lowercase()
        .replace(SLUG_STRIP_REGEX, "")
        .trim()
        .replace(SLUG_SPACES_REGEX, "-")
}
