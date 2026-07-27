package ru.wertik.orca.core

/**
 * Append-oriented parser session for streaming Markdown.
 *
 * The session splits the completed part of the stream into segments and freezes a
 * growing prefix of segments whose parse result cannot be affected by content that
 * arrives later. Frozen segments are parsed once and their blocks are reused verbatim;
 * only the active tail is re-parsed on every update. Heading anchor slugs are derived
 * with a running counter so duplicate titles keep full-parse numbering.
 *
 * Segment cut points are blank lines *and* completed top-level structures: a
 * column-zero fenced code block is a segment of its own, so a stream sitting inside a
 * long ``` block still freezes everything before the fence, and the block is frozen the
 * moment its closing line arrives instead of waiting for the next blank line. While
 * such a fence is open the tail is a single code block, which the session builds
 * directly (after checking once, against the delegate, that this matches) instead of
 * re-parsing the whole growing block on every token.
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
    private val stableSlugCounts = mutableMapOf<String, Int>()
    private val segments = SegmentScan()
    private val globalConstructs = GlobalConstructScan()
    private var verifiedFenceHeader: String? = null
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
            resetStreamState()
        }

        val result = parseIncrementallyOrNull(input) ?: parseFull(input)
        lastInput = input
        lastResult = result
        return result
    }

    /** Clears retained stable blocks and counters for a new message/session. */
    fun reset() {
        resetStreamState()
        lastInput = null
        lastResult = null
        stats = OrcaIncrementalParseStats()
    }

    private fun parseIncrementallyOrNull(input: String): OrcaParseResult? {
        if (input.isBlank()) return null
        if (globalConstructs.containsGlobalConstructs(input)) return null

        segments.advance(input)
        val boundary = segments.stableBoundary()

        val nextStableSource = if (boundary > 0) input.substring(0, boundary) else ""
        if (!nextStableSource.startsWith(stableSource)) {
            resetStablePrefix()
        }

        val activeTail = input.substring(nextStableSource.length)
        val openFence = openCodeFenceTail(activeTail)
        // Without a frozen prefix there is nothing to reuse, unless the tail is an open
        // fence the session can rebuild without going through the parser at all.
        if (boundary <= 0 && openFence == null) return null

        val appendedStableSource = nextStableSource.substring(stableSource.length)
        val appendedResult = if (appendedStableSource.isBlank()) {
            EMPTY_RESULT
        } else {
            // The leading newline is semantically neutral at a segment boundary and
            // prevents a chunk beginning with `---`/`+++` from being misread as
            // front matter, which only exists at the very start of a document.
            delegate.parseWithDiagnostics("\n" + appendedStableSource)
        }
        if (appendedResult.diagnostics.hasErrors) return null

        val tailResult = parseTail(activeTail, openFence) ?: return null

        stableBlocks += reslugHeadings(appendedResult.document.blocks, stableSlugCounts)
        stableSource = nextStableSource

        stats = stats.copy(
            incrementalParses = stats.incrementalParses + 1,
            reusedStableBlocks = stats.reusedStableBlocks + stableBlocks.size,
        )
        val tailBlocks = reslugHeadings(tailResult.document.blocks, HashMap(stableSlugCounts))
        return OrcaParseResult(
            document = OrcaDocument(blocks = stableBlocks + tailBlocks),
            diagnostics = mergeDiagnostics(appendedResult.diagnostics, tailResult.diagnostics),
        )
    }

    /**
     * Parses the active tail, taking the open-fence shortcut when it has been proven
     * equivalent for this fence header. Returns `null` when the tail failed to parse.
     */
    private fun parseTail(activeTail: String, openFence: OpenCodeFenceTail?): OrcaParseResult? {
        if (activeTail.isBlank()) return EMPTY_RESULT

        if (openFence != null && openFence.header == verifiedFenceHeader) {
            stats = stats.copy(codeFenceFastPaths = stats.codeFenceFastPaths + 1)
            return OrcaParseResult(document = OrcaDocument(blocks = listOf(openFence.block)))
        }

        val parsed = delegate.parseWithDiagnostics("\n" + activeTail)
        if (parsed.diagnostics.hasErrors) return null
        if (openFence != null) {
            // Check the shortcut against the delegate once per fence header; only a
            // delegate that maps this fence the standard way gets to skip re-parsing.
            verifiedFenceHeader = openFence.header.takeIf { openFence.matches(parsed) }
        }
        return parsed
    }

    private fun parseFull(input: String): OrcaParseResult {
        resetStreamState()
        stats = stats.copy(fullParses = stats.fullParses + 1)
        return delegate.parseWithDiagnostics(input)
    }

    private fun resetStreamState() {
        resetStablePrefix()
        segments.reset()
        globalConstructs.reset()
        verifiedFenceHeader = null
    }

    private fun resetStablePrefix() {
        stableSource = ""
        stableBlocks.clear()
        stableSlugCounts.clear()
    }
}

/** Parse-path counters reported by [OrcaIncrementalParserSession]. */
data class OrcaIncrementalParseStats(
    val fullParses: Int = 0,
    val incrementalParses: Int = 0,
    val reusedStableBlocks: Int = 0,
    /** Updates whose tail was a growing code fence rebuilt without re-parsing it. */
    val codeFenceFastPaths: Int = 0,
)

private data class SessionCacheKey(
    val delegateKey: Any,
    val session: OrcaIncrementalParserSession,
)

private val EMPTY_RESULT = OrcaParseResult(document = OrcaDocument(emptyList()))

private fun mergeDiagnostics(
    first: OrcaParseDiagnostics,
    second: OrcaParseDiagnostics,
): OrcaParseDiagnostics {
    if (first.warnings.isEmpty() && first.errors.isEmpty()) return second
    if (second.warnings.isEmpty() && second.errors.isEmpty()) return first
    return OrcaParseDiagnostics(
        errors = first.errors + second.errors,
        warnings = first.warnings + second.warnings,
    )
}

/**
 * Assigns heading anchor slugs from a running [counts] map so that duplicate titles
 * receive the same `-N` disambiguation suffixes a single full parse would produce.
 * Frozen blocks keep their slugs (and their identity) because the counter carries over
 * to the next chunk. Mirrors the main tree mapper's reach: headings inside details,
 * footnotes, and definition lists are slugged by isolated sub-parsers in the full
 * pipeline and are therefore left untouched here as well.
 */
private fun reslugHeadings(
    blocks: List<OrcaBlock>,
    counts: MutableMap<String, Int>,
): List<OrcaBlock> {
    if (blocks.isEmpty()) return blocks

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

/**
 * Append-only scan for document-global constructs.
 *
 * Both patterns are line anchored or two characters wide, so re-scanning from the
 * start of the previously last line is enough to catch anything the new chunk adds.
 * That keeps the check proportional to the appended text instead of the whole stream.
 */
private class GlobalConstructScan {
    private var found = false
    private var scannedUpTo = 0

    fun containsGlobalConstructs(input: String): Boolean {
        if (found) return true
        if (input.startsWith("---") || input.startsWith("+++")) {
            found = true
            return true
        }
        val from = scannedUpTo.coerceIn(0, input.length)
        if (GLOBAL_DEFINITION_REGEX.find(input, from) != null ||
            INLINE_FOOTNOTE_REGEX.find(input, from) != null
        ) {
            found = true
            return true
        }
        scannedUpTo = input.lastIndexOf('\n') + 1
        return false
    }

    fun reset() {
        found = false
        scannedUpTo = 0
    }
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

/**
 * Append-only segmentation of the stream.
 *
 * Every completed segment ends at a point where the scanner state is "outside a
 * segment, no open fence", so scanning can resume from the last completed segment
 * instead of re-reading the whole stream on every token. Segments are only ever
 * completed on a *terminated* line, which is what makes that resume point final: an
 * unterminated last line can still turn into anything (`para\n` growing into
 * `para\nmore` is one paragraph, not two segments). Classification runs on character
 * ranges: no per-line strings and no regex matching, since it happens once per
 * streamed chunk over the tail.
 */
private class SegmentScan {
    private val completed = mutableListOf<Segment>()
    private val preprocessors = PreprocessorMirror()
    private var resumeOffset = 0

    /** Whether the last scan ended inside a segment (or inside an unclosed fence). */
    private var tailPending = false
    private var tailContinuesPrevious = false

    fun reset() {
        completed.clear()
        preprocessors.reset()
        resumeOffset = 0
        tailPending = false
        tailContinuesPrevious = false
    }

    /**
     * Returns the largest offset such that everything before it parses independently
     * from everything after it, or `0` when no such prefix exists yet. The last
     * segment is never frozen: it may still grow into the content that follows.
     */
    fun stableBoundary(): Int {
        val count = completed.size + if (tailPending) 1 else 0
        if (count < 2) return 0

        var boundary = 0
        for (index in 0 until count - 1) {
            val segment = completed[index]
            if (segment.kind == SegmentKind.UNSAFE) break
            val nextContinues = if (index + 1 < completed.size) {
                completed[index + 1].continuesPrevious
            } else {
                tailContinuesPrevious
            }
            if (segment.kind == SegmentKind.CONTINUABLE && nextContinues) break
            boundary = segment.end
        }
        return boundary
    }

    fun advance(input: String) {
        var offset = resumeOffset.coerceIn(0, input.length)
        var inSegment = false
        var segmentFirstLineComplete = false
        var kind = SegmentKind.SAFE
        var continuesPrevious = false
        var fenceChar = ' '
        var fenceLength = 0
        var fenceIsOwnSegment = false

        fun complete(end: Int, segmentKind: SegmentKind, cutBeforeCurrentLine: Boolean = false) {
            // Cutting the document while one of the delegate's pre-passes is mid-region
            // would hand it a different view of the text than a full parse gets.
            val neutral = if (cutBeforeCurrentLine) {
                preprocessors.wasNeutralBeforeLine
            } else {
                preprocessors.isNeutral
            }
            val effectiveKind = if (neutral) segmentKind else SegmentKind.UNSAFE
            completed += Segment(end = end, kind = effectiveKind, continuesPrevious = continuesPrevious)
            resumeOffset = end
            preprocessors.commit(beforeCurrentLine = cutBeforeCurrentLine)
            inSegment = false
            segmentFirstLineComplete = false
            fenceIsOwnSegment = false
            kind = SegmentKind.SAFE
            continuesPrevious = false
        }

        preprocessors.rewind()

        while (true) {
            val newline = input.indexOf('\n', offset)
            val lineComplete = newline >= 0
            val lineEnd = if (lineComplete) newline else input.length
            val nextOffset = if (lineComplete) newline + 1 else input.length
            preprocessors.consume(input, offset, lineEnd)

            if (fenceLength > 0) {
                if (closesFence(input, offset, lineEnd, fenceChar, fenceLength)) {
                    fenceLength = 0
                    if (fenceIsOwnSegment && lineComplete) {
                        // A closed top-level fence is a finished structure: nothing that
                        // arrives later can merge into it.
                        complete(nextOffset, kind)
                    }
                }
            } else if (isBlank(input, offset, lineEnd)) {
                if (inSegment && lineComplete) complete(nextOffset, kind)
            } else {
                val opensTopLevelFence = lineComplete && isTopLevelFenceOpen(input, offset, lineEnd)
                if (inSegment && opensTopLevelFence && kind != SegmentKind.CONTINUABLE) {
                    // A fence interrupts whatever precedes it, so the text before this
                    // line is already complete. Lists are excluded: a column-zero fence
                    // right after a list item is rare enough to keep on the safe side.
                    complete(offset, kind, cutBeforeCurrentLine = true)
                }

                if (!inSegment) {
                    inSegment = true
                    segmentFirstLineComplete = lineComplete
                    fenceIsOwnSegment = opensTopLevelFence
                    // Any leading whitespace may continue a preceding list item
                    // (CommonMark continuation indent starts at two columns); list
                    // markers may extend a preceding list across the blank line.
                    continuesPrevious = input[offset].isWhitespace() || isListMarker(input, offset, lineEnd)
                    kind = when {
                        // HTML blocks (incl. <details>) and display math can extend
                        // across the blank lines that follow, so the segment may not be
                        // self-contained.
                        startsHtmlBlock(input, offset, lineEnd) || startsMathFence(input, offset, lineEnd)
                            -> SegmentKind.UNSAFE

                        continuesPrevious -> SegmentKind.CONTINUABLE

                        else -> SegmentKind.SAFE
                    }
                } else {
                    if (kind == SegmentKind.SAFE &&
                        (input[offset].isWhitespace() || isListMarker(input, offset, lineEnd))
                    ) {
                        // A list (or indented block) that starts part way into the segment
                        // can still absorb the content after the next blank line, exactly
                        // like one that starts on the first line.
                        kind = SegmentKind.CONTINUABLE
                    }
                    if (kind != SegmentKind.UNSAFE &&
                        (startsHtmlBlock(input, offset, lineEnd) || startsMathFence(input, offset, lineEnd))
                    ) {
                        kind = SegmentKind.UNSAFE
                    }
                }

                val opened = fenceOpenRunLength(input, offset, lineEnd)
                if (opened > 0) {
                    fenceChar = input[offset + leadingSpaces(input, offset, lineEnd)]
                    fenceLength = opened
                }
            }

            if (!lineComplete) break
            offset = nextOffset
        }

        // A trailing open segment (or an unclosed fence) is the active tail. Its first
        // line may still be growing, so it must also conservatively count as a potential
        // continuation of a preceding list until the line is complete.
        tailPending = inSegment || fenceLength > 0
        tailContinuesPrevious = continuesPrevious || !segmentFirstLineComplete
    }
}

private const val MIN_FENCE_LENGTH = 3
private const val MAX_BLOCK_INDENT = 3
private const val NO_FENCE = ' '

/**
 * Mirror of the delegate's line-based pre-passes that can span segments.
 *
 * `<details>` extraction and `$$` math extraction run over the raw source before the
 * markdown parser sees it, and neither shares this scanner's view of fenced code:
 * details extraction ignores fences entirely, math extraction tracks them by trimmed
 * prefix so ```` ``` ````-with-a-backtick-info-string or a four-tick fence desynchronises it.
 * Cutting the stream while one of those passes is mid-region would give the delegate a
 * different document than a full parse, so a cut is only offered where this mirror is
 * neutral too.
 */
private class PreprocessorMirror {
    private var fence = NO_FENCE
    private var detailsDepth = 0
    private var mathOpen = false

    private var previousFence = NO_FENCE
    private var previousDetailsDepth = 0
    private var previousMathOpen = false

    private var committedFence = NO_FENCE
    private var committedDetailsDepth = 0
    private var committedMathOpen = false

    /** State after the line the scanner is on. */
    val isNeutral: Boolean
        get() = fence == NO_FENCE && detailsDepth == 0 && !mathOpen

    /** State before the line the scanner is on, for cuts placed ahead of it. */
    val wasNeutralBeforeLine: Boolean
        get() = previousFence == NO_FENCE && previousDetailsDepth == 0 && !previousMathOpen

    fun reset() {
        fence = NO_FENCE
        detailsDepth = 0
        mathOpen = false
        previousFence = NO_FENCE
        previousDetailsDepth = 0
        previousMathOpen = false
        committedFence = NO_FENCE
        committedDetailsDepth = 0
        committedMathOpen = false
    }

    /** Restores the state captured at the cut the scanner resumes from. */
    fun rewind() {
        fence = committedFence
        detailsDepth = committedDetailsDepth
        mathOpen = committedMathOpen
        previousFence = committedFence
        previousDetailsDepth = committedDetailsDepth
        previousMathOpen = committedMathOpen
    }

    /**
     * Captures the state at a completed segment so the next scan can resume there.
     * A cut placed before the current line has to remember the state from before it,
     * since the resumed scan reads that line again.
     */
    fun commit(beforeCurrentLine: Boolean) {
        committedFence = if (beforeCurrentLine) previousFence else fence
        committedDetailsDepth = if (beforeCurrentLine) previousDetailsDepth else detailsDepth
        committedMathOpen = if (beforeCurrentLine) previousMathOpen else mathOpen
    }

    fun consume(input: String, from: Int, to: Int) {
        previousFence = fence
        previousDetailsDepth = detailsDepth
        previousMathOpen = mathOpen

        var start = from
        while (start < to && input[start].isWhitespace()) start += 1
        var end = to
        while (end > start && input[end - 1].isWhitespace()) end -= 1
        if (start >= end) return

        // Details extraction runs first and is not fence aware.
        if (input[start] == '<') {
            val line = input.substring(from, to)
            if (lineOpensDetails(line)) {
                detailsDepth += 1
            } else if (lineClosesDetails(line)) {
                detailsDepth = maxOf(0, detailsDepth - 1)
            }
        }

        val marker = input[start]
        if ((marker == '`' || marker == '~') && end - start >= MIN_FENCE_LENGTH &&
            input[start + 1] == marker && input[start + 2] == marker
        ) {
            fence = when (fence) {
                NO_FENCE -> marker
                marker -> NO_FENCE
                else -> fence
            }
            return
        }
        if (fence != NO_FENCE) return

        if (end - start == 2 && marker == '$' && input[start + 1] == '$') {
            mathOpen = !mathOpen
        }
    }
}

private fun isBlank(input: String, from: Int, to: Int): Boolean {
    for (index in from until to) {
        if (!input[index].isWhitespace()) return false
    }
    return true
}

/** Number of leading spaces, capped so callers can reject over-indented lines. */
private fun leadingSpaces(input: String, from: Int, to: Int): Int {
    var index = from
    while (index < to && index - from <= MAX_BLOCK_INDENT && input[index] == ' ') index += 1
    return index - from
}

/**
 * Length of a fence *opener* run after at most three spaces of indent, or `0`.
 *
 * A backtick fence may not carry backticks in its info string (```` ``` `x` ```` is a
 * paragraph, not a code block); tilde fences may. Getting this wrong inverts the
 * scanner's fence state for the rest of the document.
 */
private fun fenceOpenRunLength(input: String, from: Int, to: Int): Int {
    val indent = leadingSpaces(input, from, to)
    if (indent > MAX_BLOCK_INDENT) return 0
    val start = from + indent
    if (start >= to) return 0
    val marker = input[start]
    if (marker != '`' && marker != '~') return 0
    var index = start
    while (index < to && input[index] == marker) index += 1
    if (index - start < MIN_FENCE_LENGTH) return 0
    if (marker == '`') {
        for (rest in index until to) {
            if (input[rest] == '`') return 0
        }
    }
    return index - start
}

/**
 * Fence opener at column zero. Such a line always starts a new top-level block: it
 * interrupts paragraphs and is too far left to continue a list item, so the text
 * before it is complete even without a blank line.
 */
private fun isTopLevelFenceOpen(input: String, from: Int, to: Int): Boolean {
    return from < to && input[from] != ' ' && fenceOpenRunLength(input, from, to) > 0
}

/** Whether the line closes a fence opened with [openChar] repeated [openLength] times. */
private fun closesFence(input: String, from: Int, to: Int, openChar: Char, openLength: Int): Boolean {
    val indent = leadingSpaces(input, from, to)
    if (indent > MAX_BLOCK_INDENT) return false
    var index = from + indent
    if (index >= to || input[index] != openChar) return false
    val runStart = index
    while (index < to && input[index] == openChar) index += 1
    if (index - runStart < openLength) return false
    // `line.trim()` has to be exactly the delimiter run: no info string on a closer.
    for (rest in index until to) {
        if (!input[rest].isWhitespace()) return false
    }
    return true
}

/** `^ {0,3}(?:[-+*]|\d{1,9}[.)])(?:\s|$)` on a character range. */
private fun isListMarker(input: String, from: Int, to: Int): Boolean {
    val indent = leadingSpaces(input, from, to)
    if (indent > MAX_BLOCK_INDENT) return false
    var index = from + indent
    if (index >= to) return false
    val marker = input[index]
    if (marker == '-' || marker == '+' || marker == '*') {
        index += 1
    } else {
        var digits = 0
        while (index < to && digits < 10 && input[index].isDigit()) {
            index += 1
            digits += 1
        }
        if (digits == 0 || digits > 9) return false
        if (index >= to) return false
        val delimiter = input[index]
        if (delimiter != '.' && delimiter != ')') return false
        index += 1
    }
    return index >= to || input[index].isWhitespace()
}

/** `^ {0,3}<` on a character range. */
private fun startsHtmlBlock(input: String, from: Int, to: Int): Boolean {
    val indent = leadingSpaces(input, from, to)
    if (indent > MAX_BLOCK_INDENT) return false
    val index = from + indent
    return index < to && input[index] == '<'
}

/** `^ {0,3}\$\$` on a character range. */
private fun startsMathFence(input: String, from: Int, to: Int): Boolean {
    val indent = leadingSpaces(input, from, to)
    if (indent > MAX_BLOCK_INDENT) return false
    val index = from + indent
    return index + 1 < to && input[index] == '$' && input[index + 1] == '$'
}

/**
 * Active tail that is a single column-zero fenced code block still waiting for its
 * closing fence.
 *
 * While the block grows, re-parsing it on every token is the dominant cost of a
 * streamed LLM answer, and the result is always the same shape: one code block whose
 * body is the raw text after the info line. [matches] is used to confirm that against
 * the delegate once per fence header before the shortcut is taken.
 */
private class OpenCodeFenceTail(
    val header: String,
    val block: OrcaBlock.CodeBlock,
) {
    fun matches(result: OrcaParseResult): Boolean {
        return !result.diagnostics.hasErrors &&
            !result.diagnostics.hasWarnings &&
            result.document.frontMatter == null &&
            result.document.blocks.size == 1 &&
            result.document.blocks[0] == block
    }
}

/**
 * Whether a line would be picked up by one of the delegate's raw-source pre-passes
 * ([extractMathBlocks], [extractDetailsBlocks]) even when it sits inside a code fence.
 */
private fun isPreprocessorSensitive(input: String, from: Int, to: Int): Boolean {
    var start = from
    while (start < to && input[start].isWhitespace()) start += 1
    var end = to
    while (end > start && input[end - 1].isWhitespace()) end -= 1
    if (start >= end) return false

    val marker = input[start]
    if ((marker == '`' || marker == '~') && end - start >= MIN_FENCE_LENGTH &&
        input[start + 1] == marker && input[start + 2] == marker
    ) {
        return true
    }
    if (marker == '$' && end - start >= 2 && input[start + 1] == '$') return true
    if (marker == '<') {
        val line = input.substring(from, to)
        return lineOpensDetails(line) || lineClosesDetails(line)
    }
    return false
}

private fun openCodeFenceTail(tail: String): OpenCodeFenceTail? {
    if (tail.length < MIN_FENCE_LENGTH) return null
    val marker = tail[0]
    if (marker != '`' && marker != '~') return null

    var runLength = 0
    while (runLength < tail.length && tail[runLength] == marker) runLength += 1
    if (runLength < MIN_FENCE_LENGTH) return null

    val headerEnd = tail.indexOf('\n')
    // The info line is still being streamed: its content decides the language.
    if (headerEnd < 0) return null
    if (marker == '`') {
        for (index in runLength until headerEnd) {
            if (tail[index] == '`') return null
        }
    }
    // Carriage returns change how the delegate splits fence content; not worth guessing.
    if (tail.indexOf('\r') >= 0) return null

    var lineStart = headerEnd + 1
    while (lineStart <= tail.length) {
        val newline = tail.indexOf('\n', lineStart)
        val lineEnd = if (newline < 0) tail.length else newline
        if (closesFence(tail, lineStart, lineEnd, marker, runLength)) return null
        // The delegate's pre-passes do not see this fence the way this scanner does, so
        // code that looks like a fence, display math or <details> goes the slow way.
        if (isPreprocessorSensitive(tail, lineStart, lineEnd)) return null
        if (newline < 0) break
        lineStart = newline + 1
    }

    val info = tail.substring(runLength, headerEnd).trim()
    return OpenCodeFenceTail(
        header = tail.substring(0, headerEnd),
        block = OrcaBlock.CodeBlock(
            code = tail.substring(headerEnd + 1).trimEnd('\n'),
            language = info.split(' ').firstOrNull()?.takeIf { token -> token.isNotEmpty() },
        ),
    )
}
