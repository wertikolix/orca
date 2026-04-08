package ru.wertik.orca.compose

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import ru.wertik.orca.core.OrcaInline

internal fun buildInlineAnnotatedString(
    inlines: List<OrcaInline>,
    style: OrcaStyle,
    onLinkClick: (String) -> Unit,
    securityPolicy: OrcaSecurityPolicy = OrcaSecurityPolicies.Default,
    footnoteNumbers: Map<String, Int> = emptyMap(),
    onFootnoteClick: ((String) -> Unit)? = null,
): AnnotatedString {
    return buildAnnotatedString {
        appendInlines(
            inlines = inlines,
            style = style,
            onLinkClick = onLinkClick,
            securityPolicy = securityPolicy,
            footnoteNumbers = footnoteNumbers,
            onFootnoteClick = onFootnoteClick,
        )
    }
}

private fun AnnotatedString.Builder.appendInlines(
    inlines: List<OrcaInline>,
    style: OrcaStyle,
    onLinkClick: (String) -> Unit,
    securityPolicy: OrcaSecurityPolicy,
    footnoteNumbers: Map<String, Int>,
    onFootnoteClick: ((String) -> Unit)?,
    htmlTagStack: MutableList<String> = mutableListOf(),
) {
    inlines.forEach { inline ->
        appendInline(
            inline = inline,
            style = style,
            onLinkClick = onLinkClick,
            securityPolicy = securityPolicy,
            footnoteNumbers = footnoteNumbers,
            onFootnoteClick = onFootnoteClick,
            htmlTagStack = htmlTagStack,
        )
    }
}

private fun AnnotatedString.Builder.appendInline(
    inline: OrcaInline,
    style: OrcaStyle,
    onLinkClick: (String) -> Unit,
    securityPolicy: OrcaSecurityPolicy,
    footnoteNumbers: Map<String, Int>,
    onFootnoteClick: ((String) -> Unit)?,
    htmlTagStack: MutableList<String>,
) {
    when (inline) {
        is OrcaInline.Text -> append(inline.text)

        is OrcaInline.Bold -> withStyle(style = boldStyle) {
            appendInlines(
                inlines = inline.content,
                style = style,
                onLinkClick = onLinkClick,
                securityPolicy = securityPolicy,
                footnoteNumbers = footnoteNumbers,
                onFootnoteClick = onFootnoteClick,
            )
        }

        is OrcaInline.Italic -> withStyle(style = italicStyle) {
            appendInlines(
                inlines = inline.content,
                style = style,
                onLinkClick = onLinkClick,
                securityPolicy = securityPolicy,
                footnoteNumbers = footnoteNumbers,
                onFootnoteClick = onFootnoteClick,
            )
        }

        is OrcaInline.Strikethrough -> withStyle(style = style.inline.strikethrough) {
            appendInlines(
                inlines = inline.content,
                style = style,
                onLinkClick = onLinkClick,
                securityPolicy = securityPolicy,
                footnoteNumbers = footnoteNumbers,
                onFootnoteClick = onFootnoteClick,
            )
        }

        is OrcaInline.InlineCode -> withStyle(style = style.inline.inlineCode) {
            append(inline.code)
        }

        is OrcaInline.Link -> if (!securityPolicy.isAllowed(OrcaUrlType.LINK, inline.destination)) {
            appendLinkContent(
                inline = inline,
                style = style,
                onLinkClick = onLinkClick,
                securityPolicy = securityPolicy,
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
                    securityPolicy = securityPolicy,
                    footnoteNumbers = footnoteNumbers,
                    onFootnoteClick = onFootnoteClick,
                )
            }
        }

        is OrcaInline.Image -> appendInlineContent(inlineImageId(inline.source), inline.alt ?: inline.source)

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

        is OrcaInline.Superscript -> withStyle(style = style.inline.superscript) {
            appendInlines(
                inlines = inline.content,
                style = style,
                onLinkClick = onLinkClick,
                securityPolicy = securityPolicy,
                footnoteNumbers = footnoteNumbers,
                onFootnoteClick = onFootnoteClick,
            )
        }

        is OrcaInline.Subscript -> withStyle(style = style.inline.subscript) {
            appendInlines(
                inlines = inline.content,
                style = style,
                onLinkClick = onLinkClick,
                securityPolicy = securityPolicy,
                footnoteNumbers = footnoteNumbers,
                onFootnoteClick = onFootnoteClick,
            )
        }

        is OrcaInline.Highlight -> withStyle(style = style.inline.highlight) {
            appendInlines(
                inlines = inline.content,
                style = style,
                onLinkClick = onLinkClick,
                securityPolicy = securityPolicy,
                footnoteNumbers = footnoteNumbers,
                onFootnoteClick = onFootnoteClick,
            )
        }

        is OrcaInline.HtmlInline -> {
            val tag = parseHtmlInlineTag(inline.html)
            if (tag != null) {
                if (!tag.isClosing) {
                    val spanStyle = htmlInlineStyleForTag(tag.name, style)
                    if (spanStyle != null) {
                        pushStyle(spanStyle)
                        htmlTagStack.add(tag.name)
                    }
                } else {
                    val normalized = normalizeHtmlTagName(tag.name)
                    val idx = htmlTagStack.indexOfLast { normalizeHtmlTagName(it) == normalized }
                    if (idx >= 0) {
                        val toPop = htmlTagStack.size - idx
                        val toRePush = mutableListOf<String>()
                        repeat(toPop) {
                            pop()
                            val removed = htmlTagStack.removeLastOrNull()
                            if (removed != null && normalizeHtmlTagName(removed) != normalized) {
                                toRePush.add(0, removed)
                            }
                        }
                        for (t in toRePush) {
                            val s = htmlInlineStyleForTag(t, style) ?: continue
                            pushStyle(s)
                            htmlTagStack.add(t)
                        }
                    }
                }
            } else {
                val plainText = htmlInlineFallbackText(inline.html)
                if (plainText.isNotEmpty()) append(plainText)
            }
        }

        is OrcaInline.Abbreviation -> withStyle(style = style.inline.abbreviation) {
            append(inline.text)
        }
    }
}

private fun AnnotatedString.Builder.appendLinkContent(
    inline: OrcaInline.Link,
    style: OrcaStyle,
    onLinkClick: (String) -> Unit,
    securityPolicy: OrcaSecurityPolicy,
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
            securityPolicy = securityPolicy,
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

private data class HtmlInlineTag(val name: String, val isClosing: Boolean)

private val INLINE_TAG_REGEX = Regex("""^<(/?)(\w+)[^>]*>$""")

private fun parseHtmlInlineTag(html: String): HtmlInlineTag? {
    val m = INLINE_TAG_REGEX.find(html.trim()) ?: return null
    return HtmlInlineTag(name = m.groupValues[2].lowercase(), isClosing = m.groupValues[1] == "/")
}

private fun normalizeHtmlTagName(tag: String): String = when (tag) {
    "strong" -> "b"
    "em" -> "i"
    "del", "strike" -> "s"
    "ins" -> "u"
    else -> tag
}

private fun htmlInlineStyleForTag(tag: String, style: OrcaStyle): SpanStyle? = when (tag) {
    "b", "strong" -> SpanStyle(fontWeight = FontWeight.Bold)
    "i", "em" -> SpanStyle(fontStyle = FontStyle.Italic)
    "s", "del", "strike" -> SpanStyle(textDecoration = TextDecoration.LineThrough)
    "u", "ins" -> SpanStyle(textDecoration = TextDecoration.Underline)
    "code" -> style.inline.inlineCode
    "sup" -> SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 12.sp)
    "sub" -> SpanStyle(baselineShift = BaselineShift.Subscript, fontSize = 12.sp)
    "mark" -> SpanStyle(background = Color(0x40FFEB3B))
    "kbd" -> SpanStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, background = Color(0x1A000000))
    else -> null
}

