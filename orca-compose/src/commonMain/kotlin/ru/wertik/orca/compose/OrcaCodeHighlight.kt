package ru.wertik.orca.compose

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString

internal fun buildCodeAnnotatedString(
    code: String,
    language: String?,
    style: OrcaStyle,
): AnnotatedString {
    if (!style.code.syntaxHighlightingEnabled || code.isEmpty()) {
        return AnnotatedString(code)
    }

    val normalizedLanguage = language
        ?.trim()
        ?.lowercase()
        ?.takeIf { it.isNotEmpty() }
        ?: return AnnotatedString(code)

    val highlights = detectHighlights(
        code = code,
        language = normalizedLanguage,
        style = style,
    )
    if (highlights.isEmpty()) {
        return AnnotatedString(code)
    }

    return buildAnnotatedString {
        append(code)
        highlights.forEach { token ->
            addStyle(
                style = token.style,
                start = token.start,
                end = token.endExclusive,
            )
        }
    }
}

private data class HighlightToken(
    val start: Int,
    val endExclusive: Int,
    val style: SpanStyle,
)

private fun detectHighlights(
    code: String,
    language: String,
    style: OrcaStyle,
): List<HighlightToken> {
    val occupied = BooleanArray(code.length)
    val result = mutableListOf<HighlightToken>()

    fun addMatches(regex: Regex, tokenStyle: SpanStyle) {
        regex.findAll(code).forEach { match ->
            val range = match.range
            if (range.isEmpty()) return@forEach
            if (range.last >= occupied.size) return@forEach
            if ((range.first..range.last).any { occupied[it] }) return@forEach
            (range.first..range.last).forEach { index -> occupied[index] = true }
            result += HighlightToken(
                start = range.first,
                endExclusive = range.last + 1,
                style = tokenStyle,
            )
        }
    }

    commentRegexes(language).forEach { regex ->
        addMatches(regex, style.code.highlightComment)
    }
    addMatches(STRING_REGEX, style.code.highlightString)
    addMatches(NUMBER_REGEX, style.code.highlightNumber)

    val keywords = keywordsFor(language)
    if (keywords.isNotEmpty()) {
        WORD_REGEX.findAll(code).forEach { match ->
            if (match.value !in keywords) return@forEach
            val range = match.range
            if (range.last >= occupied.size) return@forEach
            if ((range.first..range.last).any { occupied[it] }) return@forEach
            (range.first..range.last).forEach { index -> occupied[index] = true }
            result += HighlightToken(
                start = range.first,
                endExclusive = range.last + 1,
                style = style.code.highlightKeyword,
            )
        }
    }

    return result.sortedBy { token -> token.start }
}

private fun commentRegexes(language: String): List<Regex> {
    return when (language) {
        "kotlin", "java", "js", "javascript", "ts", "typescript", "c", "cpp", "csharp", "swift", "go", "rust" -> {
            listOf(SLASH_LINE_COMMENT_REGEX, BLOCK_COMMENT_REGEX)
        }

        "sql" -> listOf(SQL_LINE_COMMENT_REGEX)
        "bash", "sh", "zsh", "shell", "python", "yaml", "yml", "toml", "properties" -> listOf(HASH_LINE_COMMENT_REGEX)
        else -> emptyList()
    }
}

private fun keywordsFor(language: String): Set<String> {
    return when (language) {
        "kotlin" -> setOf(
            "fun", "val", "var", "class", "object", "interface", "data", "sealed", "enum",
            "if", "else", "when", "for", "while", "do", "return", "in", "is", "as", "try", "catch", "finally",
            "null", "true", "false", "private", "public", "internal", "protected", "suspend", "inline",
        )

        "java" -> setOf(
            "class", "interface", "enum", "public", "private", "protected", "static", "final", "void",
            "if", "else", "switch", "case", "for", "while", "do", "return", "new", "try", "catch", "finally",
            "null", "true", "false", "extends", "implements",
        )

        "js", "javascript", "ts", "typescript" -> setOf(
            "function", "const", "let", "var", "class", "extends", "import", "export", "from",
            "if", "else", "switch", "case", "for", "while", "do", "return", "new", "try", "catch", "finally",
            "null", "true", "false", "async", "await",
        )

        "json" -> setOf("true", "false", "null")
        "sql" -> setOf(
            "select", "from", "where", "join", "left", "right", "inner", "outer",
            "insert", "into", "update", "delete", "create", "table", "alter", "drop",
            "group", "by", "order", "having", "limit", "and", "or", "not", "as",
        )

        "bash", "sh", "zsh", "shell" -> setOf(
            "if", "then", "else", "fi", "for", "in", "do", "done", "case", "esac", "function", "local", "export",
        )

        else -> emptySet()
    }
}

private val WORD_REGEX = Regex("""\b[A-Za-z_][A-Za-z0-9_]*\b""")
private val NUMBER_REGEX = Regex("""\b\d+(?:\.\d+)?\b""")
private val STRING_REGEX = Regex("""("(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*')""")
private val SLASH_LINE_COMMENT_REGEX = Regex("""//.*$""", setOf(RegexOption.MULTILINE))
private val HASH_LINE_COMMENT_REGEX = Regex("""#.*$""", setOf(RegexOption.MULTILINE))
private val SQL_LINE_COMMENT_REGEX = Regex("""--.*$""", setOf(RegexOption.MULTILINE))
private val BLOCK_COMMENT_REGEX = Regex("""/\*[\s\S]*?\*/""")
