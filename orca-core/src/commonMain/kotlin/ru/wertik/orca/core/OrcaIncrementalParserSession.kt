package ru.wertik.orca.core

/**
 * Append-oriented parser session for streaming Markdown.
 *
 * The session splits the completed part of the stream into blank-line separated
 * segments (treating fenced code blocks as atomic) and freezes a growing prefix of
 * segments whose parse result cannot be affected by content that arrives later.
 * Frozen segments are parsed once and their blocks are reused verbatim; only the
 * active tail is re-parsed on every update. Heading anchor slugs are re-derived
 * across the merged document so duplicate titles keep full-parse numbering.
 *
 * Constructs with document-global effects (link reference definitions, footnote
 * definitions, abbreviation definitions, inline footnotes, front matter, and
 * definition lists) disable the fast path entirely so the delegate parser keeps
 * exact semantics. Segments that may interact with later content across blank
 * lines (HTML blocks, `<details>`, display math, list/indented continuations)
 * conservatively stop the frozen prefix from advancing past them.
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
        if (input.isBlank()) return null
        if (containsGlobalConstructs(input)) return null

        val boundary = stableBoundary(input)
        if (boundary <= 0) return null

        val nextStableSource = input.substring(0, boundary)
        if (!nextStableSource.startsWith(stableSource)) {
            resetStablePrefix()
        }

        val appendedStableSource = nextStableSource.substring(stableSource.length)
        val appendedResult = if (appendedStableSource.isBlank()) {
            OrcaParseResult(document = OrcaDocument(emptyList()))
        } else {
            // The leading newline is semantically neutral at a segment boundary and
            // prevents a chunk beginning with `---`/`+++` from being misread as
            // front matter, which only exists at the very start of a document.
            delegate.parseWithDiagnostics("\n" + appendedStableSource)
        }
        if (appendedResult.diagnostics.hasErrors) return null

        stableBlocks += appendedResult.document.blocks
        stableSource = nextStableSource

        val activeTail = input.substring(stableSource.length)
        val tailResult = if (activeTail.isBlank()) {
            OrcaParseResult(document = OrcaDocument(emptyList()))
        } else {
            delegate.parseWithDiagnostics("\n" + activeTail)
        }
        if (tailResult.diagnostics.hasErrors) return null

        stats = stats.copy(
            incrementalParses = stats.incrementalParses + 1,
            reusedStableBlocks = stats.reusedStableBlocks + stableBlocks.size,
        )
        return OrcaParseResult(
            document = OrcaDocument(blocks = reslugHeadings(stableBlocks + tailResult.document.blocks)),
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

/**
 * Recomputes heading anchor slugs across the merged document so that duplicate
 * titles receive the same `-N` disambiguation suffixes a single full parse would
 * produce. Mirrors the main tree mapper's reach: headings inside details,
 * footnotes, and definition lists are slugged by isolated sub-parsers in the full
 * pipeline and are therefore left untouched here as well.
 */
private fun reslugHeadings(blocks: List<OrcaBlock>): List<OrcaBlock> {
    val counts = mutableMapOf<String, Int>()

    fun slug(content: List<OrcaInline>): String {
        val base = orcaHeadingSlugBase(content)
        if (base.isEmpty()) return ""
        val count = counts[base] ?: 0
        counts[base] = count + 1
        return if (count == 0) base else "$base-$count"
    }

    fun remap(block: OrcaBlock): OrcaBlock = when (block) {
        is OrcaBlock.Heading -> if (block.id == null) block else block.copy(id = slug(block.content))
        is OrcaBlock.Quote -> block.copy(blocks = block.blocks.map(::remap))
        is OrcaBlock.Admonition -> block.copy(blocks = block.blocks.map(::remap))
        is OrcaBlock.ListBlock -> block.copy(
            items = block.items.map { item -> item.copy(blocks = item.blocks.map(::remap)) },
        )
        else -> block
    }

    return blocks.map(::remap)
}

// Constructs whose meaning is resolved against surrounding content that may live in
// other segments (or the whole document). Their presence anywhere in the stream
// disables incremental reuse: link/footnote/abbreviation definitions, inline
// footnotes, front matter, and definition lists (a `: definition` line binds to a
// term line that may be separated from it by a blank line).
private val GLOBAL_DEFINITION_REGEX = Regex("""(?m)^ {0,3}(?:\[\^?[^\]\n]+]:|\*\[[^\]\n]+]:|:\s)""")
private val INLINE_FOOTNOTE_REGEX = Regex("""\^\[""")

private fun containsGlobalConstructs(input: String): Boolean {
    if (input.startsWith("---") || input.startsWith("+++")) return true
    return GLOBAL_DEFINITION_REGEX.containsMatchIn(input) ||
        INLINE_FOOTNOTE_REGEX.containsMatchIn(input)
}

private enum class SegmentKind {
    /** Self-contained once followed by a blank line. */
    SAFE,

    /** Safe only when the next segment cannot continue it (lists, indented code). */
    CONTINUABLE,

    /** Stops the frozen prefix from advancing past it. */
    UNSAFE,
}

private class Segment(
    /** Exclusive end offset including the trailing blank-line separator. */
    val end: Int,
    val kind: SegmentKind,
    /** Whether this segment could extend the previous [SegmentKind.CONTINUABLE] segment. */
    val continuesPrevious: Boolean,
)

private val FENCE_OPEN_REGEX = Regex("""^ {0,3}(`{3,}|~{3,})""")
private val LIST_MARKER_REGEX = Regex("""^ {0,3}(?:[-+*]|\d{1,9}[.)])(?:\s|$)""")
private val HTML_BLOCK_START_REGEX = Regex("""^ {0,3}<""")
private val MATH_FENCE_REGEX = Regex("""^ {0,3}\$\$""")

/**
 * Returns the largest offset such that everything before it parses independently
 * from everything after it, or `0` when no such prefix exists yet.
 */
private fun stableBoundary(input: String): Int {
    val segments = splitSegments(input)
    if (segments.size < 2) return 0

    var boundary = 0
    for (index in 0 until segments.size - 1) {
        val segment = segments[index]
        if (segment.kind == SegmentKind.UNSAFE) break
        val next = segments[index + 1]
        if (segment.kind == SegmentKind.CONTINUABLE && next.continuesPrevious) break
        boundary = segment.end
    }
    return boundary
}

/** Splits [input] into blank-line separated segments, keeping fenced code atomic. */
private fun splitSegments(input: String): List<Segment> {
    val segments = mutableListOf<Segment>()
    val lines = input.lines()

    var offset = 0
    var segmentStartLine = -1
    var kind = SegmentKind.SAFE
    var continuesPrevious = false
    var fenceDelimiter: String? = null
    var fenceClosed = true

    fun classifyFirstLine(line: String) {
        // Any leading whitespace may continue a preceding list item (CommonMark
        // continuation indent starts at two columns); list markers may extend a
        // preceding list across the blank line.
        continuesPrevious = line.first().isWhitespace() || LIST_MARKER_REGEX.containsMatchIn(line)
        kind = when {
            // HTML blocks (incl. <details>) and display math can extend across the
            // blank lines that follow, so the segment may not be self-contained.
            HTML_BLOCK_START_REGEX.containsMatchIn(line) || MATH_FENCE_REGEX.containsMatchIn(line)
                -> SegmentKind.UNSAFE

            continuesPrevious -> SegmentKind.CONTINUABLE

            else -> SegmentKind.SAFE
        }
    }

    fun escalate(line: String) {
        if (kind == SegmentKind.UNSAFE) return
        // HTML blocks and display math opened mid-segment can span the following
        // blank lines, so the segment may not be self-contained.
        if (HTML_BLOCK_START_REGEX.containsMatchIn(line) || MATH_FENCE_REGEX.containsMatchIn(line)) {
            kind = SegmentKind.UNSAFE
        }
    }

    lines.forEachIndexed { lineIndex, line ->
        val lineEnd = offset + line.length + if (lineIndex < lines.lastIndex) 1 else 0
        val inSegment = segmentStartLine >= 0

        val openFence = fenceDelimiter
        if (openFence != null) {
            val closing = FENCE_OPEN_REGEX.find(line)
            if (closing != null &&
                closing.groupValues[1].first() == openFence.first() &&
                closing.groupValues[1].length >= openFence.length &&
                line.trim() == closing.groupValues[1]
            ) {
                fenceDelimiter = null
                fenceClosed = true
            }
            offset = lineEnd
            return@forEachIndexed
        }

        if (line.isBlank()) {
            if (inSegment) {
                segments += Segment(end = lineEnd, kind = kind, continuesPrevious = continuesPrevious)
                segmentStartLine = -1
            }
            offset = lineEnd
            return@forEachIndexed
        }

        if (!inSegment) {
            segmentStartLine = lineIndex
            classifyFirstLine(line)
        } else {
            escalate(line)
        }

        val fence = FENCE_OPEN_REGEX.find(line)
        if (fence != null) {
            fenceDelimiter = fence.groupValues[1]
            fenceClosed = false
        }

        offset = lineEnd
    }

    if (segmentStartLine >= 0 || !fenceClosed) {
        // A trailing open segment (or an unclosed fence) is the active tail. Its
        // first line may still be growing, so it must also conservatively count as
        // a potential continuation of a preceding list until the line is complete.
        val firstLineComplete = segmentStartLine in 0 until lines.lastIndex
        segments += Segment(
            end = input.length,
            kind = SegmentKind.UNSAFE,
            continuesPrevious = continuesPrevious || !firstLineComplete,
        )
    } else if (segments.isNotEmpty()) {
        // The final blank-line separated segment may still receive content that
        // merges into it (e.g. a list gaining new items); treat it as the tail.
        val last = segments.removeAt(segments.lastIndex)
        segments += Segment(end = last.end, kind = SegmentKind.UNSAFE, continuesPrevious = last.continuesPrevious)
    }

    return segments
}
