package ru.wertik.orca.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OrcaSearchTest {

    private val parser = OrcaMarkdownParser()

    private val document = parser.parse(
        """
        # Streaming guide

        Streaming keeps the reader in sync while tokens arrive.

        ## Details

        The **streaming** cursor is optional.
        """.trimIndent(),
    )

    @Test
    fun findsMatchesAcrossBlocksIgnoringCaseByDefault() {
        val matches = document.findMatches("streaming")

        assertEquals(listOf(0, 1, 3), matches.map { it.blockIndex })
        assertEquals(3, matches.size)
    }

    @Test
    fun matchesIgnoreInlineMarkup() {
        val matches = parser.parse("A **bold word** here.").findMatches("bold word")

        assertEquals(1, matches.size)
        assertEquals("A bold word here.", matches.single().blockText)
    }

    @Test
    fun caseSensitiveSearchOnlyMatchesExactCasing() {
        val matches = document.findMatches(
            query = "Streaming",
            options = OrcaSearchOptions(caseSensitive = true),
        )

        assertEquals(listOf(0, 1), matches.map { it.blockIndex })
    }

    @Test
    fun wholeWordSearchSkipsPartialMatches() {
        val source = parser.parse("stream streaming streamed")

        assertEquals(3, source.findMatches("stream").size)
        assertEquals(
            1,
            source.findMatches("stream", OrcaSearchOptions(wholeWord = true)).size,
        )
    }

    @Test
    fun matchesCarryNearestHeadingAndSnippet() {
        val match = document.findMatches("cursor").single()

        assertEquals("Details", match.headingTitle)
        assertEquals("details", match.headingId)
        assertTrue(match.snippet.contains("cursor"))
        assertEquals("cursor", match.blockText.substring(match.range))
    }

    @Test
    fun snippetIsElidedAroundLongBlocks() {
        val filler = (1..40).joinToString(" ") { "word" }
        val long = parser.parse("$filler needle $filler")

        val snippet = long.findMatches("needle").single().snippet

        assertTrue(snippet.startsWith("…"), snippet)
        assertTrue(snippet.endsWith("…"), snippet)
        assertTrue(snippet.contains("needle"), snippet)
    }

    @Test
    fun blankQueryAndLimitsAreRespected() {
        assertEquals(emptyList(), document.findMatches("   "))
        assertEquals(1, document.findMatches("streaming", OrcaSearchOptions(limit = 1)).size)
        assertEquals(3, document.countMatches("streaming"))
    }
}

class OrcaPlainTextTest {

    private val parser = OrcaMarkdownParser()

    @Test
    fun documentFlattensBlocksInReadingOrder() {
        val document = parser.parse(
            """
            # Title

            Body *text*.
            """.trimIndent(),
        )

        assertEquals("Title\n\nBody text.", document.plainText())
    }

    @Test
    fun nestedBlocksAreIncluded() {
        val document = parser.parse(
            """
            > [!NOTE]
            > Stay flat.
            """.trimIndent(),
        )

        val text = document.plainText()

        assertTrue(text.contains("Stay flat."), text)
    }

    @Test
    fun tableCellsAreTabSeparated() {
        val document = parser.parse(
            """
            | A | B |
            | - | - |
            | 1 | 2 |
            """.trimIndent(),
        )

        assertEquals("A\tB\n1\t2", document.plainText())
    }

    @Test
    fun inlineListFlattensToPlainText() {
        val inlines = listOf(
            OrcaInline.Text("a "),
            OrcaInline.Bold(listOf(OrcaInline.Text("b"))),
            OrcaInline.InlineCode("c"),
        )

        assertEquals("a bc", inlines.plainText())
    }
}
