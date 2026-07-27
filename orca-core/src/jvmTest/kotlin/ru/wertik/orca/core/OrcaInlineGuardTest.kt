package ru.wertik.orca.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test as JUnitTest

class OrcaInlineGuardTest {
    private val parser = OrcaMarkdownParser(cacheSize = 1)

    @Test
    fun ordinaryDocumentsAreNotGuarded() {
        val document = buildString {
            repeat(200) { index ->
                append("Paragraph $index with a [link](https://example.com/$index), ")
                append("![image](https://example.com/$index.png) and `code[0]`.\n\n")
            }
        }
        val result = parser.parseWithDiagnostics(document)

        assertEquals(emptyList(), result.diagnostics.warnings)
        assertEquals(200, result.document.blocks.size)
    }

    @Test
    fun balancedBracketsAreNeverGuarded() {
        val result = parser.parseWithDiagnostics("[]".repeat(5_000))

        assertEquals(emptyList(), result.diagnostics.warnings)
    }

    @Test
    fun bracketBombBecomesPlainTextWithAWarning() {
        val bomb = "[".repeat(2_000)
        val result = parser.parseWithDiagnostics("# Title\n\nbefore\n\n$bomb\n\nafter")

        assertEquals(
            listOf(
                OrcaParseWarning.InlineBracketLimitExceeded(
                    maxInlineBracketDepth = DEFAULT_MAX_INLINE_BRACKET_DEPTH,
                    exceededDepth = 2_000,
                    guardedBlocks = 1,
                ),
            ),
            result.diagnostics.warnings,
        )
        val blocks = result.document.blocks
        assertEquals(4, blocks.size)
        assertEquals(OrcaBlock.Paragraph(listOf(OrcaInline.Text(bomb))), blocks[2])
        assertEquals(OrcaBlock.Paragraph(listOf(OrcaInline.Text("after"))), blocks[3])
    }

    @Test
    fun onlyTheOffendingBlockIsGuarded() {
        val document = "intro [link](https://example.com)\n\n${"[".repeat(1_000)}\n\n## Heading\n\ntail"
        val result = parser.parse(document)

        assertEquals(4, result.blocks.size)
        val intro = result.blocks[0] as OrcaBlock.Paragraph
        assertTrue(intro.content.any { inline -> inline is OrcaInline.Link })
        assertEquals(OrcaBlock.Paragraph(listOf(OrcaInline.Text("[".repeat(1_000)))), result.blocks[1])
        assertEquals(2, (result.blocks[2] as OrcaBlock.Heading).level)
        assertEquals(OrcaBlock.Paragraph(listOf(OrcaInline.Text("tail"))), result.blocks[3])
    }

    @Test
    fun fencedCodeKeepsItsBracketsAndItsBlockType() {
        val bomb = "[".repeat(2_000)
        val result = parser.parseWithDiagnostics("```\n$bomb\n```")

        assertEquals(emptyList(), result.diagnostics.warnings)
        assertEquals(listOf(OrcaBlock.CodeBlock(code = bomb, language = null)), result.document.blocks)
    }

    @Test
    fun displayMathKeepsItsSource() {
        val bomb = "[".repeat(1_000)
        val result = parser.parseWithDiagnostics("$$\n$bomb\n$$")

        assertEquals(emptyList(), result.diagnostics.warnings)
        assertEquals(listOf(OrcaBlock.Math(bomb)), result.document.blocks)
    }

    @Test
    fun guardedBlocksInsideDetailsKeepTheirText() {
        val bomb = "[".repeat(1_000)
        val result = parser.parse("<details>\n<summary>S</summary>\n\n$bomb\n\n</details>")

        val details = result.blocks.filterIsInstance<OrcaBlock.Details>().single()
        assertEquals(listOf(OrcaBlock.Paragraph(listOf(OrcaInline.Text(bomb)))), details.blocks)
    }

    @Test
    fun theLimitIsConfigurable() {
        val strict = OrcaMarkdownParser(cacheSize = 1, maxInlineBracketDepth = 4)
        val result = strict.parseWithDiagnostics("a [[[[[ b")

        assertEquals(listOf(OrcaBlock.Paragraph(listOf(OrcaInline.Text("a [[[[[ b")))), result.document.blocks)
        assertTrue(result.diagnostics.warnings.single() is OrcaParseWarning.InlineBracketLimitExceeded)
    }

    /**
     * 25 600 unmatched openers used to take minutes: the inline scanner backtracks over
     * every one of them for every following opener. The timeout is generous on purpose —
     * it is here to catch a hang, not to measure speed (the benchmarks do that).
     */
    @JUnitTest(timeout = 30_000)
    fun pathologicalInlineInputStaysFast() {
        listOf("[", "![", "[^", "^[", "[]([").forEach { pattern ->
            val guarded = OrcaMarkdownParser(cacheSize = 1).parse(pattern.repeat(25_600))
            assertTrue(guarded.blocks.isNotEmpty(), pattern)
        }
    }

    @Test
    fun deeplyNestedBlocksBecomePlainTextWithAWarning() {
        val quotes = "> ".repeat(1_000) + "text"
        val result = parser.parseWithDiagnostics(quotes)

        assertEquals(listOf(OrcaBlock.Paragraph(listOf(OrcaInline.Text(quotes)))), result.document.blocks)
        assertEquals(
            listOf(
                OrcaParseWarning.BlockNestingLimitExceeded(
                    maxBlockNestingDepth = DEFAULT_MAX_BLOCK_NESTING_DEPTH,
                    exceededDepth = 1_000,
                    guardedBlocks = 1,
                ),
            ),
            result.diagnostics.warnings,
        )
    }

    @Test
    fun ordinaryNestingIsParsedAndStillReportsTheTreeDepthLimit() {
        // 64 levels is far past anything real but well inside the guard, so the existing
        // maxTreeDepth contract (truncated tree plus a warning) has to keep working.
        val result = OrcaMarkdownParser(cacheSize = 1, maxTreeDepth = 8)
            .parseWithDiagnostics("> ".repeat(64) + "deep")

        assertTrue(result.document.blocks.first() is OrcaBlock.Quote)
        assertTrue(result.diagnostics.warnings.single() is OrcaParseWarning.DepthLimitExceeded)
    }

    @Test
    fun nestingGuardIgnoresIndentedCodeAndFencedContent() {
        val indented = " ".repeat(400) + "code line"
        assertEquals(emptyList(), parser.parseWithDiagnostics(indented).diagnostics.warnings)
        assertTrue(parser.parse(indented).blocks.single() is OrcaBlock.CodeBlock)

        val fenced = "```\n" + "> ".repeat(1_000) + "\n```"
        assertEquals(emptyList(), parser.parseWithDiagnostics(fenced).diagnostics.warnings)
        assertTrue(parser.parse(fenced).blocks.single() is OrcaBlock.CodeBlock)
    }

    @Test
    fun realisticNestingIsNotGuarded() {
        val document = buildString {
            repeat(12) { level -> append(" ".repeat(level * 2)).append("- item $level\n") }
            append("\n")
            repeat(6) { append("> ") }
            append("quoted\n")
        }
        val result = parser.parseWithDiagnostics(document)

        assertEquals(emptyList(), result.diagnostics.warnings)
        assertTrue(result.document.blocks.first() is OrcaBlock.ListBlock)
    }

    /**
     * Deep nesting recurses in the block parser and used to exhaust a 1 MB stack (or run
     * for minutes on nested list items) long before any AST depth limit was reached.
     */
    @JUnitTest(timeout = 30_000)
    fun pathologicalNestingStaysFast() {
        val quotes = "> ".repeat(25_600) + "text"
        val lists = (0 until 4_096).joinToString("\n") { level -> " ".repeat(level * 2) + "- item" }

        listOf(quotes, lists).forEach { document ->
            var failure: Throwable? = null
            val worker = Thread(
                null,
                { OrcaMarkdownParser(cacheSize = 1).parse(document) },
                "guard-small-stack",
                1L * 1024 * 1024,
            )
            worker.setUncaughtExceptionHandler { _, error -> failure = error }
            worker.start()
            worker.join(20_000)

            assertTrue(!worker.isAlive, "parse did not finish")
            assertEquals(null, failure?.let { error -> error::class.simpleName })
        }
    }
}
