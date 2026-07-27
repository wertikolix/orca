package ru.wertik.orca.core

internal data class MathBlockExtraction(
    val markdown: String,
    val mathBlocks: List<String>,
)

internal fun extractMathBlocks(markdown: String): MathBlockExtraction {
    val lines = markdown.lines()
    val output = mutableListOf<String>()
    val blocks = mutableListOf<String>()
    var index = 0
    var activeFence: String? = null

    while (index < lines.size) {
        val trimmed = lines[index].trim()
        val fence = trimmed.takeIf { it.startsWith("```") || it.startsWith("~~~") }?.take(3)
        if (fence != null) {
            activeFence = if (activeFence == null) fence else if (activeFence == fence) null else activeFence
            output += lines[index]
            index += 1
            continue
        }
        if (activeFence != null) {
            output += lines[index]
            index += 1
            continue
        }
        if (trimmed.startsWith("$$") && trimmed.endsWith("$$") && trimmed.length > 4) {
            blocks += trimmed.removePrefix("$$").removeSuffix("$$").trim()
            output += "<!--orca:math:${blocks.lastIndex}-->"
            index += 1
            continue
        }
        if (trimmed == "$$") {
            val closeIndex = (index + 1 until lines.size).firstOrNull { lines[it].trim() == "$$" }
            if (closeIndex != null) {
                blocks += lines.subList(index + 1, closeIndex).joinToString("\n").trim()
                output += "<!--orca:math:${blocks.lastIndex}-->"
                index = closeIndex + 1
                continue
            }
        }
        output += lines[index]
        index += 1
    }

    return MathBlockExtraction(markdown = output.joinToString("\n"), mathBlocks = blocks)
}

internal fun processInlineMathSyntax(inlines: List<OrcaInline>): List<OrcaInline> {
    if (!inlines.needsRewrite { text -> text.contains('$') }) return inlines

    return inlines.flatMap { inline ->
        when (inline) {
            is OrcaInline.Text -> parseInlineMathText(inline.text)
            is OrcaInline.Bold -> listOf(inline.copy(content = processInlineMathSyntax(inline.content)))
            is OrcaInline.Italic -> listOf(inline.copy(content = processInlineMathSyntax(inline.content)))
            is OrcaInline.Strikethrough -> listOf(inline.copy(content = processInlineMathSyntax(inline.content)))
            is OrcaInline.Link -> listOf(inline.copy(content = processInlineMathSyntax(inline.content)))
            is OrcaInline.Superscript -> listOf(inline.copy(content = processInlineMathSyntax(inline.content)))
            is OrcaInline.Subscript -> listOf(inline.copy(content = processInlineMathSyntax(inline.content)))
            is OrcaInline.Highlight -> listOf(inline.copy(content = processInlineMathSyntax(inline.content)))
            is OrcaInline.Underline -> listOf(inline.copy(content = processInlineMathSyntax(inline.content)))
            else -> listOf(inline)
        }
    }
}

private fun parseInlineMathText(text: String): List<OrcaInline> {
    if ('$' !in text) return listOf(OrcaInline.Text(text))
    val output = mutableListOf<OrcaInline>()
    val plain = StringBuilder()
    var index = 0

    fun flushPlain() {
        if (plain.isNotEmpty()) {
            output += OrcaInline.Text(plain.toString())
            plain.clear()
        }
    }

    while (index < text.length) {
        if (text[index] != '$' || text.isEscaped(index) || text.getOrNull(index + 1) == '$') {
            plain.append(text[index])
            index += 1
            continue
        }
        val contentStart = index + 1
        if (text.getOrNull(contentStart)?.isWhitespace() != false) {
            plain.append('$')
            index += 1
            continue
        }
        var closing = contentStart
        while (closing < text.length) {
            closing = text.indexOf('$', closing)
            if (closing < 0) break
            if (!text.isEscaped(closing) && text.getOrNull(closing - 1)?.isWhitespace() != true) break
            closing += 1
        }
        if (closing <= contentStart) {
            plain.append('$')
            index += 1
            continue
        }
        val source = text.substring(contentStart, closing)
        if ('\n' in source || source.isBlank() || source.isProbableCurrencyAmount()) {
            plain.append('$')
            index += 1
            continue
        }
        flushPlain()
        output += OrcaInline.Math(source)
        index = closing + 1
    }
    flushPlain()
    return output
}

private fun String.isProbableCurrencyAmount(): Boolean {
    if (firstOrNull()?.isDigit() != true) return false
    val numericPrefix = takeWhile { character -> character.isDigit() || character == '.' || character == ',' }
    if (numericPrefix.length == length) return true
    if (getOrNull(numericPrefix.length)?.isWhitespace() != true) return false
    return drop(numericPrefix.length).none { character ->
        character == '\\' || character in "+-=*/^_{}<>"
    }
}

private fun String.isEscaped(index: Int): Boolean {
    var precedingSlashes = 0
    var cursor = index - 1
    while (cursor >= 0 && this[cursor] == '\\') {
        precedingSlashes += 1
        cursor -= 1
    }
    return precedingSlashes % 2 == 1
}
