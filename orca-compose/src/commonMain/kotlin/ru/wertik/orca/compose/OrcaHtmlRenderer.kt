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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

private val TAG_REGEX = Regex("""<(/?)(\w+)([^>]*)>""")
private val HREF_REGEX = Regex("""href\s*=\s*["']([^"']*)["']""")
private val ENTITY_MAP = mapOf(
    "&amp;" to "&",
    "&lt;" to "<",
    "&gt;" to ">",
    "&quot;" to "\"",
    "&#39;" to "'",
    "&apos;" to "'",
    "&nbsp;" to " ",
)

internal fun renderHtmlToAnnotatedString(
    html: String,
    style: OrcaStyle,
    onLinkClick: (String) -> Unit,
    securityPolicy: OrcaSecurityPolicy,
): AnnotatedString {
    return buildAnnotatedString {
        // Track the number of pushStyle() calls we've made on the builder so that
        // we never call pop() more times than we've pushed — which would crash on
        // malformed HTML like </b></b> without matching opening tags.
        var builderPushCount = 0
        var lastEnd = 0

        // Per-tag tracking so we only pop the style that was actually pushed for
        // a given tag name. This prevents orphan closing tags from popping styles
        // that belong to a different element.
        val tagPushStack = mutableListOf<String>()

        TAG_REGEX.findAll(html).forEach { match ->
            val beforeTag = html.substring(lastEnd, match.range.first)
            if (beforeTag.isNotEmpty()) {
                append(decodeHtmlEntities(beforeTag))
            }
            lastEnd = match.range.last + 1

            val isClosing = match.groupValues[1] == "/"
            val tagName = match.groupValues[2].lowercase()
            val attrs = match.groupValues[3]

            if (!isClosing) {
                when (tagName) {
                    "br" -> append("\n")
                    "b", "strong" -> {
                        val s = SpanStyle(fontWeight = FontWeight.Bold)
                        pushStyle(s)
                        builderPushCount++
                        tagPushStack.add(tagName)
                    }
                    "i", "em" -> {
                        val s = SpanStyle(fontStyle = FontStyle.Italic)
                        pushStyle(s)
                        builderPushCount++
                        tagPushStack.add(tagName)
                    }
                    "s", "del", "strike" -> {
                        val s = SpanStyle(textDecoration = TextDecoration.LineThrough)
                        pushStyle(s)
                        builderPushCount++
                        tagPushStack.add(tagName)
                    }
                    "u", "ins" -> {
                        val s = SpanStyle(textDecoration = TextDecoration.Underline)
                        pushStyle(s)
                        builderPushCount++
                        tagPushStack.add(tagName)
                    }
                    "code" -> {
                        val s = style.inline.inlineCode
                        pushStyle(s)
                        builderPushCount++
                        tagPushStack.add(tagName)
                    }
                    "sup" -> {
                        val s = SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 12.sp)
                        pushStyle(s)
                        builderPushCount++
                        tagPushStack.add(tagName)
                    }
                    "sub" -> {
                        val s = SpanStyle(baselineShift = BaselineShift.Subscript, fontSize = 12.sp)
                        pushStyle(s)
                        builderPushCount++
                        tagPushStack.add(tagName)
                    }
                    "mark" -> {
                        val s = SpanStyle(background = Color(0x40FFEB3B))
                        pushStyle(s)
                        builderPushCount++
                        tagPushStack.add(tagName)
                    }
                    "a" -> {
                        val href = HREF_REGEX.find(attrs)?.groupValues?.get(1).orEmpty()
                        if (href.isNotEmpty() && securityPolicy.isAllowed(OrcaUrlType.LINK, href)) {
                            val linkAnnotation = LinkAnnotation.Url(
                                url = href,
                                styles = TextLinkStyles(style = style.inline.link),
                                linkInteractionListener = LinkInteractionListener { annotation ->
                                    val target = (annotation as? LinkAnnotation.Url)?.url ?: href
                                    onLinkClick(target)
                                },
                            )
                            pushLink(linkAnnotation)
                            builderPushCount++
                            tagPushStack.add(tagName)
                        }
                    }
                    "p", "div" -> {
                        if (length > 0 && !endsWith("\n")) {
                            append("\n")
                        }
                    }
                    "h1", "h2", "h3", "h4", "h5", "h6" -> {
                        if (length > 0 && !endsWith("\n")) {
                            append("\n")
                        }
                        val s = SpanStyle(fontWeight = FontWeight.Bold)
                        pushStyle(s)
                        builderPushCount++
                        tagPushStack.add(tagName)
                    }
                    "li" -> {
                        if (length > 0 && !endsWith("\n")) {
                            append("\n")
                        }
                        append("  \u2022 ")
                    }
                    "hr" -> {
                        if (length > 0 && !endsWith("\n")) {
                            append("\n")
                        }
                        append("\u2500".repeat(20))
                        append("\n")
                    }
                    "blockquote" -> {
                        if (length > 0 && !endsWith("\n")) {
                            append("\n")
                        }
                        val s = SpanStyle(fontStyle = FontStyle.Italic, color = Color(0xFF6D6D6D))
                        pushStyle(s)
                        builderPushCount++
                        tagPushStack.add(tagName)
                    }
                    "pre" -> {
                        val s = SpanStyle(fontFamily = FontFamily.Monospace)
                        pushStyle(s)
                        builderPushCount++
                        tagPushStack.add(tagName)
                    }
                }
            } else {
                when (tagName) {
                    "b", "strong", "i", "em", "s", "del", "strike", "u", "ins",
                    "code", "sup", "sub", "mark", "a",
                    "h1", "h2", "h3", "h4", "h5", "h6",
                    "blockquote", "pre",
                    -> {
                        if (builderPushCount > 0 && tagPushStack.isNotEmpty()) {
                            val normalizedClosing = normalizeTagName(tagName)

                            // Find the matching opening tag anywhere in the stack.
                            // This handles interleaved tags like <b><i></b></i>:
                            // when </b> arrives, last pushed is "i", so we need to
                            // pop "i", pop "b" (the match), then re-push "i".
                            val matchIndex = tagPushStack.indexOfLast {
                                normalizeTagName(it) == normalizedClosing
                            }
                            if (matchIndex >= 0) {
                                // Collect tags above the match that need re-pushing.
                                val toPop = tagPushStack.size - matchIndex
                                val toRePush = mutableListOf<String>()
                                repeat(toPop) {
                                    if (builderPushCount > 0) {
                                        pop()
                                        builderPushCount--
                                        val removed = tagPushStack.removeLastOrNull()
                                        // Re-push everything except the matched tag.
                                        if (removed != null && normalizeTagName(removed) != normalizedClosing) {
                                            toRePush.add(0, removed) // prepend to preserve order
                                        }
                                    }
                                }
                                // Re-apply styles that were popped but not closed.
                                for (tag in toRePush) {
                                    val reStyle = styleForTag(tag, style)
                                    if (reStyle != null) {
                                        pushStyle(reStyle)
                                        builderPushCount++
                                        tagPushStack.add(tag)
                                    }
                                }
                            }
                        }
                    }
                    "p", "div", "li" -> {
                        if (length > 0 && !endsWith("\n")) {
                            append("\n")
                        }
                    }
                }
            }
        }

        val remaining = html.substring(lastEnd)
        if (remaining.isNotEmpty()) {
            append(decodeHtmlEntities(remaining))
        }
    }
}

private fun AnnotatedString.Builder.endsWith(suffix: String): Boolean {
    val text = toAnnotatedString().text
    return text.endsWith(suffix)
}

private fun decodeHtmlEntities(text: String): String {
    var result = text
    ENTITY_MAP.forEach { (entity, replacement) ->
        result = result.replace(entity, replacement)
    }
    return result
}

/**
 * Normalize tag names so that synonymous tags (e.g. `<b>` / `<strong>`) are treated
 * as the same when matching opening and closing tags in the push/pop stack.
 */
private fun normalizeTagName(tag: String): String {
    return when (tag) {
        "strong" -> "b"
        "em" -> "i"
        "del", "strike" -> "s"
        "ins" -> "u"
        else -> tag
    }
}

/**
 * Returns the [SpanStyle] to re-push when a tag is temporarily popped
 * to close an interleaved inner tag.
 */
private fun styleForTag(tag: String, style: OrcaStyle): SpanStyle? {
    return when (tag) {
        "b", "strong" -> SpanStyle(fontWeight = FontWeight.Bold)
        "i", "em" -> SpanStyle(fontStyle = FontStyle.Italic)
        "s", "del", "strike" -> SpanStyle(textDecoration = TextDecoration.LineThrough)
        "u", "ins" -> SpanStyle(textDecoration = TextDecoration.Underline)
        "code" -> style.inline.inlineCode
        "sup" -> SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 12.sp)
        "sub" -> SpanStyle(baselineShift = BaselineShift.Subscript, fontSize = 12.sp)
        "mark" -> SpanStyle(background = Color(0x40FFEB3B))
        "h1", "h2", "h3", "h4", "h5", "h6" -> SpanStyle(fontWeight = FontWeight.Bold)
        "blockquote" -> SpanStyle(fontStyle = FontStyle.Italic, color = Color(0xFF6D6D6D))
        "pre" -> SpanStyle(fontFamily = FontFamily.Monospace)
        else -> null
    }
}

internal fun extractHtmlPlainText(html: String): String {
    return decodeHtmlEntities(
        html
            .replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("(?i)</(p|div|li|h[1-6]|blockquote|tr|table|ul|ol)>"), "\n")
            .replace(Regex("<[^>]*>"), ""),
    ).trim()
}
