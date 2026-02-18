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
    // Use a sorted interval list for O(log n) overlap checks instead of O(n) per match
    val result = mutableListOf<HighlightToken>()

    fun overlapsExisting(start: Int, endExclusive: Int): Boolean {
        // Binary search for the insertion point by start index
        // An interval [a, b) overlaps [start, endExclusive) iff a < endExclusive && b > start
        for (token in result) {
            if (token.start >= endExclusive) break  // result is sorted by start; no further overlap possible
            if (token.endExclusive > start) return true
        }
        return false
    }

    fun insertSorted(token: HighlightToken) {
        // Insert while maintaining sort order by start
        val insertIndex = result.binarySearchInsertionPoint(token.start)
        result.add(insertIndex, token)
    }

    fun addMatches(regex: Regex, tokenStyle: SpanStyle) {
        regex.findAll(code).forEach { match ->
            val range = match.range
            if (range.isEmpty()) return@forEach
            if (range.last >= code.length) return@forEach
            val start = range.first
            val endExclusive = range.last + 1
            if (overlapsExisting(start, endExclusive)) return@forEach
            insertSorted(HighlightToken(
                start = start,
                endExclusive = endExclusive,
                style = tokenStyle,
            ))
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
            if (range.last >= code.length) return@forEach
            val start = range.first
            val endExclusive = range.last + 1
            if (overlapsExisting(start, endExclusive)) return@forEach
            insertSorted(HighlightToken(
                start = start,
                endExclusive = endExclusive,
                style = style.code.highlightKeyword,
            ))
        }
    }

    return result
}

/**
 * Binary search for the insertion point where a token with the given [start] should be placed
 * to keep the list sorted by start index.
 */
private fun List<HighlightToken>.binarySearchInsertionPoint(start: Int): Int {
    var low = 0
    var high = size
    while (low < high) {
        val mid = (low + high) ushr 1
        if (this[mid].start < start) low = mid + 1 else high = mid
    }
    return low
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

        "python" -> setOf(
            "def", "class", "import", "from", "as", "if", "elif", "else", "for", "while", "break", "continue",
            "return", "yield", "try", "except", "finally", "raise", "with", "lambda", "pass", "del",
            "None", "True", "False", "and", "or", "not", "in", "is",
        )

        "go" -> setOf(
            "func", "var", "const", "type", "struct", "interface", "map", "chan", "package", "import",
            "if", "else", "for", "range", "switch", "case", "default", "return", "break", "continue",
            "goto", "defer", "go", "select", "nil", "true", "false",
        )

        "rust" -> setOf(
            "fn", "let", "mut", "const", "struct", "enum", "trait", "impl", "use", "pub", "mod",
            "if", "else", "match", "for", "while", "loop", "return", "break", "continue",
            "true", "false", "None", "Some", "Ok", "Err", "self", "Self", "super",
        )

        "c", "cpp" -> setOf(
            "int", "float", "double", "char", "bool", "void", "long", "short", "unsigned", "signed",
            "if", "else", "for", "while", "do", "switch", "case", "default", "return", "break", "continue",
            "struct", "union", "enum", "typedef", "const", "static", "extern", "inline",
            "true", "false", "null", "NULL",
        )

        "swift" -> setOf(
            "func", "var", "let", "class", "struct", "enum", "protocol", "extension", "import",
            "if", "else", "for", "while", "switch", "case", "return", "break", "continue",
            "true", "false", "nil", "self", "super", "guard", "defer", "throw", "try", "catch",
        )

        "csharp" -> setOf(
            "class", "interface", "struct", "enum", "namespace", "using", "public", "private", "protected",
            "static", "readonly", "const", "void", "if", "else", "for", "foreach", "while", "switch",
            "case", "return", "break", "continue", "new", "null", "true", "false", "var", "async", "await",
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
