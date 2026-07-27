package ru.wertik.orca.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test as JUnitTest

/**
 * Covers the line scanner behind [extractDefinitionLists], which only probes lines that
 * can actually open a list. The parser-level behaviour lives in [OrcaDefinitionListTest];
 * these cases pin the scanner itself so the linear rewrite cannot drift.
 */
class OrcaDefinitionListScannerTest {
    @Test
    fun documentsWithoutDefinitionLinesArePassedThroughUnchanged() {
        listOf(
            "",
            "plain paragraph",
            "# Heading\n\ntext with a : colon in the middle\n\n- item",
            ":no-space-after-colon\n:smile: emoji shortcode",
            ":",
            ": ",
        ).forEach { markdown ->
            val extraction = extractDefinitionLists(markdown)

            assertEquals(markdown, extraction.markdown, markdown)
            assertEquals(emptyList(), extraction.definitionLists, markdown)
        }
    }

    @Test
    fun definitionLinesWithoutATermAreLeftInPlace() {
        val markdown = ": orphan one\n: orphan two\n\n: orphan three"
        val extraction = extractDefinitionLists(markdown)

        assertEquals(markdown, extraction.markdown)
        assertEquals(emptyList(), extraction.definitionLists)
    }

    @Test
    fun aListIsReplacedByOnePlaceholderStartingAtTheTerm() {
        val extraction = extractDefinitionLists("intro\n\nTerm\n: Definition\n\noutro")

        assertEquals("intro\n\n<!--orca:deflist:0-->\n\noutro", extraction.markdown)
        assertEquals(1, extraction.definitionLists.size)
        assertEquals("Term", extraction.definitionLists[0].items.single().term)
        assertEquals(listOf("Definition"), extraction.definitionLists[0].items.single().definitions)
    }

    @Test
    fun theTermMayBeSeparatedFromItsDefinitionByOneBlankLine() {
        val extraction = extractDefinitionLists("Term\n\n: Definition")

        assertEquals("<!--orca:deflist:0-->", extraction.markdown)
        assertEquals("Term", extraction.definitionLists.single().items.single().term)
    }

    @Test
    fun twoBlankLinesBreakTheBinding() {
        val markdown = "Term\n\n\n: Definition"
        val extraction = extractDefinitionLists(markdown)

        assertEquals(markdown, extraction.markdown)
        assertEquals(emptyList(), extraction.definitionLists)
    }

    @Test
    fun onlyTheLastLineOfATermRunOpensTheList() {
        val extraction = extractDefinitionLists("paragraph line one\nparagraph line two\n: Definition")

        assertEquals("<!--orca:deflist:0-->", extraction.markdown)
        assertEquals(
            listOf("paragraph line one", "paragraph line two"),
            extraction.definitionLists.single().items.map { item -> item.term },
        )
    }

    @Test
    fun continuationLinesStayWithTheirDefinition() {
        val extraction = extractDefinitionLists("Term\n: First line\n    continued line\n: Second")

        assertEquals(
            listOf("First line\ncontinued line", "Second"),
            extraction.definitionLists.single().items.single().definitions,
        )
    }

    @Test
    fun severalListsGetSequentialPlaceholders() {
        val extraction = extractDefinitionLists("A\n: one\n\nprose\n\nB\n: two")

        assertEquals("<!--orca:deflist:0-->\n\nprose\n\n<!--orca:deflist:1-->", extraction.markdown)
        assertEquals(2, extraction.definitionLists.size)
    }

    /**
     * The scanner used to probe every line for a list start, which is quadratic on a
     * document that is one long paragraph — the single most common shape there is.
     * Timeouts here only guard against that class of blow-up; the ratios are measured
     * by `:orca-benchmarks`.
     */
    @JUnitTest(timeout = 30_000)
    fun largeDocumentsWithoutDefinitionListsStayFast() {
        val lines = 40_000
        val paragraph = (0 until lines).joinToString("\n") { index -> "prose line $index in one paragraph" }

        val extraction = extractDefinitionLists(paragraph)
        assertEquals(paragraph, extraction.markdown)

        val blankTail = "Term\n: def\n" + "\n".repeat(lines)
        assertEquals(1, extractDefinitionLists(blankTail).definitionLists.size)

        val orphans = (0 until lines).joinToString("\n") { index -> ": orphan definition $index" }
        assertTrue(extractDefinitionLists(orphans).definitionLists.isEmpty())
    }
}
