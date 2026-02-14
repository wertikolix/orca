package ru.wertik.fhmd.compose.android

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import ru.wertik.fhmd.core.FhMdInline

internal fun buildInlineAnnotatedString(
    inlines: List<FhMdInline>,
    style: FhMdStyle,
    onLinkClick: (String) -> Unit,
): AnnotatedString {
    return buildAnnotatedString {
        appendInlines(
            inlines = inlines,
            style = style,
            onLinkClick = onLinkClick,
        )
    }
}

private fun AnnotatedString.Builder.appendInlines(
    inlines: List<FhMdInline>,
    style: FhMdStyle,
    onLinkClick: (String) -> Unit,
) {
    inlines.forEach { inline ->
        appendInline(inline, style, onLinkClick)
    }
}

private fun AnnotatedString.Builder.appendInline(
    inline: FhMdInline,
    style: FhMdStyle,
    onLinkClick: (String) -> Unit,
) {
    when (inline) {
        is FhMdInline.Text -> append(inline.text)

        is FhMdInline.Bold -> withStyle(style = boldStyle) {
            appendInlines(
                inlines = inline.content,
                style = style,
                onLinkClick = onLinkClick,
            )
        }

        is FhMdInline.Italic -> withStyle(style = italicStyle) {
            appendInlines(
                inlines = inline.content,
                style = style,
                onLinkClick = onLinkClick,
            )
        }

        is FhMdInline.Strikethrough -> withStyle(style = style.strikethroughStyle) {
            appendInlines(
                inlines = inline.content,
                style = style,
                onLinkClick = onLinkClick,
            )
        }

        is FhMdInline.InlineCode -> withStyle(style = style.inlineCode) {
            append(inline.code)
        }

        is FhMdInline.Link -> if (!isSafeLinkDestination(inline.destination)) {
            appendLinkContent(
                inline = inline,
                style = style,
                onLinkClick = onLinkClick,
            )
        } else {
            withLink(
                LinkAnnotation.Url(
                    url = inline.destination,
                    styles = TextLinkStyles(style = style.linkStyle),
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
                )
            }
        }

        is FhMdInline.Image -> append(imageInlineFallbackText(inline))
    }
}

private fun AnnotatedString.Builder.appendLinkContent(
    inline: FhMdInline.Link,
    style: FhMdStyle,
    onLinkClick: (String) -> Unit,
) {
    if (inline.content.isEmpty()) {
        append(inline.destination)
    } else {
        appendInlines(
            inlines = inline.content,
            style = style,
            onLinkClick = onLinkClick,
        )
    }
}

internal fun imageInlineFallbackText(image: FhMdInline.Image): String {
    return image.alt?.takeIf { it.isNotBlank() } ?: image.source
}
