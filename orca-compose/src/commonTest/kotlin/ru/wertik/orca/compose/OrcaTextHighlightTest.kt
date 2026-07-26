package ru.wertik.orca.compose

import ru.wertik.orca.core.OrcaInline
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OrcaTextHighlightTest {

    private val style = OrcaDefaults.lightStyle()

    private fun build(
        inlines: List<OrcaInline>,
        highlight: OrcaTextHighlight?,
    ) = buildInlineAnnotatedString(
        inlines = inlines,
        style = style,
        onLinkClick = {},
        highlight = highlight,
    )

    private fun matchRanges(inlines: List<OrcaInline>, highlight: OrcaTextHighlight?): List<IntRange> {
        return build(inlines, highlight).spanStyles
            .filter { range -> range.item.background == style.inline.searchMatch.background }
            .map { range -> range.start until range.end }
    }

    @Test
    fun highlightsEveryOccurrenceIgnoringCase() {
        val inlines = listOf(OrcaInline.Text("Stream and stream and STREAM"))

        val ranges = matchRanges(inlines, OrcaTextHighlight("stream"))

        assertEquals(3, ranges.size)
        assertEquals(0 until 6, ranges.first())
    }

    @Test
    fun highlightSpansMarkupBoundaries() {
        val inlines = listOf(
            OrcaInline.Text("flat "),
            OrcaInline.Bold(listOf(OrcaInline.Text("design"))),
            OrcaInline.Text(" system"),
        )

        val ranges = matchRanges(inlines, OrcaTextHighlight("flat design"))

        assertEquals(listOf(0 until 11), ranges)
    }

    @Test
    fun caseSensitiveAndWholeWordOptionsNarrowMatches() {
        val inlines = listOf(OrcaInline.Text("Stream streaming stream"))

        assertEquals(1, matchRanges(inlines, OrcaTextHighlight("Stream", caseSensitive = true)).size)
        assertEquals(2, matchRanges(inlines, OrcaTextHighlight("stream", wholeWord = true)).size)
    }

    @Test
    fun blankOrMissingHighlightLeavesTextUnchanged() {
        val inlines = listOf(OrcaInline.Text("nothing to shade"))

        assertTrue(matchRanges(inlines, null).isEmpty())
        assertTrue(matchRanges(inlines, OrcaTextHighlight("   ")).isEmpty())
        assertTrue(matchRanges(inlines, OrcaTextHighlight("absent")).isEmpty())
        assertEquals("nothing to shade", build(inlines, OrcaTextHighlight("absent")).text)
    }

    @Test
    fun highlightIsInactiveForBlankQueries() {
        assertTrue(OrcaTextHighlight("query").isActive)
        assertTrue(!OrcaTextHighlight(" ").isActive)
    }
}
