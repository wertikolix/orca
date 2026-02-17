package ru.wertik.orca.core

fun interface OrcaParser {
    fun parse(input: String): OrcaDocument

    fun parseWithDiagnostics(input: String): OrcaParseResult {
        return OrcaParseResult(
            document = parse(input),
        )
    }

    /**
     * Parse with caching by [key]. The default implementation does NOT cache
     * and delegates to [parseWithDiagnostics]. Override to provide actual caching.
     * [OrcaMarkdownParser] provides a full LRU cache implementation.
     */
    fun parseCached(
        key: Any,
        input: String,
    ): OrcaDocument {
        return parseCachedWithDiagnostics(
            key = key,
            input = input,
        ).document
    }

    /**
     * Parse with diagnostics and caching by [key]. The default implementation
     * does NOT cache and delegates to [parseWithDiagnostics]. Override to provide
     * actual caching. [OrcaMarkdownParser] provides a full LRU cache implementation.
     */
    fun parseCachedWithDiagnostics(
        key: Any,
        input: String,
    ): OrcaParseResult {
        return parseWithDiagnostics(input)
    }

    fun cacheKey(): Any = this::class
}
