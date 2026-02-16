package ru.wertik.orca.core

import org.intellij.markdown.parser.LinkMap
import org.intellij.markdown.parser.MarkdownParser

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
        return cache.getOrPut(
            key = key,
            input = input,
            parse = { parseInternal(input) },
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

    fun clearCache() {
        cache.clear()
    }

    private fun parseInternal(input: String): OrcaParseResult {
        val frontMatterExtraction = extractFrontMatter(input)
        val footnoteExtraction = extractFootnoteDefinitions(frontMatterExtraction.markdown)
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
            blocks += OrcaBlock.Footnotes(definitions = allFootnotes)
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
                blocks = blocks,
                frontMatter = frontMatterExtraction.frontMatter,
            ),
            diagnostics = OrcaParseDiagnostics(
                warnings = warnings,
            ),
        )
    }
}
