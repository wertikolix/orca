package ru.wertik.orca.core

internal data class FrontMatterExtraction(
    val markdown: String,
    val frontMatter: OrcaFrontMatter?,
)

private enum class FrontMatterFormat {
    YAML,
    TOML,
}

private const val YAML_FRONT_MATTER_DELIMITER = "---"
private const val YAML_FRONT_MATTER_ALT_END = "..."
private const val TOML_FRONT_MATTER_DELIMITER = "+++"

internal fun extractFrontMatter(input: String): FrontMatterExtraction {
    val normalized = input
        .replace("\r\n", "\n")
        .replace('\r', '\n')

    return extractDelimitedFrontMatter(
        markdown = normalized,
        openingDelimiter = YAML_FRONT_MATTER_DELIMITER,
        format = FrontMatterFormat.YAML,
    ) ?: extractDelimitedFrontMatter(
        markdown = normalized,
        openingDelimiter = TOML_FRONT_MATTER_DELIMITER,
        format = FrontMatterFormat.TOML,
    ) ?: FrontMatterExtraction(
        markdown = normalized,
        frontMatter = null,
    )
}

private fun extractDelimitedFrontMatter(
    markdown: String,
    openingDelimiter: String,
    format: FrontMatterFormat,
): FrontMatterExtraction? {
    val lines = markdown.split('\n')
    if (lines.firstOrNull() != openingDelimiter) {
        return null
    }

    var closingIndex = -1
    for (lineIndex in 1 until lines.size) {
        val line = lines[lineIndex]
        val isClosing = when (format) {
            FrontMatterFormat.YAML -> line == YAML_FRONT_MATTER_DELIMITER || line == YAML_FRONT_MATTER_ALT_END
            FrontMatterFormat.TOML -> line == TOML_FRONT_MATTER_DELIMITER
        }
        if (isClosing) {
            closingIndex = lineIndex
            break
        }
    }

    if (closingIndex == -1) {
        return null
    }

    val raw = lines.subList(1, closingIndex).joinToString("\n").trimEnd()
    val entries = parseFrontMatterEntries(
        raw = raw,
        format = format,
    )
    val frontMatter = when (format) {
        FrontMatterFormat.YAML -> OrcaFrontMatter.Yaml(raw = raw, entries = entries)
        FrontMatterFormat.TOML -> OrcaFrontMatter.Toml(raw = raw, entries = entries)
    }
    val markdownBody = lines.drop(closingIndex + 1)
        .joinToString("\n")
        .trimStart('\n')

    return FrontMatterExtraction(
        markdown = markdownBody,
        frontMatter = frontMatter,
    )
}

private fun parseFrontMatterEntries(
    raw: String,
    format: FrontMatterFormat,
): Map<String, String> {
    val separator = when (format) {
        FrontMatterFormat.YAML -> ':'
        FrontMatterFormat.TOML -> '='
    }

    val entries = linkedMapOf<String, String>()
    raw.lineSequence().forEach { line ->
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return@forEach
        if (trimmed.startsWith("#")) return@forEach
        if (format == FrontMatterFormat.YAML && trimmed.startsWith("-")) return@forEach

        val separatorIndex = trimmed.indexOf(separator)
        if (separatorIndex <= 0) return@forEach

        val key = trimmed.substring(0, separatorIndex).trim().stripMatchingQuotes()
        val value = trimmed.substring(separatorIndex + 1).trim().stripMatchingQuotes()
        if (key.isNotEmpty()) {
            entries[key] = value
        }
    }
    return entries
}

private fun String.stripMatchingQuotes(): String {
    if (length >= 2) {
        val first = first()
        val last = last()
        if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
            return substring(1, length - 1)
        }
    }
    return this
}
