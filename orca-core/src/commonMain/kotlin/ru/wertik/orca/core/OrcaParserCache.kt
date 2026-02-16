package ru.wertik.orca.core

internal class OrcaParserCache(
    maxEntries: Int = DEFAULT_PARSE_CACHE_SIZE,
) {
    private val maxEntries = maxEntries.coerceAtLeast(1)
    private val entries = linkedMapOf<Any, CachedParseEntry>()

    fun getOrPut(
        key: Any,
        input: String,
        parse: () -> OrcaParseResult,
    ): OrcaParseResult {
        val cached = entries[key]
        if (cached != null && cached.input == input) {
            entries.remove(key)
            entries[key] = cached
            return cached.result
        }

        val parsed = parse()
        entries.remove(key)
        entries[key] = CachedParseEntry(
            input = input,
            result = parsed,
        )
        trimToLimit()
        return parsed
    }

    fun clear() {
        entries.clear()
    }

    private fun trimToLimit() {
        while (entries.size > maxEntries) {
            val eldestKey = entries.entries.firstOrNull()?.key ?: return
            entries.remove(eldestKey)
        }
    }
}

private data class CachedParseEntry(
    val input: String,
    val result: OrcaParseResult,
)
