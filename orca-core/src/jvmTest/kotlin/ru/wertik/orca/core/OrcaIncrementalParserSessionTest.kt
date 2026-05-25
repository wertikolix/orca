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

        assertEquals(listOf("Tail grows."), parsedInputs)
    }

    @Test
    fun richMarkdownFallsBackToFullParser() {
        val session = OrcaIncrementalParserSession(OrcaMarkdownParser())
        val input = "# Heading\n\n- one\n- two"

        assertEquals(fullParser.parse(input), session.parse(input))
        assertEquals(1, session.stats.fullParses)
        assertEquals(0, session.stats.incrementalParses)
    }

    @Test
    fun inlineFootnotesFallBackBecauseDefinitionsAreDocumentScoped() {
        val session = OrcaIncrementalParserSession(OrcaMarkdownParser())
        val input = "Paragraph with note ^[inside].\n\nTail"

        assertEquals(fullParser.parse(input), session.parse(input))
        assertEquals(1, session.stats.fullParses)
    }

    @Test
    fun tablesFallBackToFullParser() {
        val session = OrcaIncrementalParserSession(OrcaMarkdownParser())
        val input = "| Header |\n| --- |\n| Body |\n\nTail"

        assertEquals(fullParser.parse(input), session.parse(input))
        assertEquals(1, session.stats.fullParses)
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
}
