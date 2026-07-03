package ru.wertik.orca.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.em
import ru.wertik.orca.core.OrcaInline

private const val INLINE_MATH_ID_PREFIX = "orca-inline-math:"

/** Supplies exact inline layout bounds when a math renderer can measure its output. */
typealias OrcaInlineMathPlaceholder = (source: String) -> Placeholder?

internal val LocalOrcaInlineMathPlaceholder = staticCompositionLocalOf<OrcaInlineMathPlaceholder?> { null }

internal fun inlineMathId(source: String): String = "$INLINE_MATH_ID_PREFIX$source"

internal fun buildInlineMathMap(
    inlines: List<OrcaInline>,
    inlineMathContent: OrcaMathContent?,
    inlineMathPlaceholder: OrcaInlineMathPlaceholder? = null,
): Map<String, InlineTextContent> {
    if (inlineMathContent == null) return emptyMap()
    val sources = buildList { collectInlineMath(inlines, this) }.distinct()
    return sources.associate { source ->
        inlineMathId(source) to InlineTextContent(
            placeholder = inlineMathPlaceholder?.invoke(source) ?: Placeholder(
                width = (source.length.coerceAtLeast(2) * 0.55f).em,
                height = 1.45.em,
                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
            ),
        ) { Box { inlineMathContent(source) } }
    }
}

private fun collectInlineMath(inlines: List<OrcaInline>, output: MutableList<String>) {
    inlines.forEach { inline ->
        when (inline) {
            is OrcaInline.Math -> output += inline.source
            is OrcaInline.Bold -> collectInlineMath(inline.content, output)
            is OrcaInline.Italic -> collectInlineMath(inline.content, output)
            is OrcaInline.Strikethrough -> collectInlineMath(inline.content, output)
            is OrcaInline.Link -> collectInlineMath(inline.content, output)
            is OrcaInline.Superscript -> collectInlineMath(inline.content, output)
            is OrcaInline.Subscript -> collectInlineMath(inline.content, output)
            is OrcaInline.Highlight -> collectInlineMath(inline.content, output)
            is OrcaInline.Underline -> collectInlineMath(inline.content, output)
            else -> Unit
        }
    }
}
