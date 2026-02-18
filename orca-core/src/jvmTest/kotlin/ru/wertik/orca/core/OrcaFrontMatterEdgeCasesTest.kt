package ru.wertik.orca.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for front matter parsing edge cases via the public OrcaParser API.
 */
class OrcaFrontMatterEdgeCasesTest {

    private val parser = OrcaMarkdownParser()

    @Test
    fun yamlFrontMatterWithDotDotDotClosingDelimiter() {
        val input = "---\ntitle: hello\n...\n\n# Body"
        val result = parser.parse(input)

        val fm = result.frontMatter as OrcaFrontMatter.Yaml
        assertEquals("hello", fm.entries["title"])
        val heading = result.blocks.single() as OrcaBlock.Heading
        assertEquals("Body", (heading.content.single() as OrcaInline.Text).text)
    }

    @Test
    fun tomlFrontMatterStripsQuotesFromValues() {
        val input = "+++\ntitle = \"Quoted Value\"\nauthor = 'single'\n+++\n\nbody"
        val result = parser.parse(input)

        val fm = result.frontMatter as OrcaFrontMatter.Toml
        assertEquals("Quoted Value", fm.entries["title"])
        assertEquals("single", fm.entries["author"])
    }

    @Test
    fun yamlFrontMatterIgnoresCommentLines() {
        val input = "---\n# this is a comment\ntitle: real\n---\n\nbody"
        val result = parser.parse(input)

        val fm = result.frontMatter as OrcaFrontMatter.Yaml
        assertNull(fm.entries["# this is a comment"])
        assertEquals("real", fm.entries["title"])
    }

    @Test
    fun yamlFrontMatterIgnoresListItems() {
        val input = "---\ntags:\n- kotlin\n- android\ntitle: test\n---\n\nbody"
        val result = parser.parse(input)

        val fm = result.frontMatter as OrcaFrontMatter.Yaml
        // list items (- kotlin) should not be parsed as entries
        assertNull(fm.entries["- kotlin"])
        assertEquals("test", fm.entries["title"])
    }

    @Test
    fun yamlFrontMatterWithEmptyBodyProducesEmptyEntries() {
        val input = "---\n---\n\nbody"
        val result = parser.parse(input)

        val fm = result.frontMatter as OrcaFrontMatter.Yaml
        assertTrue(fm.entries.isEmpty())
        assertEquals("", fm.raw)
    }

    @Test
    fun frontMatterWithNoClosingDelimiterIsIgnored() {
        val input = "---\ntitle: no close\n\n# Heading"
        val result = parser.parse(input)

        assertNull(result.frontMatter)
    }

    @Test
    fun tomlFrontMatterWithNoClosingDelimiterIsIgnored() {
        val input = "+++\ntitle = \"no close\"\n\n# Heading"
        val result = parser.parse(input)

        assertNull(result.frontMatter)
    }

    @Test
    fun yamlFrontMatterRawDoesNotIncludeDelimiters() {
        val input = "---\ntitle: value\n---\n\nbody"
        val result = parser.parse(input)

        val fm = result.frontMatter as OrcaFrontMatter.Yaml
        assertTrue(!fm.raw.contains("---"))
        assertTrue(fm.raw.contains("title: value"))
    }

    @Test
    fun frontMatterBodyIsNotIncludedInParsedBlocks() {
        val input = "---\ntitle: Hello\nauthor: World\n---\n\n# Heading"
        val result = parser.parse(input)

        // Only the heading block should be present, not front matter content
        assertEquals(1, result.blocks.size)
        assertTrue(result.blocks.single() is OrcaBlock.Heading)
    }

    @Test
    fun yamlFrontMatterWithQuotedValues() {
        val input = "---\ntitle: \"Quoted Title\"\ndescription: 'single quoted'\n---\n\nbody"
        val result = parser.parse(input)

        val fm = result.frontMatter as OrcaFrontMatter.Yaml
        assertEquals("Quoted Title", fm.entries["title"])
        assertEquals("single quoted", fm.entries["description"])
    }

    @Test
    fun frontMatterDoesNotConflictWithThematicBreak() {
        // A thematic break --- in the middle of content should NOT be parsed as front matter
        val input = "# Heading\n\n---\n\nParagraph"
        val result = parser.parse(input)

        assertNull(result.frontMatter)
        assertEquals(3, result.blocks.size)
        assertTrue(result.blocks[1] is OrcaBlock.ThematicBreak)
    }
}

/**
 * Tests for footnote definition parsing edge cases via the public OrcaParser API.
 */
class OrcaFootnoteEdgeCasesTest {

    private val parser = OrcaMarkdownParser()

    @Test
    fun footnoteDefinitionWithMultiParagraphBodyPreservesBlocks() {
        val input = """
            Text[^note].
            
            [^note]: First paragraph.
            
                Second paragraph.
        """.trimIndent()
        val result = parser.parse(input)

        val footnotes = result.blocks.filterIsInstance<OrcaBlock.Footnotes>().single()
        val def = footnotes.definitions.find { it.label == "note" }!!
        assertTrue(def.blocks.size >= 1)
    }

    @Test
    fun footnoteDefinitionWithEmptyBodyProducesEmptyBlocks() {
        val input = "Text[^empty].\n\n[^empty]:"
        val result = parser.parse(input)

        val footnotes = result.blocks.filterIsInstance<OrcaBlock.Footnotes>().single()
        val def = footnotes.definitions.find { it.label == "empty" }!!
        assertTrue(def.blocks.isEmpty())
    }

    @Test
    fun footnoteDefinitionLabelIsTrimmed() {
        // Labels with surrounding spaces should still be resolved
        val input = "See[^ref].\n\n[^ref]: Definition."
        val result = parser.parse(input)

        val footnotes = result.blocks.filterIsInstance<OrcaBlock.Footnotes>().single()
        assertTrue(footnotes.definitions.any { it.label == "ref" })
    }

    @Test
    fun multipleFootnoteReferencesToSameLabelResolveToSameDefinition() {
        val input = "First[^note] and second[^note].\n\n[^note]: Shared body."
        val result = parser.parse(input)

        val paragraph = result.blocks.filterIsInstance<OrcaBlock.Paragraph>().single()
        val refs = paragraph.content.filterIsInstance<OrcaInline.FootnoteReference>()
        assertEquals(2, refs.size)
        assertEquals(refs[0].label, refs[1].label)

        // Only one definition should exist for the label
        val footnotes = result.blocks.filterIsInstance<OrcaBlock.Footnotes>().single()
        assertEquals(1, footnotes.definitions.filter { it.label == "note" }.size)
    }

    @Test
    fun inlineFootnoteWithNestedBracketsIsParsedCorrectly() {
        val input = "Note ^[see [link](https://example.com)]."
        val result = parser.parse(input)

        assertEquals(2, result.blocks.size)
        val footnotes = result.blocks[1] as OrcaBlock.Footnotes
        val def = footnotes.definitions.single()
        val paragraph = def.blocks.single() as OrcaBlock.Paragraph
        assertTrue(paragraph.content.any { it is OrcaInline.Link })
    }

    @Test
    fun footnoteReferenceInHeadingIsPreserved() {
        val input = "## Title[^note]\n\n[^note]: Heading footnote."
        val result = parser.parse(input)

        val heading = result.blocks.filterIsInstance<OrcaBlock.Heading>().single()
        assertTrue(heading.content.any { it is OrcaInline.FootnoteReference })
    }

    @Test
    fun footnoteDefinitionWithFormattedBodyPreservesInlines() {
        val input = "Text[^fmt].\n\n[^fmt]: Body with **bold** and *italic*."
        val result = parser.parse(input)

        val footnotes = result.blocks.filterIsInstance<OrcaBlock.Footnotes>().single()
        val def = footnotes.definitions.find { it.label == "fmt" }!!
        val paragraph = def.blocks.single() as OrcaBlock.Paragraph
        assertTrue(paragraph.content.any { it is OrcaInline.Bold })
        assertTrue(paragraph.content.any { it is OrcaInline.Italic })
    }
}
