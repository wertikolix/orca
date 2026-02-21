package ru.wertik.orca.core

import org.intellij.markdown.parser.LinkMap
import org.intellij.markdown.parser.MarkdownParser

/**
 * Default [OrcaParser] implementation backed by IntelliJ's markdown parser.
 *
 * Supports front matter extraction, footnotes, admonitions, and an LRU parse cache.
 *
 * @param parser Underlying IntelliJ [MarkdownParser]. Defaults to [defaultParser].
 * @param maxTreeDepth Maximum allowed AST nesting depth. Defaults to [DEFAULT_MAX_TREE_DEPTH].
 * @param onDepthLimitExceeded Optional callback invoked when nesting exceeds [maxTreeDepth].
 * @param cacheSize Maximum number of cached parse results (LRU). Defaults to [DEFAULT_PARSE_CACHE_SIZE].
 * @see OrcaParser
 */
class OrcaMarkdownParser(
    private val parser: MarkdownParser = defaultParser(),
    private val maxTreeDepth: Int = DEFAULT_MAX_TREE_DEPTH,
    private val onDepthLimitExceeded: ((Int) -> Unit)? = null,
    cacheSize: Int = DEFAULT_PARSE_CACHE_SIZE,
) : OrcaParser {
    private val cache = OrcaParserCache(maxEntries = cacheSize)

    init {
        require(maxTreeDepth > 0) { "maxTreeDepth must be greater than 0" }
        require(cacheSize > 0) { "cacheSize must be greater than 0" }
    }

    override fun cacheKey(): Any = ParserCacheKey(parser, maxTreeDepth)

    override fun parse(input: String): OrcaDocument {
        return parseInternal(input).document
    }

    override fun parseWithDiagnostics(input: String): OrcaParseResult {
        return try {
            parseInternal(input)
        } catch (error: Exception) {
            OrcaParseResult(
                document = OrcaDocument(emptyList()),
                diagnostics = OrcaParseDiagnostics(
                    errors = listOf(
                        OrcaParseError.ParserFailure(
                            message = error.message ?: "Unknown parser failure",
                        ),
                    ),
                ),
            )
        }
    }

    override fun parseCached(
        key: Any,
        input: String,
    ): OrcaDocument {
        return parseCachedWithDiagnostics(
            key = key,
            input = input,
        ).document
    }

    override fun parseCachedWithDiagnostics(
        key: Any,
        input: String,
    ): OrcaParseResult {
        return cache.getOrPut(
            key = key,
            input = input,
            parse = { parseWithDiagnostics(input) },
        )
    }

    /** Evicts all entries from the parse cache. */
    fun clearCache() {
        cache.clear()
    }

    private fun parseInternal(input: String): OrcaParseResult {
        val frontMatterExtraction = extractFrontMatter(input)
        val definitionListExtraction = extractDefinitionLists(frontMatterExtraction.markdown)
        val footnoteExtraction = extractFootnoteDefinitions(definitionListExtraction.markdown)
        val root = parser.buildMarkdownTreeFromString(footnoteExtraction.markdown)
        val depthLimitReporter = DepthLimitReporter(onDepthLimitExceeded)
        val mapper = IntellijTreeMapper(
            source = footnoteExtraction.markdown,
            parser = parser,
            linkMap = LinkMap.buildLinkMap(root, footnoteExtraction.markdown),
            maxTreeDepth = maxTreeDepth,
            depthLimitReporter = depthLimitReporter,
        )

        val blocks = root.children
            .mapNotNull { child -> mapper.mapBlock(child, depth = 0) }
            .toMutableList()

        // Replace definition list placeholders with actual DefinitionList blocks.
        val defListPlaceholderRegex = Regex("""^<!--orca:deflist:(\d+)-->$""")
        val resolvedBlocks = blocks.map { block ->
            if (block is OrcaBlock.HtmlBlock) {
                val match = defListPlaceholderRegex.matchEntire(block.html)
                if (match != null) {
                    val listIndex = match.groupValues[1].toInt()
                    val source = definitionListExtraction.definitionLists.getOrNull(listIndex)
                    if (source != null) {
                        mapDefinitionList(
                            parser = parser,
                            source = source,
                            maxTreeDepth = maxTreeDepth,
                            depthLimitReporter = depthLimitReporter,
                        )
                    } else {
                        block
                    }
                } else {
                    block
                }
            } else {
                block
            }
        }.toMutableList()

        val definitionBlocks = footnoteExtraction.definitions
            .map { definition ->
                mapFootnoteDefinition(
                    parser = parser,
                    definition = definition,
                    maxTreeDepth = maxTreeDepth,
                    depthLimitReporter = depthLimitReporter,
                )
            }

        val allFootnotes = definitionBlocks + mapper.consumeInlineFootnoteDefinitions()
        if (allFootnotes.isNotEmpty()) {
            resolvedBlocks += OrcaBlock.Footnotes(definitions = allFootnotes)
        }

        val warnings = buildList {
            val exceededDepth = depthLimitReporter.exceededDepth()
            if (exceededDepth != null) {
                add(
                    OrcaParseWarning.DepthLimitExceeded(
                        maxTreeDepth = maxTreeDepth,
                        exceededDepth = exceededDepth,
                    ),
                )
            }
        }

        return OrcaParseResult(
            document = OrcaDocument(
                blocks = resolvedBlocks,
                frontMatter = frontMatterExtraction.frontMatter,
            ),
            diagnostics = OrcaParseDiagnostics(
                warnings = warnings,
            ),
        )
    }
}
