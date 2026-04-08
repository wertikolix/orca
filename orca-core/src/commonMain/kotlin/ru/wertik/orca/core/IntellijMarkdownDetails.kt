package ru.wertik.orca.core

import org.intellij.markdown.parser.LinkMap
import org.intellij.markdown.parser.MarkdownParser

internal data class DetailsExtraction(
    val markdown: String,
    val detailsBlocks: List<DetailsSource>,
)

internal data class DetailsSource(
    val summary: String?,
    val contentMarkdown: String,
    val startOpen: Boolean,
)

private val DETAILS_OPEN_REGEX = Regex("""^\s*<details(\s[^>]*)?>""", RegexOption.IGNORE_CASE)
private val DETAILS_CLOSE_REGEX = Regex("""^\s*</details\s*>""", RegexOption.IGNORE_CASE)
private val SUMMARY_REGEX = Regex("""<summary>(.*?)</summary>""", RegexOption.IGNORE_CASE)
private val OPEN_ATTR_REGEX = Regex("""\bopen\b""", RegexOption.IGNORE_CASE)

internal fun extractDetailsBlocks(markdown: String): DetailsExtraction {
    val lines = markdown.split('\n')
    val bodyLines = mutableListOf<String>()
    val detailsBlocks = mutableListOf<DetailsSource>()
    var i = 0

    while (i < lines.size) {
        val openMatch = DETAILS_OPEN_REGEX.find(lines[i])
        if (openMatch != null) {
            val attrs = openMatch.groupValues.getOrElse(1) { "" }
            val startOpen = OPEN_ATTR_REGEX.containsMatchIn(attrs)
            var summary: String? = null
            val contentLines = mutableListOf<String>()

            // rest of the opening line after <details>
            val afterOpen = lines[i].substring(openMatch.range.last + 1).trim()
            if (afterOpen.isNotEmpty()) contentLines += afterOpen

            i++
            var depth = 1
            while (i < lines.size && depth > 0) {
                if (DETAILS_CLOSE_REGEX.containsMatchIn(lines[i])) {
                    depth--
                    if (depth == 0) { i++; break }
                }
                if (DETAILS_OPEN_REGEX.containsMatchIn(lines[i])) depth++
                contentLines += lines[i]
                i++
            }

            // pull <summary> from content lines
            val joined = contentLines.toMutableList()
            val summaryIdx = joined.indexOfFirst { SUMMARY_REGEX.containsMatchIn(it) }
            if (summaryIdx >= 0) {
                val m = SUMMARY_REGEX.find(joined[summaryIdx])!!
                summary = m.groupValues[1].trim()
                joined[summaryIdx] = joined[summaryIdx].replace(m.value, "").trim()
                if (joined[summaryIdx].isBlank()) joined.removeAt(summaryIdx)
            }

            val contentMd = joined.joinToString("\n").trim()
            val placeholder = "<!--orca:details:${detailsBlocks.size}-->"
            detailsBlocks += DetailsSource(summary = summary, contentMarkdown = contentMd, startOpen = startOpen)
            bodyLines += placeholder
        } else {
            bodyLines += lines[i]
            i++
        }
    }

    return DetailsExtraction(
        markdown = bodyLines.joinToString("\n"),
        detailsBlocks = detailsBlocks,
    )
}

internal fun mapDetailsBlock(
    parser: MarkdownParser,
    source: DetailsSource,
    maxTreeDepth: Int,
    depthLimitReporter: DepthLimitReporter,
): OrcaBlock.Details {
    val blocks = if (source.contentMarkdown.isBlank()) {
        emptyList()
    } else {
        val root = parser.buildMarkdownTreeFromString(source.contentMarkdown)
        val mapper = IntellijTreeMapper(
            source = source.contentMarkdown,
            parser = parser,
            linkMap = LinkMap.buildLinkMap(root, source.contentMarkdown),
            maxTreeDepth = maxTreeDepth,
            depthLimitReporter = depthLimitReporter,
        )
        root.children.mapNotNull { child -> mapper.mapBlock(child, depth = 0) }
    }
    val summaryInlines = if (source.summary != null) {
        parseInlinesFromMarkdown(parser, source.summary, maxTreeDepth, depthLimitReporter)
    } else {
        emptyList()
    }
    return OrcaBlock.Details(
        summary = summaryInlines,
        blocks = blocks,
        startOpen = source.startOpen,
    )
}
