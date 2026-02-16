package ru.wertik.orca.core

import org.intellij.markdown.parser.LinkMap
import org.intellij.markdown.parser.MarkdownParser

class IntellijMarkdownOrcaParser(
    private val parser: MarkdownParser = defaultParser(),
    private val maxTreeDepth: Int = DEFAULT_MAX_TREE_DEPTH,
    private val onDepthLimitExceeded: ((Int) -> Unit)? = null,
) : OrcaParser {

    init {
        require(maxTreeDepth > 0) { "maxTreeDepth must be greater than 0" }
    }

    override fun cacheKey(): Any = ParserCacheKey(parser, maxTreeDepth)

    override fun parse(input: String): OrcaDocument {
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

        return OrcaDocument(
            blocks = blocks,
            frontMatter = frontMatterExtraction.frontMatter,
        )
    }
}
