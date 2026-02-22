package ru.wertik.orca.core

import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser

internal const val DEFAULT_MAX_TREE_DEPTH = 64
internal const val DEFAULT_PARSE_CACHE_SIZE = 64
internal const val INLINE_FOOTNOTE_LABEL_PREFIX = "__inline_footnote_"

internal data class ParserCacheKey(
    val parser: MarkdownParser,
    val maxTreeDepth: Int,
    val enableSuperscript: Boolean = true,
    val enableSubscript: Boolean = true,
)

internal class DepthLimitReporter(
    private val callback: ((Int) -> Unit)?,
) {
    private var exceededDepth: Int? = null

    fun report(depth: Int) {
        if (exceededDepth != null) return
        exceededDepth = depth
        callback?.invoke(depth)
    }

    fun exceededDepth(): Int? = exceededDepth
}

internal fun defaultParser(): MarkdownParser {
    return MarkdownParser(GFMFlavourDescriptor())
}
