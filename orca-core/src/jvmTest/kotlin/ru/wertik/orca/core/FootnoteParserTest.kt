package ru.wertik.orca.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FootnoteParserTest {

    private val parser = OrcaMarkdownParser()

    @Test
    fun noFootnotesProducesNoFootnotesBlock() {
        val result = parser.parseWithDiagnostics("# Heading\n\nSome paragraph.")
        val footnotes = result.document.blocks.filterIsInstance<OrcaBlock.Footnotes>()
        assertTrue(footnotes.isEmpty())
    }

    @Test
    fun singleFootnoteDefinitionIsExtracted() {
        val input = "Body text.[^note]\n\n[^note]: This is the footnote."
        val result = parser.parseWithDiagnostics(input)

        val footnotes = result.document.blocks.filterIsInstance<OrcaBlock.Footnotes>().firstOrNull()
        assertTrue(footnotes != null)
        val def = footnotes!!.definitions.find { it.label == "note" }
        assertTrue(def != null)
    }

    @Test
    fun multipleFootnoteDefinitionsAreExtracted() {
        val input = "Text[^a] and[^b].\n\n[^a]: First\n[^b]: Second"
        val result = parser.parseWithDiagnostics(input)

        val footnotes = result.document.blocks.filterIsInstance<OrcaBlock.Footnotes>().firstOrNull()
        assertTrue(footnotes != null)
        val labels = footnotes!!.definitions.map { it.label }
        assertTrue(labels.contains("a"))
        assertTrue(labels.contains("b"))
    }

    @Test
    fun footnoteReferenceAppearsAsInline() {
        val input = "See this[^ref].\n\n[^ref]: The reference."
        val result = parser.parseWithDiagnostics(input)

        val paragraph = result.document.blocks.filterIsInstance<OrcaBlock.Paragraph>().firstOrNull()
        assertTrue(paragraph != null)
        val hasRef = paragraph!!.content.any { it is OrcaInline.FootnoteReference && it.label == "ref" }
        assertTrue(hasRef)
    }

    @Test
    fun footnoteDefinitionBlockContainsInnerBlocks() {
        val input = "Text[^note].\n\n[^note]: This is the footnote body."
        val result = parser.parseWithDiagnostics(input)

        val footnotes = result.document.blocks.filterIsInstance<OrcaBlock.Footnotes>().firstOrNull()
        val def = footnotes?.definitions?.find { it.label == "note" }
        assertTrue(def != null)
        assertTrue(def!!.blocks.isNotEmpty())
    }

    @Test
    fun unusedFootnoteDefinitionIsStillIncluded() {
        val input = "No references here.\n\n[^unused]: Orphan footnote."
        val result = parser.parseWithDiagnostics(input)

        val footnotes = result.document.blocks.filterIsInstance<OrcaBlock.Footnotes>().firstOrNull()
        assertTrue(footnotes != null)
        val def = footnotes!!.definitions.find { it.label == "unused" }
        assertTrue(def != null)
    }
}
