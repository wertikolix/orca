package ru.wertik.orca.core

fun interface OrcaParser {
    fun parse(input: String): OrcaDocument

    fun parseWithDiagnostics(input: String): OrcaParseResult {
        return OrcaParseResult(
            document = parse(input),
        )
    }

    fun parseCached(
        key: Any,
        input: String,
    ): OrcaDocument {
        return parseCachedWithDiagnostics(
            key = key,
            input = input,
        ).document
    }

    fun parseCachedWithDiagnostics(
        key: Any,
        input: String,
    ): OrcaParseResult {
        return parseWithDiagnostics(input)
    }

    fun cacheKey(): Any = this::class
}
