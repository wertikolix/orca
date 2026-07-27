package ru.wertik.orca.core

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The incremental session is only allowed to be faster, never different: every prefix of
 * a stream must parse to exactly what a full parse of that prefix produces.
 *
 * These cases exist because the segment boundary rules are subtle — a fence opener with
 * backticks in its info string, a list that starts halfway into a segment, or a `$$`
 * pair the delegate extracts before parsing all move the safe cut point around.
 */
class OrcaIncrementalStreamEquivalenceTest {
    private val fullParser = OrcaMarkdownParser(cacheSize = 8)

    private fun assertStreamMatchesFullParse(document: String, chunkSizes: List<Int> = listOf(1, 5, 17)) {
        chunkSizes.forEach { chunkSize ->
            val session = OrcaIncrementalParserSession(OrcaMarkdownParser(cacheSize = 4))
            var streamed = ""
            document.chunked(chunkSize).forEach { chunk ->
                streamed += chunk
                assertEquals(
                    fullParser.parse(streamed),
                    session.parse(streamed),
                    "chunk=$chunkSize length=${streamed.length} document=<<<$document>>>",
                )
            }
        }
    }

    @Test
    fun assistantShapedAnswerStreamsIdentically() {
        assertStreamMatchesFullParse(
            """
                Here is how to do it.
                ```kotlin
                fun main() {
                    println("hi")

                    println("blank lines stay inside the fence")
                }
                ```
                Then a paragraph directly after the closing fence.

                ## Notes

                - first
                - second

                ```bash
                ./gradlew build
                ```

                Final paragraph.
            """.trimIndent(),
            chunkSizes = listOf(1, 3, 11, 29),
        )
    }

    @Test
    fun fenceEdgeCasesStreamIdentically() {
        listOf(
            // Backticks in the info string: not a fence at all.
            "``` `weird` info\ntext below\n\nmore text\n",
            // Nested fences of different widths.
            "````markdown\n```\ninner\n```\n````\n\nafter\n",
            // Indented closing candidates do not close a column-zero fence.
            "```\ncode\n    ```\nstill code\n```\n\nafter\n",
            // Tilde fences carry anything in the info string.
            "~~~python ~x~\nprint(1)\n~~~\n\nafter\n",
            // Unclosed fence at the end of the stream.
            "intro\n\n```kotlin\nval x = 1\n\nval y = 2\n",
            // Fence directly after a list, which the fence terminates.
            "- item\n```\ncode\n```\ntail\n",
            // Fence content that the pre-passes react to.
            "intro\n\n```md\n$$\nx\n$$\n<details>\n</details>\n```\n\ntail\n",
        ).forEach { document -> assertStreamMatchesFullParse(document) }
    }

    @Test
    fun continuationEdgeCasesStreamIdentically() {
        listOf(
            // A list that starts on the second line of a segment can still grow.
            "---\n- list item\n\n- another item\n\ntail\n",
            "paragraph\n- list item\n\n- another item\n",
            // Indented continuation after a blank line.
            "- item\n\n  continued item body\n\ntail\n",
            // Display math and details block the boundary.
            "before\n\n$$\na + b\n\n= c\n$$\n\nafter\n",
            "before\n\n<details>\n<summary>S</summary>\n\ncontent\n\n</details>\n\nafter\n",
        ).forEach { document -> assertStreamMatchesFullParse(document) }
    }

    @Test
    fun randomFragmentDocumentsStreamIdentically() {
        val fragments = listOf(
            "# Title\n", "## Section\n", "#not-a-heading\n", "Plain paragraph line.\n",
            "Another line of the same paragraph.\n", "\n", "```kotlin\n", "fun main() = Unit\n",
            "```\n", "``` `weird` info\n", "````\n", "  ```\n", "~~~python\n", "- list item\n",
            "  nested continuation\n", "1. ordered\n", "> quote line\n", "| a | b |\n", "|---|---|\n",
            "---\n", "***\n", "    indented code\n", "<div>\n", "</div>\n", "$$\n", "x + y\n",
            "text with `code span` and **bold**\n", "[link](https://example.com)\n",
        )
        val random = Random(20240513)
        repeat(400) {
            val document = (0 until random.nextInt(2, 12))
                .joinToString("") { fragments[random.nextInt(fragments.size)] }
            assertStreamMatchesFullParse(document, chunkSizes = listOf(1, 7))
        }
    }

    @Test
    fun growingCodeFenceIsRebuiltInsteadOfReparsed() {
        val document = "Intro paragraph.\n\n```kotlin\n" +
            (0 until 40).joinToString("\n") { index -> "fun handler$index() = $index" } +
            "\n```\n\nOutro paragraph.\n"
        val parsedInputs = mutableListOf<Int>()
        val delegate = object : OrcaParser {
            private val parser = OrcaMarkdownParser(cacheSize = 4)

            override fun parse(input: String): OrcaDocument = parseWithDiagnostics(input).document

            override fun parseWithDiagnostics(input: String): OrcaParseResult {
                parsedInputs += input.length
                return parser.parseWithDiagnostics(input)
            }
        }
        val session = OrcaIncrementalParserSession(delegate)

        var streamed = ""
        document.chunked(8).forEach { chunk ->
            streamed += chunk
            assertEquals(fullParser.parse(streamed), session.parse(streamed), "length=${streamed.length}")
        }

        assertTrue(session.stats.codeFenceFastPaths > 50, "stats=${session.stats}")
        // Re-parsing every prefix would feed the delegate the whole document each time.
        assertTrue(parsedInputs.sum() < document.length * 3, "delegate saw ${parsedInputs.sum()} chars")
    }

    @Test
    fun frozenBlocksKeepTheirIdentityAcrossUpdates() {
        val session = OrcaIncrementalParserSession(OrcaMarkdownParser())
        val first = session.parse("# Heading\n\nStable paragraph.\n\ntail")
        val frozenHeading = first.blocks[0]
        val frozenParagraph = first.blocks[1]

        val second = session.parse("# Heading\n\nStable paragraph.\n\ntail grows longer")

        assertTrue(frozenHeading === second.blocks[0])
        assertTrue(frozenParagraph === second.blocks[1])
    }
}
