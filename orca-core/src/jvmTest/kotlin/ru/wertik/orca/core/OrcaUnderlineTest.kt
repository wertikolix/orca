package ru.wertik.orca.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OrcaUnderlineTest {

    private val parser = OrcaMarkdownParser()

    @Test
    fun underlineParsedCorrectly() {
        val result = parser.parse("This is ++inserted text++ in a sentence.")
        val paragraph = result.blocks.single() as OrcaBlock.Paragraph
        val underline = paragraph.content.filterIsInstance<OrcaInline.Underline>()
        assertEquals(1, underline.size)
        val text = (underline.single().content.single() as OrcaInline.Text).text
        assertEquals("inserted text", text)
    }

    @Test
    fun surroundingTextPreserved() {
        val result = parser.parse("before ++mid++ after")
        val paragraph = result.blocks.single() as OrcaBlock.Paragraph
        val plain = paragraph.content.filterIsInstance<OrcaInline.Text>().joinToString("") { it.text }
        assertEquals("before  after", plain)
    }

    @Test
    fun multipleUnderlinesInOneParagraph() {
        val result = parser.parse("++one++ and ++two++")
        val paragraph = result.blocks.single() as OrcaBlock.Paragraph
        assertEquals(2, paragraph.content.filterIsInstance<OrcaInline.Underline>().size)
    }

    @Test
    fun singlePlusNotParsed() {
        val result = parser.parse("a + b + c")
        val paragraph = result.blocks.single() as OrcaBlock.Paragraph
        assertTrue(paragraph.content.none { it is OrcaInline.Underline })
    }

    @Test
    fun unbalancedDelimitersLeftAsText() {
        val result = parser.parse("broken ++underline without closing")
        val paragraph = result.blocks.single() as OrcaBlock.Paragraph
        assertTrue(paragraph.content.none { it is OrcaInline.Underline })
    }

    @Test
    fun underlineCombinesWithHighlightAndSuperscript() {
        val result = parser.parse("++ins++ ==mark== x^2^")
        val paragraph = result.blocks.single() as OrcaBlock.Paragraph
        assertEquals(1, paragraph.content.filterIsInstance<OrcaInline.Underline>().size)
        assertEquals(1, paragraph.content.filterIsInstance<OrcaInline.Highlight>().size)
        assertEquals(1, paragraph.content.filterIsInstance<OrcaInline.Superscript>().size)
    }

    @Test
    fun underlineInsideBoldParsed() {
        val result = parser.parse("**bold ++ins++ text**")
        val paragraph = result.blocks.single() as OrcaBlock.Paragraph
        val bold = paragraph.content.filterIsInstance<OrcaInline.Bold>().single()
        assertEquals(1, bold.content.filterIsInstance<OrcaInline.Underline>().size)
    }

    @Test
    fun tomlFrontMatterNotConfusedWithUnderline() {
        val result = parser.parse("+++\ntitle = \"Doc\"\n+++\n\nBody ++ins++ text")
        assertTrue(result.frontMatter is OrcaFrontMatter.Toml)
        val paragraph = result.blocks.single() as OrcaBlock.Paragraph
        assertEquals(1, paragraph.content.filterIsInstance<OrcaInline.Underline>().size)
    }
}
