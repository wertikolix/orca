package ru.wertik.orca.benchmarks

import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser
import ru.wertik.orca.core.OrcaDocument
import ru.wertik.orca.core.OrcaIncrementalParserSession
import ru.wertik.orca.core.OrcaMarkdownParser
import ru.wertik.orca.core.OrcaParseResult
import ru.wertik.orca.core.OrcaParser

/** Result of a full benchmark run. */
data class BenchmarkReport(
    val measurements: List<Measurement>,
    val checks: List<Check>,
) {
    val failed: List<Check> get() = checks.filterNot { check -> check.passed }
}

/** Input growth factor used by every scaling case. */
private const val SIZE_FACTOR = 4

/** The parser Orca builds on, measured on its own as the baseline for overhead. */
private val upstreamParser = MarkdownParser(GFMFlavourDescriptor())

/**
 * How much more a byte of input may cost when the document grows [SIZE_FACTOR]x.
 *
 * Linear work lands at ~1.0 (the healthy cases measure 0.75-1.05). Work that is
 * quadratic in the document lands at the byte growth factor, so anything above this
 * limit means an accidental rescan crept back into a pre-pass, the mapper, or the
 * inline scanner.
 */
private const val LINEAR_RATIO_LIMIT = 1.75

fun runBenchmarks(quick: Boolean): BenchmarkReport {
    val harness = Harness(quick = quick)
    val measurements = mutableListOf<Measurement>()
    val checks = mutableListOf<Check>()

    fun parseCase(name: String, document: String): Measurement {
        // A fresh parser per call keeps the LRU cache out of the measurement.
        val parser = OrcaMarkdownParser(cacheSize = 1)
        return harness.measure(name = name, bytesPerOp = document.length) { parser.parse(document) }
            .also(measurements::add)
    }

    fun scalingFamily(
        name: String,
        smallSize: Int,
        document: (Int) -> String,
        ratioLimit: Double = LINEAR_RATIO_LIMIT,
    ) {
        val small = parseCase("$name/1x", document(smallSize))
        val large = parseCase("$name/${SIZE_FACTOR}x", document(smallSize * SIZE_FACTOR))
        checks += scalingCheck(
            name = "$name scales linearly",
            small = small,
            large = large,
            maxRatio = ratioLimit,
        )
    }

    scalingFamily("parse/realistic", smallSize = 60, document = Corpus::realisticDocument)
    scalingFamily("parse/single-paragraph", smallSize = 1_000, document = Corpus::singleParagraph)
    scalingFamily("parse/definition-lists", smallSize = 250, document = Corpus::definitionLists)
    scalingFamily("parse/tables", smallSize = 500, document = Corpus::tables)
    scalingFamily("parse/code-blocks", smallSize = 500, document = Corpus::codeBlocks)
    scalingFamily("guard/bracket-bomb", smallSize = 6_400, document = Corpus::bracketBomb)
    scalingFamily("guard/balanced-brackets", smallSize = 6_400, document = Corpus::balancedBrackets)
    scalingFamily("guard/nested-quotes", smallSize = 6_400, document = Corpus::nestedQuotes)
    // Nesting levels grow the document quadratically, which the per-byte check handles.
    scalingFamily("guard/nested-lists", smallSize = 512, document = Corpus::nestedLists)

    // The guard must keep a pathological block near the cost of ordinary text.
    val bomb = measurements.first { measurement -> measurement.name == "guard/bracket-bomb/4x" }
    val balanced = measurements.first { measurement -> measurement.name == "guard/balanced-brackets/4x" }
    checks += ratioCheck(
        name = "guard/bracket-bomb stays close to balanced input",
        detail = "unmatched openers cost x${format(bomb.bestNanos.toDouble() / balanced.bestNanos)} of " +
            "balanced ones (limit x8.0, ${format(bomb.bestMillis)}ms vs ${format(balanced.bestMillis)}ms)",
        value = bomb.bestNanos.toDouble() / balanced.bestNanos,
        limit = 8.0,
    )

    // Overhead of everything Orca does on top of building the upstream AST. Kept as a
    // ratio so it tracks the mapper and the pre-passes, not the machine.
    val document = Corpus.realisticDocument(240)
    val upstream = harness.measure(name = "parse/upstream-tree-only", bytesPerOp = document.length) {
        upstreamParser.buildMarkdownTreeFromString(document)
    }
    measurements += upstream
    val orca = parseCase("parse/orca-full", document)
    checks += ratioCheck(
        name = "parse/orca-full stays close to the upstream parser",
        detail = "x${format(orca.bestNanos.toDouble() / upstream.bestNanos)} of the upstream tree build " +
            "(limit x3.0, ${format(upstream.bestMillis)}ms -> ${format(orca.bestMillis)}ms)",
        value = orca.bestNanos.toDouble() / upstream.bestNanos,
        limit = 3.0,
    )

    checks += streamingBenchmarks(harness, measurements)

    return BenchmarkReport(measurements = measurements, checks = checks)
}

/**
 * Streams an assistant answer chunk by chunk and compares the incremental session with
 * re-parsing the whole prefix on every update, which is what a renderer without a
 * session does.
 */
private fun streamingBenchmarks(
    harness: Harness,
    measurements: MutableList<Measurement>,
): List<Check> {
    val document = Corpus.assistantAnswer()
    val chunks = document.chunked(16)
    var sessionNotes = emptyMap<String, String>()
    var delegateChars = 0L
    var fullChars = 0L

    val sessionMeasurement = harness.measure(
        name = "stream/session",
        bytesPerOp = document.length,
        notes = { sessionNotes },
    ) {
        val counting = CountingParser()
        val session = OrcaIncrementalParserSession(counting)
        var streamed = ""
        var last: OrcaDocument? = null
        chunks.forEach { chunk ->
            streamed += chunk
            last = session.parse(streamed)
        }
        delegateChars = counting.chars
        val stats = session.stats
        sessionNotes = mapOf(
            "updates" to chunks.size.toString(),
            "delegate chars" to counting.chars.toString(),
            "full parses" to stats.fullParses.toString(),
            "fence fast paths" to stats.codeFenceFastPaths.toString(),
        )
        last
    }
    measurements += sessionMeasurement

    val fullMeasurement = harness.measure(
        name = "stream/full-reparse",
        bytesPerOp = document.length,
        notes = { mapOf("updates" to chunks.size.toString(), "delegate chars" to fullChars.toString()) },
    ) {
        val counting = CountingParser()
        var streamed = ""
        var last: OrcaDocument? = null
        chunks.forEach { chunk ->
            streamed += chunk
            last = counting.parse(streamed)
        }
        fullChars = counting.chars
        last
    }
    measurements += fullMeasurement

    val speedup = fullMeasurement.bestNanos.toDouble() / sessionMeasurement.bestNanos
    val workRatio = delegateChars.toDouble() / fullChars.coerceAtLeast(1)
    return listOf(
        ratioCheck(
            name = "stream/session beats full re-parsing",
            detail = "x${format(speedup)} faster over ${chunks.size} updates (minimum x3.0, " +
                "${format(sessionMeasurement.bestMillis)}ms vs ${format(fullMeasurement.bestMillis)}ms)",
            value = speedup,
            limit = 3.0,
            lowerIsBetter = false,
        ),
        ratioCheck(
            name = "stream/session re-parses little text",
            detail = "${format(workRatio * 100)}% of the characters a full re-parse feeds the parser " +
                "(limit 15.0%, $delegateChars vs $fullChars)",
            value = workRatio,
            limit = 0.15,
        ),
    )
}

/** Counts everything handed to the real parser so work, not just time, is comparable. */
private class CountingParser(
    private val delegate: OrcaParser = OrcaMarkdownParser(cacheSize = 1),
) : OrcaParser {
    var chars = 0L
        private set

    override fun parse(input: String): OrcaDocument = parseWithDiagnostics(input).document

    override fun parseWithDiagnostics(input: String): OrcaParseResult {
        chars += input.length
        return delegate.parseWithDiagnostics(input)
    }
}
