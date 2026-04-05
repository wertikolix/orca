package ru.wertik.orca.core

internal class OrcaParserCache(
    maxEntries: Int = DEFAULT_PARSE_CACHE_SIZE,
) {
    private val maxEntries = maxEntries.coerceAtLeast(1)
    private val entries = linkedMapOf<Any, CachedParseEntry>()
    private val lock = OrcaLock()

    /**
     * Returns the cached result for [key] if it matches [input],
     * otherwise invokes [parse] and caches the result.
     *
     * Thread-safe: the lock only guards cache reads and writes.
     * Parsing itself runs **outside** the lock so that concurrent
     * callers on different keys (or the same key with different
     * input) never block each other.
     */
    fun getOrPut(
        key: Any,
        input: String,
        parse: () -> OrcaParseResult,
    ): OrcaParseResult {
        // Fast path: check cache under a short lock.
        val cached = lock.withLock {
            val entry = entries[key]
            if (entry != null && entry.input == input) {
                // Cache hit — move to end (LRU refresh).
                entries.remove(key)
                entries[key] = entry
                entry.result
            } else {
                null
            }
        }
        if (cached != null) return cached

        // Slow path: parse outside the lock to avoid blocking other threads.
        val result = parse()

        val raced = lock.withLock {
            // Re-check: another thread may have raced and cached the same key.
            val existing = entries[key]
            if (existing != null && existing.input == input) {
                existing.result
            } else {
                entries.remove(key)
                entries[key] = CachedParseEntry(input = input, result = result)
                trimToLimit()
                null
            }
        }

        return raced ?: result
    }

    fun clear() {
        lock.withLock {
            entries.clear()
        }
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
