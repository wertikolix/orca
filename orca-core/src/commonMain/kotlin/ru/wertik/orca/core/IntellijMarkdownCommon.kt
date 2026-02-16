package ru.wertik.orca.core

import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser

internal const val DEFAULT_MAX_TREE_DEPTH = 64
internal const val INLINE_FOOTNOTE_LABEL_PREFIX = "__inline_footnote_"

internal data class ParserCacheKey(
    val parser: MarkdownParser,
    val maxTreeDepth: Int,
)

internal class DepthLimitReporter(
    private val callback: ((Int) -> Unit)?,
) {
    private var wasReported = false

    fun report(depth: Int) {
        if (wasReported) return
        wasReported = true
        callback?.invoke(depth)
    }
}

internal fun defaultParser(): MarkdownParser {
    return MarkdownParser(GFMFlavourDescriptor())
}
