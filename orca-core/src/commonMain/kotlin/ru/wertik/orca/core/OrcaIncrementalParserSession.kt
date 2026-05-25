package ru.wertik.orca.core

/**
 * Append-oriented parser session for streaming Markdown.
 *
 * The first implementation deliberately applies its incremental fast path only to streams whose
 * completed prefix consists of independent paragraphs. Markdown constructs that can change the
 * meaning of earlier blocks (lists, headings, reference definitions, footnotes, tables, fences,
 * HTML blocks, or front matter) fall back to the delegate parser for correctness.
 *
 * This makes ordinary prose streams cheaper while preserving exact delegate semantics for rich
 * Markdown until more specialised incremental strategies are added.
 */
class OrcaIncrementalParserSession(
    private val delegate: OrcaParser,
) : OrcaParser {
    private var stableSource: String = ""
    private val stableBlocks = mutableListOf<OrcaBlock>()
    private var lastInput: String? = null
    private var lastResult: OrcaParseResult? = null

    /** Runtime counters useful when benchmarking streaming rendering. */
    var stats: OrcaIncrementalParseStats = OrcaIncrementalParseStats()
        private set

    override fun cacheKey(): Any = SessionCacheKey(delegate.cacheKey(), this)

    override fun parse(input: String): OrcaDocument = parseWithDiagnostics(input).document

    override fun parseCachedWithDiagnostics(key: Any, input: String): OrcaParseResult {
        return parseWithDiagnostics(input)
    }

    override fun parseWithDiagnostics(input: String): OrcaParseResult {
        if (input == lastInput) return lastResult ?: parseFull(input)
        if (lastInput != null && !input.startsWith(lastInput.orEmpty())) {
            resetStablePrefix()
        }

        val result = parseIncrementallyOrNull(input) ?: parseFull(input)
        lastInput = input
        lastResult = result
        return result
    }

    /** Clears retained stable blocks and counters for a new message/session. */
    fun reset() {
        stableSource = ""
        stableBlocks.clear()
        lastInput = null
        lastResult = null
        stats = OrcaIncrementalParseStats()
    }

    private fun parseIncrementallyOrNull(input: String): OrcaParseResult? {
        if (!isIndependentParagraphStream(input)) return null
        val stableBoundary = input.lastIndexOf("\n\n")
        if (stableBoundary < 0) return null

        val nextStableSource = input.substring(0, stableBoundary + 2)
        if (!nextStableSource.startsWith(stableSource)) {
            resetStablePrefix()
        }

        val appendedStableSource = nextStableSource.substring(stableSource.length)
        val appendedResult = if (appendedStableSource.isEmpty()) {
            OrcaParseResult(document = OrcaDocument(emptyList()))
        } else {
            delegate.parseWithDiagnostics(appendedStableSource)
        }
        if (appendedResult.diagnostics.hasErrors) return null

        stableBlocks += appendedResult.document.blocks
        stableSource = nextStableSource

        val activeTail = input.substring(stableSource.length)
        val tailResult = if (activeTail.isEmpty()) {
            OrcaParseResult(document = OrcaDocument(emptyList()))
        } else {
            delegate.parseWithDiagnostics(activeTail)
        }
        if (tailResult.diagnostics.hasErrors) return null

        stats = stats.copy(
            incrementalParses = stats.incrementalParses + 1,
            reusedStableBlocks = stats.reusedStableBlocks + stableBlocks.size,
        )
        return OrcaParseResult(
            document = OrcaDocument(blocks = stableBlocks.toList() + tailResult.document.blocks),
            diagnostics = mergeDiagnostics(appendedResult.diagnostics, tailResult.diagnostics),
        )
    }

    private fun parseFull(input: String): OrcaParseResult {
        resetStablePrefix()
        stats = stats.copy(fullParses = stats.fullParses + 1)
        return delegate.parseWithDiagnostics(input)
    }

    private fun resetStablePrefix() {
        stableSource = ""
        stableBlocks.clear()
    }
}

/** Parse-path counters reported by [OrcaIncrementalParserSession]. */
data class OrcaIncrementalParseStats(
    val fullParses: Int = 0,
    val incrementalParses: Int = 0,
    val reusedStableBlocks: Int = 0,
)

private data class SessionCacheKey(
    val delegateKey: Any,
    val session: OrcaIncrementalParserSession,
)

private fun mergeDiagnostics(
    first: OrcaParseDiagnostics,
    second: OrcaParseDiagnostics,
): OrcaParseDiagnostics {
    return OrcaParseDiagnostics(
        errors = first.errors + second.errors,
        warnings = first.warnings + second.warnings,
    )
}

private fun isIndependentParagraphStream(input: String): Boolean {
    if (input.isBlank()) return true
    val unsafeLinePrefix = Regex("""(?m)^(?: {4}\S| {0,3}(?:#{1,6}(?:\s|$)|>|[-+*](?:\s|$)|\d+[.)](?:\s|$)|```|~~~|<|---(?:\s*$)|\+\+\+(?:\s*$)))""")
    val globalDefinition = Regex("""(?m)^ {0,3}(?:\[\^?[^]]+]:|\*\[[^]]+]:)""")
    val inlineFootnote = Regex("""\^\[""")
    val setextHeading = Regex("""(?m)^ {0,3}(?:=+|-+)\s*$""")
    val thematicBreak = Regex("""(?m)^ {0,3}(?:\*{3,}|_{3,}|-{3,})\s*$""")
    return !unsafeLinePrefix.containsMatchIn(input) &&
        !globalDefinition.containsMatchIn(input) &&
        !inlineFootnote.containsMatchIn(input) &&
        !setextHeading.containsMatchIn(input) &&
        !thematicBreak.containsMatchIn(input) &&
        '|' !in input
}
