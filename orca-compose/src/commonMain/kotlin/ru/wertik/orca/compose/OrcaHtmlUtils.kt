package ru.wertik.orca.compose

internal val HTML_TAG_REGEX = Regex("</?[a-zA-Z][^>]*>")
internal val BR_TAG_REGEX = Regex("(?i)<br\\s*/?>")
internal val BLOCK_BREAK_TAG_REGEX = Regex("(?i)</(p|div|li|h[1-6]|blockquote|tr|table|ul|ol)>")

private val BASIC_NUMERIC_DEC = Regex("""&#(\d+);""")
private val BASIC_NUMERIC_HEX = Regex("""&#x([0-9a-fA-F]+);""")

internal fun decodeBasicHtmlEntities(text: String): String {
    var result = text
        .replace("&amp;", "&")
        .replace("&nbsp;", " ")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
    result = BASIC_NUMERIC_DEC.replace(result) { m ->
        val code = m.groupValues[1].toIntOrNull() ?: return@replace m.value
        runCatching { String(intArrayOf(code), 0, 1) }.getOrDefault(m.value)
    }
    result = BASIC_NUMERIC_HEX.replace(result) { m ->
        val code = m.groupValues[1].toIntOrNull(16) ?: return@replace m.value
        runCatching { String(intArrayOf(code), 0, 1) }.getOrDefault(m.value)
    }
    return result
}
