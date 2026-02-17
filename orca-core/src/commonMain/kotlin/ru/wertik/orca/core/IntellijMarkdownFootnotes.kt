package ru.wertik.orca.core

import org.intellij.markdown.parser.LinkMap
import org.intellij.markdown.parser.MarkdownParser

internal data class FootnoteDefinitionsExtraction(
    val markdown: String,
    val definitions: List<FootnoteSourceDefinition>,
)

internal data class FootnoteSourceDefinition(
    val label: String,
    val markdown: String,
)

private val FOOTNOTE_DEFINITION_REGEX = Regex("""^\[\^([^\]]+)]:\s*(.*)$""")

internal fun extractFootnoteDefinitions(markdown: String): FootnoteDefinitionsExtraction {
    val lines = markdown.split('\n')
    val bodyLines = mutableListOf<String>()
    val definitions = mutableListOf<FootnoteSourceDefinition>()
    var index = 0

    while (index < lines.size) {
        val line = lines[index]
        val match = FOOTNOTE_DEFINITION_REGEX.matchEntire(line)
        if (match == null) {
            bodyLines += line
            index += 1
            continue
        }

        val label = match.groupValues[1].trim()
        val contentLines = mutableListOf(match.groupValues[2])
        index += 1

        while (index < lines.size) {
            val continuation = lines[index]
            val stripped = stripFootnoteContinuationIndent(continuation)
            if (stripped != null) {
                contentLines += stripped
                index += 1
                continue
            }
            if (continuation.isBlank() && index + 1 < lines.size) {
                val next = lines[index + 1]
                if (stripFootnoteContinuationIndent(next) != null) {
                    contentLines += ""
                    index += 1
                    continue
                }
            }
            break
        }

        definitions += FootnoteSourceDefinition(
            label = label,
            markdown = contentLines.joinToString("\n").trimEnd(),
        )
    }

    return FootnoteDefinitionsExtraction(
        markdown = bodyLines.joinToString("\n"),
        definitions = definitions,
    )
}

internal fun mapFootnoteDefinition(
    parser: MarkdownParser,
    definition: FootnoteSourceDefinition,
    maxTreeDepth: Int,
    depthLimitReporter: DepthLimitReporter,
): OrcaFootnoteDefinition {
    if (definition.markdown.isBlank()) {
        return OrcaFootnoteDefinition(
            label = definition.label,
            blocks = emptyList(),
        )
    }

    val root = parser.buildMarkdownTreeFromString(definition.markdown)
    val mapper = IntellijTreeMapper(
        source = definition.markdown,
        parser = parser,
        linkMap = LinkMap.buildLinkMap(root, definition.markdown),
        maxTreeDepth = maxTreeDepth,
        depthLimitReporter = depthLimitReporter,
    )
    return OrcaFootnoteDefinition(
        label = definition.label,
        blocks = root.children.mapNotNull { child -> mapper.mapBlock(child, depth = 0) },
    )
}

/**
 * Strip one level of continuation indent (tab, 4+ spaces, or tab+spaces).
 * Returns the unindented line, or null if the line is not indented.
 */
private fun stripFootnoteContinuationIndent(line: String): String? {
    if (line.startsWith("\t")) return line.removePrefix("\t")
    if (line.startsWith("    ")) return line.substring(4)
    return null
}
