package ru.wertik.orca.compose

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import ru.wertik.orca.core.OrcaInline

private const val INLINE_IMAGE_ID_PREFIX = "orca-inline-img:"

internal fun inlineImageId(source: String): String {
    return "$INLINE_IMAGE_ID_PREFIX$source"
}

internal fun collectInlineImages(inlines: List<OrcaInline>): List<OrcaInline.Image> {
    val result = mutableListOf<OrcaInline.Image>()
    fun walk(items: List<OrcaInline>) {
        for (item in items) {
            when (item) {
                is OrcaInline.Image -> result.add(item)
                is OrcaInline.Bold -> walk(item.content)
                is OrcaInline.Italic -> walk(item.content)
                is OrcaInline.Strikethrough -> walk(item.content)
                is OrcaInline.Link -> walk(item.content)
                is OrcaInline.Superscript -> walk(item.content)
                is OrcaInline.Subscript -> walk(item.content)
                else -> {}
            }
        }
    }
    walk(inlines)
    return result
}

internal fun buildInlineImageMap(
    inlines: List<OrcaInline>,
    style: OrcaStyle,
    securityPolicy: OrcaSecurityPolicy,
): Map<String, InlineTextContent> {
    val images = collectInlineImages(inlines)
    if (images.isEmpty()) return emptyMap()

    val seen = mutableSetOf<String>()
    val map = mutableMapOf<String, InlineTextContent>()

    for (image in images) {
        if (!seen.add(image.source)) continue
        val safeSource = image.source.takeIf { source ->
            securityPolicy.isAllowed(OrcaUrlType.IMAGE, source)
        } ?: continue

        val id = inlineImageId(image.source)
        val widthSp = style.inlineImage.widthSp
        val heightSp = style.inlineImage.heightSp

        map[id] = InlineTextContent(
            placeholder = Placeholder(
                width = widthSp,
                height = heightSp,
                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
            ),
        ) { _ ->
            AsyncImage(
                model = safeSource,
                contentDescription = image.alt,
                modifier = Modifier
                    .size(style.inlineImage.size)
                    .clip(style.inlineImage.shape),
            )
        }
    }
    return map
}
