package ru.wertik.orca.compose.android

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import ru.wertik.orca.core.OrcaInline

internal fun buildInlineAnnotatedString(
    inlines: List<OrcaInline>,
    style: OrcaStyle,
    onLinkClick: (String) -> Unit,
    footnoteNumbers: Map<String, Int> = emptyMap(),
    onFootnoteClick: ((String) -> Unit)? = null,
): AnnotatedString {
    return buildAnnotatedString {
        appendInlines(
            inlines = inlines,
            style = style,
            onLinkClick = onLinkClick,
            footnoteNumbers = footnoteNumbers,
            onFootnoteClick = onFootnoteClick,
        )
    }
}

private fun AnnotatedString.Builder.appendInlines(
    inlines: List<OrcaInline>,
    style: OrcaStyle,
    onLinkClick: (String) -> Unit,
    footnoteNumbers: Map<String, Int>,
    onFootnoteClick: ((String) -> Unit)?,
) {
    inlines.forEach { inline ->
        appendInline(
            inline = inline,
            style = style,
            onLinkClick = onLinkClick,
            footnoteNumbers = footnoteNumbers,
            onFootnoteClick = onFootnoteClick,
        )
    }
}

private fun AnnotatedString.Builder.appendInline(
    inline: OrcaInline,
    style: OrcaStyle,
    onLinkClick: (String) -> Unit,
    footnoteNumbers: Map<String, Int>,
    onFootnoteClick: ((String) -> Unit)?,
) {
    when (inline) {
        is OrcaInline.Text -> append(inline.text)

        is OrcaInline.Bold -> withStyle(style = boldStyle) {
            appendInlines(
                inlines = inline.content,
                style = style,
                onLinkClick = onLinkClick,
                footnoteNumbers = footnoteNumbers,
                onFootnoteClick = onFootnoteClick,
            )
        }

        is OrcaInline.Italic -> withStyle(style = italicStyle) {
            appendInlines(
                inlines = inline.content,
                style = style,
                onLinkClick = onLinkClick,
                footnoteNumbers = footnoteNumbers,
                onFootnoteClick = onFootnoteClick,
            )
        }

        is OrcaInline.Strikethrough -> withStyle(style = style.inline.strikethrough) {
            appendInlines(
                inlines = inline.content,
                style = style,
                onLinkClick = onLinkClick,
                footnoteNumbers = footnoteNumbers,
                onFootnoteClick = onFootnoteClick,
            )
        }

        is OrcaInline.InlineCode -> withStyle(style = style.inline.inlineCode) {
            append(inline.code)
        }

        is OrcaInline.Link -> if (!isSafeLinkDestination(inline.destination)) {
            appendLinkContent(
                inline = inline,
                style = style,
                onLinkClick = onLinkClick,
                footnoteNumbers = footnoteNumbers,
                onFootnoteClick = onFootnoteClick,
            )
        } else {
            withLink(
                LinkAnnotation.Url(
                    url = inline.destination,
                    styles = TextLinkStyles(style = style.inline.link),
                    linkInteractionListener = LinkInteractionListener { annotation ->
                        val target = (annotation as? LinkAnnotation.Url)?.url ?: inline.destination
                        onLinkClick(target)
                    },
                ),
            ) {
                appendLinkContent(
                    inline = inline,
                    style = style,
                    onLinkClick = onLinkClick,
                    footnoteNumbers = footnoteNumbers,
                    onFootnoteClick = onFootnoteClick,
                )
            }
        }

        is OrcaInline.Image -> append(imageInlineFallbackText(inline))

        is OrcaInline.FootnoteReference -> {
            val text = footnoteReferenceText(inline.label, footnoteNumbers)
            val handler = onFootnoteClick
            if (handler == null) {
                withStyle(style.inline.footnoteReference) {
                    append(text)
                }
            } else {
                withLink(
                    LinkAnnotation.Url(
                        url = "orca-footnote://${inline.label}",
                        styles = TextLinkStyles(style = style.inline.footnoteReference),
                        linkInteractionListener = LinkInteractionListener {
                            handler(inline.label)
                        },
                    ),
                ) {
                    append(text)
                }
            }
        }

        is OrcaInline.HtmlInline -> append(htmlInlineFallbackText(inline.html))
    }
}

private fun AnnotatedString.Builder.appendLinkContent(
    inline: OrcaInline.Link,
    style: OrcaStyle,
    onLinkClick: (String) -> Unit,
    footnoteNumbers: Map<String, Int>,
    onFootnoteClick: ((String) -> Unit)?,
) {
    if (inline.content.isEmpty()) {
        append(inline.destination)
    } else {
        appendInlines(
            inlines = inline.content,
            style = style,
            onLinkClick = onLinkClick,
            footnoteNumbers = footnoteNumbers,
            onFootnoteClick = onFootnoteClick,
        )
    }
}

internal fun footnoteReferenceText(
    label: String,
    footnoteNumbers: Map<String, Int>,
): String {
    val number = footnoteNumbers[label]
    return if (number != null) {
        "[$number]"
    } else {
        "[${label}]"
    }
}

internal fun imageInlineFallbackText(image: OrcaInline.Image): String {
    return image.alt?.takeIf { it.isNotBlank() } ?: image.source
}

internal fun htmlInlineFallbackText(html: String): String {
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

private val HTML_TAG_REGEX = Regex("</?[a-zA-Z][^>]*>")
private val BR_TAG_REGEX = Regex("(?i)<br\\s*/?>")
