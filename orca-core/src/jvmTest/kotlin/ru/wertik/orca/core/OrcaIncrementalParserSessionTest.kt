package ru.wertik.orca.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OrcaIncrementalParserSessionTest {
    private val fullParser = OrcaMarkdownParser()

    @Test
    fun proseStreamMatchesFullParserAndReusesStableBlocks() {
        val session = OrcaIncrementalParserSession(OrcaMarkdownParser())
        val first = session.parse("First paragraph.\n\nSecond")
        val stableFirstBlock = first.blocks.first()
        val secondInput = "First paragraph.\n\nSecond paragraph grows."
        val second = session.parse(secondInput)

        assertEquals(fullParser.parse(secondInput), second)
        assertTrue(stableFirstBlock === second.blocks.first())
        assertEquals(2, session.stats.incrementalParses)
        assertTrue(session.stats.reusedStableBlocks >= 2)
    }

    @Test
    fun subsequentProseUpdateParsesOnlyActiveTail() {
        val parsedInputs = mutableListOf<String>()
        val delegate = object : OrcaParser {
            private val parser = OrcaMarkdownParser()

            override fun parse(input: String): OrcaDocument = parser.parse(input)

            override fun parseWithDiagnostics(input: String): OrcaParseResult {
                parsedInputs += input
                return parser.parseWithDiagnostics(input)
            }
        }
        val session = OrcaIncrementalParserSession(delegate)
        session.parse("Stable paragraph.\n\nTail")
        parsedInputs.clear()

        session.parse("Stable paragraph.\n\nTail grows.")

        assertEquals(listOf("\nTail grows."), parsedInputs)
    }

    @Test
    fun headingsListsAndTablesUseTheIncrementalPath() {
        val session = OrcaIncrementalParserSession(OrcaMarkdownParser())
        val input = "# Heading\n\n- one\n- two\n\n| H |\n| --- |\n| B |\n\ntail prose"

        assertEquals(fullParser.parse(input), session.parse(input))
        assertEquals(0, session.stats.fullParses)
        assertEquals(1, session.stats.incrementalParses)
    }

    @Test
    fun closedFencesAreFrozenAndReused() {
        val session = OrcaIncrementalParserSession(OrcaMarkdownParser())
        val first = session.parse("```kotlin\nval x = 1\n\nval y = 2\n```\n\ntail")
        val fenceBlock = first.blocks.first()
        val secondInput = "```kotlin\nval x = 1\n\nval y = 2\n```\n\ntail grows here"
        val second = session.parse(secondInput)

        assertEquals(fullParser.parse(secondInput), second)
        assertTrue(fenceBlock === second.blocks.first())
    }

    @Test
    fun unclosedFenceStaysInActiveTail() {
        val session = OrcaIncrementalParserSession(OrcaMarkdownParser())
        val input = "intro\n\n```kotlin\nval x = 1\n\nstill code"

        assertEquals(fullParser.parse(input), session.parse(input))
        assertEquals(1, session.stats.incrementalParses)
    }

    @Test
    fun listsAcrossBlankLinesAreNotSplit() {
        val session = OrcaIncrementalParserSession(OrcaMarkdownParser())
        val input = "- alpha\n\n- beta\n\nafter list"

        assertEquals(fullParser.parse(input), session.parse(input))
    }

    @Test
    fun orderedListNumberingSurvivesStreaming() {
        val session = OrcaIncrementalParserSession(OrcaMarkdownParser())
        val chunks = listOf(
            "1. one\n\n",
            "1. one\n\n2",
            "1. one\n\n2. two\n\nafter",
        )
        chunks.forEach { chunk ->
            assertEquals(fullParser.parse(chunk), session.parse(chunk))
        }
    }

    @Test
    fun duplicateHeadingSlugsMatchFullParse() {
        val session = OrcaIncrementalParserSession(OrcaMarkdownParser())
        session.parse("# Same\n\nbody\n\n")
        val input = "# Same\n\nbody\n\n# Same\n\ntail"
        val document = session.parse(input)

        assertEquals(fullParser.parse(input), document)
        val ids = document.blocks.filterIsInstance<OrcaBlock.Heading>().map { it.id }
        assertEquals(listOf("same", "same-1"), ids)
    }

    @Test
    fun thematicBreaksAreNotMistakenForFrontMatter() {
        val session = OrcaIncrementalParserSession(OrcaMarkdownParser())
        val input = "intro\n\n---\n\nmiddle\n\n---\n\ntail"

        assertEquals(fullParser.parse(input), session.parse(input))
        assertTrue(session.stats.incrementalParses >= 1)
    }

    @Test
    fun detailsBlocksFreezeTheBoundary() {
        val session = OrcaIncrementalParserSession(OrcaMarkdownParser())
        val input = "before\n\n<details>\n<summary>S</summary>\n\ncontent\n\n</details>\n\nafter"

        assertEquals(fullParser.parse(input), session.parse(input))
    }

    @Test
    fun displayMathFreezesTheBoundary() {
        val session = OrcaIncrementalParserSession(OrcaMarkdownParser())
        val input = "before\n\n$$\na + b\n\n= c\n$$\n\nafter"

        assertEquals(fullParser.parse(input), session.parse(input))
    }

    @Test
    fun documentScopedConstructsFallBackToFullParser() {
        listOf(
            "Paragraph with note ^[inside].\n\nTail",
            "Reference[^1] use.\n\n[^1]: Definition\n\nTail",
            "*[KMP]: Kotlin Multiplatform\n\nKMP text\n\nTail",
            "---\ntitle: doc\n---\n\nBody\n\nTail",
            "Term\n\n: definition body\n\nTail",
        ).forEach { input ->
            val session = OrcaIncrementalParserSession(OrcaMarkdownParser())
            assertEquals(fullParser.parse(input), session.parse(input), input)
            assertEquals(1, session.stats.fullParses, input)
        }
    }

    @Test
    fun replacementInputResetsStablePrefix() {
        val session = OrcaIncrementalParserSession(OrcaMarkdownParser())
        session.parse("First paragraph.\n\nTail")
        val replacement = "Changed paragraph.\n\nTail"

        assertEquals(fullParser.parse(replacement), session.parse(replacement))
    }

    @Test
    fun repeatedInputReturnsSameResultWithoutNewParse() {
        val session = OrcaIncrementalParserSession(OrcaMarkdownParser())
        val input = "First paragraph.\n\nTail"
        val first = session.parseWithDiagnostics(input)
        val before = session.stats
        val second = session.parseWithDiagnostics(input)

        assertTrue(first === second)
        assertEquals(before, session.stats)
    }

    @Test
    fun everyStreamedPrefixMatchesTheFullParser() {
        val document = """
            # Streaming document

            Intro paragraph with **bold**, ==highlight==, ++underline++ and `code`.

            ## Lists

            - first item
            - second item
                - nested item

            1. ordered one
            2. ordered two

            ```kotlin
            fun main() {
                println("hi")

                println("blank line above stays inside the fence")
            }
            ```

            > [!NOTE]
            > Admonitions keep working.

            > Plain quote with a [link](https://example.com).

            | Column | Value |
            |--------|-------|
            | a      | 1     |
            | b      | 2     |

            ---

            ## Lists

            Duplicate heading title above checks slug numbering.

                indented code block
                second line

            ### Endnotes

            *** 

            Final prose paragraph that keeps growing until the very end.
        """.trimIndent()

        listOf(1, 3, 7, 16, 41).forEach { chunkSize ->
            val session = OrcaIncrementalParserSession(OrcaMarkdownParser())
            var streamed = ""
            document.chunked(chunkSize).forEach { chunk ->
                streamed += chunk
                assertEquals(
                    fullParser.parse(streamed),
                    session.parse(streamed),
                    "chunkSize=$chunkSize length=${streamed.length}",
                )
            }
            assertTrue(session.stats.incrementalParses > 0, "chunkSize=$chunkSize")
        }
    }
}
