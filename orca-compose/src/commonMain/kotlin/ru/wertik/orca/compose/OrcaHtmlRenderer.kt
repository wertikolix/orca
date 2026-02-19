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
        val styleStack = mutableListOf<SpanStyle>()
        var lastEnd = 0

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
                        styleStack.add(s)
                    }
                    "i", "em" -> {
                        val s = SpanStyle(fontStyle = FontStyle.Italic)
                        pushStyle(s)
                        styleStack.add(s)
                    }
                    "s", "del", "strike" -> {
                        val s = SpanStyle(textDecoration = TextDecoration.LineThrough)
                        pushStyle(s)
                        styleStack.add(s)
                    }
                    "u", "ins" -> {
                        val s = SpanStyle(textDecoration = TextDecoration.Underline)
                        pushStyle(s)
                        styleStack.add(s)
                    }
                    "code" -> {
                        val s = style.inline.inlineCode
                        pushStyle(s)
                        styleStack.add(s)
                    }
                    "sup" -> {
                        val s = SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 12.sp)
                        pushStyle(s)
                        styleStack.add(s)
                    }
                    "sub" -> {
                        val s = SpanStyle(baselineShift = BaselineShift.Subscript, fontSize = 12.sp)
                        pushStyle(s)
                        styleStack.add(s)
                    }
                    "mark" -> {
                        val s = SpanStyle(background = Color(0x40FFEB3B))
                        pushStyle(s)
                        styleStack.add(s)
                    }
                    "a" -> {
                        val href = HREF_REGEX.find(attrs)?.groupValues?.get(1).orEmpty()
                        if (href.isNotEmpty() && securityPolicy.isAllowed(OrcaUrlType.LINK, href)) {
                            val s = style.inline.link
                            pushStyle(s)
                            styleStack.add(s)
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
                        styleStack.add(s)
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
                        styleStack.add(s)
                    }
                    "pre" -> {
                        val s = SpanStyle(fontFamily = FontFamily.Monospace)
                        pushStyle(s)
                        styleStack.add(s)
                    }
                }
            } else {
                when (tagName) {
                    "b", "strong", "i", "em", "s", "del", "strike", "u", "ins",
                    "code", "sup", "sub", "mark", "a",
                    "h1", "h2", "h3", "h4", "h5", "h6",
                    "blockquote", "pre",
                    -> {
                        if (styleStack.isNotEmpty()) {
                            pop()
                            styleStack.removeLastOrNull()
                        }
                    }
                    "p", "div", "li", "blockquote" -> {
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

internal fun extractHtmlPlainText(html: String): String {
    return decodeHtmlEntities(
        html
            .replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("(?i)</(p|div|li|h[1-6]|blockquote|tr|table|ul|ol)>"), "\n")
            .replace(Regex("<[^>]*>"), ""),
    ).trim()
}
