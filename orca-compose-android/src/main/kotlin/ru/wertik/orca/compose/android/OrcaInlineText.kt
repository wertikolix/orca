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
): AnnotatedString {
    return buildAnnotatedString {
        appendInlines(
            inlines = inlines,
            style = style,
            onLinkClick = onLinkClick,
            footnoteNumbers = footnoteNumbers,
        )
    }
}

private fun AnnotatedString.Builder.appendInlines(
    inlines: List<OrcaInline>,
    style: OrcaStyle,
    onLinkClick: (String) -> Unit,
    footnoteNumbers: Map<String, Int>,
) {
    inlines.forEach { inline ->
        appendInline(
            inline = inline,
            style = style,
            onLinkClick = onLinkClick,
            footnoteNumbers = footnoteNumbers,
        )
    }
}

private fun AnnotatedString.Builder.appendInline(
    inline: OrcaInline,
    style: OrcaStyle,
    onLinkClick: (String) -> Unit,
    footnoteNumbers: Map<String, Int>,
) {
    when (inline) {
        is OrcaInline.Text -> append(inline.text)

        is OrcaInline.Bold -> withStyle(style = boldStyle) {
            appendInlines(
                inlines = inline.content,
                style = style,
                onLinkClick = onLinkClick,
                footnoteNumbers = footnoteNumbers,
            )
        }

        is OrcaInline.Italic -> withStyle(style = italicStyle) {
            appendInlines(
                inlines = inline.content,
                style = style,
                onLinkClick = onLinkClick,
                footnoteNumbers = footnoteNumbers,
            )
        }

        is OrcaInline.Strikethrough -> withStyle(style = style.inline.strikethrough) {
            appendInlines(
                inlines = inline.content,
                style = style,
                onLinkClick = onLinkClick,
                footnoteNumbers = footnoteNumbers,
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
                )
            }
        }

        is OrcaInline.Image -> append(imageInlineFallbackText(inline))

        is OrcaInline.FootnoteReference -> withStyle(style.inline.footnoteReference) {
            append(footnoteReferenceText(inline.label, footnoteNumbers))
        }
    }
}

private fun AnnotatedString.Builder.appendLinkContent(
    inline: OrcaInline.Link,
    style: OrcaStyle,
    onLinkClick: (String) -> Unit,
    footnoteNumbers: Map<String, Int>,
) {
    if (inline.content.isEmpty()) {
        append(inline.destination)
    } else {
        appendInlines(
            inlines = inline.content,
            style = style,
            onLinkClick = onLinkClick,
            footnoteNumbers = footnoteNumbers,
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
