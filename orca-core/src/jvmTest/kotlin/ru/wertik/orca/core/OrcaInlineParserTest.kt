package ru.wertik.orca.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for inline node parsing edge cases via the public OrcaParser API.
 */
class OrcaInlineParserTest {

    private val parser = OrcaMarkdownParser()

    // --- bold / italic nesting ---

    @Test
    fun boldInsideItalicProducesNestedNodes() {
        val result = parser.parse("*outer **inner** outer*")
        val paragraph = result.blocks.single() as OrcaBlock.Paragraph
        val italic = paragraph.content.filterIsInstance<OrcaInline.Italic>().single()
        assertTrue(italic.content.any { it is OrcaInline.Bold })
    }

    @Test
    fun italicInsideBoldProducesNestedNodes() {
        val result = parser.parse("**outer *inner* outer**")
        val paragraph = result.blocks.single() as OrcaBlock.Paragraph
        val bold = paragraph.content.filterIsInstance<OrcaInline.Bold>().single()
        assertTrue(bold.content.any { it is OrcaInline.Italic })
    }

    @Test
    fun strikethroughContainsBoldText() {
        val result = parser.parse("~~**important**~~")
        val paragraph = result.blocks.single() as OrcaBlock.Paragraph
        val strike = paragraph.content.filterIsInstance<OrcaInline.Strikethrough>().single()
        assertTrue(strike.content.any { it is OrcaInline.Bold })
    }

    // --- inline code ---

    @Test
    fun inlineCodeWithBacktickEscapeHandledCorrectly() {
        val result = parser.parse("Use `` `tick` `` here")
        val paragraph = result.blocks.single() as OrcaBlock.Paragraph
        val code = paragraph.content.filterIsInstance<OrcaInline.InlineCode>().single()
        assertEquals("`tick`", code.code)
    }

    @Test
    fun inlineCodeStripsLeadingAndTrailingSpaceWhenSurrounded() {
        val result = parser.parse("` code `")
        val paragraph = result.blocks.single() as OrcaBlock.Paragraph
        val code = paragraph.content.filterIsInstance<OrcaInline.InlineCode>().single()
        assertEquals("code", code.code)
    }

    // --- links ---

    @Test
    fun linkWithTitleAttributeIsIgnoredInDestination() {
        val result = parser.parse("[text](https://example.com \"My Title\")")
        val paragraph = result.blocks.single() as OrcaBlock.Paragraph
        val link = paragraph.content.filterIsInstance<OrcaInline.Link>().single()
        assertEquals("https://example.com", link.destination)
        assertEquals(listOf(OrcaInline.Text("text")), link.content)
    }

    @Test
    fun linkWithEmptyDestinationHasEmptyString() {
        val result = parser.parse("[text]()")
        val paragraph = result.blocks.single() as OrcaBlock.Paragraph
        val link = paragraph.content.filterIsInstance<OrcaInline.Link>().single()
        assertEquals("", link.destination)
    }

    @Test
    fun linkContentCanContainInlineCode() {
        val result = parser.parse("[`code`](https://example.com)")
        val paragraph = result.blocks.single() as OrcaBlock.Paragraph
        val link = paragraph.content.filterIsInstance<OrcaInline.Link>().single()
        assertTrue(link.content.any { it is OrcaInline.InlineCode })
    }

    @Test
    fun shortReferenceLinkResolvesDestination() {
        val markdown = """
            See [docs].
            
            [docs]: https://example.com
        """.trimIndent()
        val result = parser.parse(markdown)
        val paragraph = result.blocks.first() as OrcaBlock.Paragraph
        val link = paragraph.content.filterIsInstance<OrcaInline.Link>().single()
        assertEquals("https://example.com", link.destination)
    }

    // --- images ---

    @Test
    fun imageWithBlankAltProducesNullAlt() {
        val result = parser.parse("![   ](https://example.com/img.png)")
        val image = result.blocks.single() as OrcaBlock.Image
        assertNull(image.alt)
    }

    @Test
    fun imageWithTitlePreservesTitle() {
        val result = parser.parse("![alt](https://example.com/img.png \"tooltip\")")
        val image = result.blocks.single() as OrcaBlock.Image
        assertEquals("tooltip", image.title)
        assertEquals("alt", image.alt)
    }

    @Test
    fun inlineImageInParagraphDoesNotPromoteToImageBlock() {
        val result = parser.parse("text ![icon](https://example.com/icon.png) more")
        val paragraph = result.blocks.single() as OrcaBlock.Paragraph
        assertTrue(paragraph.content.any { it is OrcaInline.Image })
    }

    // --- html inline ---

    @Test
    fun htmlInlineNodePreservesRawHtml() {
        val result = parser.parse("A <strong>bold</strong> word")
        val paragraph = result.blocks.single() as OrcaBlock.Paragraph
        val htmlNodes = paragraph.content.filterIsInstance<OrcaInline.HtmlInline>()
        assertTrue(htmlNodes.any { it.html == "<strong>" })
        assertTrue(htmlNodes.any { it.html == "</strong>" })
    }

    // --- multiple inline footnotes ---

    @Test
    fun multipleInlineFootnotesGetDistinctLabels() {
        val result = parser.parse("First ^[one] and second ^[two].")
        assertEquals(2, result.blocks.size)
        val paragraph = result.blocks[0] as OrcaBlock.Paragraph
        val refs = paragraph.content.filterIsInstance<OrcaInline.FootnoteReference>()
        assertEquals(2, refs.size)
        assertTrue(refs[0].label != refs[1].label)

        val footnotes = result.blocks[1] as OrcaBlock.Footnotes
        assertEquals(2, footnotes.definitions.size)
        assertEquals(refs[0].label, footnotes.definitions[0].label)
        assertEquals(refs[1].label, footnotes.definitions[1].label)
    }

    @Test
    fun inlineFootnoteWithFormattingInContent() {
        val result = parser.parse("Note ^[**bold** content].")
        assertEquals(2, result.blocks.size)
        val footnotes = result.blocks[1] as OrcaBlock.Footnotes
        val definition = footnotes.definitions.single()
        val paragraph = definition.blocks.single() as OrcaBlock.Paragraph
        assertTrue(paragraph.content.any { it is OrcaInline.Bold })
    }

    @Test
    fun footnoteReferenceAndInlineFootnoteBothAppearInFootnotesBlock() {
        val markdown = """
            See[^ref] and also ^[inline note].
            
            [^ref]: Reference body.
        """.trimIndent()
        val result = parser.parse(markdown)
        val footnotes = result.blocks.filterIsInstance<OrcaBlock.Footnotes>().single()
        assertEquals(2, footnotes.definitions.size)
        // [^ref] definition comes from extraction, inline footnote from mapper
        assertTrue(footnotes.definitions.any { it.label == "ref" })
        assertTrue(footnotes.definitions.any { it.label.startsWith("__inline_footnote_") })
    }

    // --- autolink ---

    @Test
    fun gfmAutolinkWithoutSchemePrepareHttpPrefix() {
        val result = parser.parse("Visit www.example.com today")
        val paragraph = result.blocks.single() as OrcaBlock.Paragraph
        val link = paragraph.content.filterIsInstance<OrcaInline.Link>().single()
        assertTrue(link.destination.startsWith("http"))
        assertEquals("www.example.com", link.content.filterIsInstance<OrcaInline.Text>().single().text)
    }

    // --- code fence edge cases ---

    @Test
    fun codeFenceWithIndentedFenceMarkerPreservesContent() {
        val markdown = "   ```\n   code\n   ```"
        val result = parser.parse(markdown)
        val codeBlock = result.blocks.filterIsInstance<OrcaBlock.CodeBlock>().firstOrNull()
        assertTrue(codeBlock != null)
    }

    @Test
    fun codeFenceWithMultipleLineCodePreservesNewlines() {
        val markdown = "```\nline1\nline2\nline3\n```"
        val result = parser.parse(markdown)
        val code = result.blocks.single() as OrcaBlock.CodeBlock
        assertEquals("line1\nline2\nline3", code.code)
    }

    // --- heading edge cases ---

    @Test
    fun headingLevelSixIsMaximum() {
        val result = parser.parse("###### Level 6")
        val heading = result.blocks.single() as OrcaBlock.Heading
        assertEquals(6, heading.level)
    }

    @Test
    fun headingWithInlineFormattingPreservesContent() {
        val result = parser.parse("## Hello **world**")
        val heading = result.blocks.single() as OrcaBlock.Heading
        assertEquals(2, heading.level)
        assertTrue(heading.content.any { it is OrcaInline.Bold })
    }

    // --- paragraph soft wrap ---

    @Test
    fun softLineBreakInParagraphProducesNewlineInline() {
        val result = parser.parse("line one\nline two")
        val paragraph = result.blocks.single() as OrcaBlock.Paragraph
        val newlines = paragraph.content.filterIsInstance<OrcaInline.Text>().filter { it.text == "\n" }
        assertEquals(1, newlines.size)
    }

    // --- block quote edge cases ---

    @Test
    fun emptyBlockQuoteProducesEmptyQuote() {
        val result = parser.parse(">")
        val quote = result.blocks.filterIsInstance<OrcaBlock.Quote>().firstOrNull()
        assertTrue(quote != null)
    }

    @Test
    fun nestedBlockQuoteProducesNestedQuote() {
        val result = parser.parse("> > nested")
        val outer = result.blocks.single() as OrcaBlock.Quote
        val inner = outer.blocks.filterIsInstance<OrcaBlock.Quote>().single()
        assertTrue(inner.blocks.isNotEmpty())
    }

    // --- list edge cases ---

    @Test
    fun unorderedListWithAsteriskBullets() {
        val result = parser.parse("* alpha\n* beta")
        val list = result.blocks.single() as OrcaBlock.ListBlock
        assertEquals(false, list.ordered)
        assertEquals(2, list.items.size)
    }

    @Test
    fun orderedListStartNumberIsPreserved() {
        val result = parser.parse("3. first\n4. second\n5. third")
        val list = result.blocks.single() as OrcaBlock.ListBlock
        assertEquals(true, list.ordered)
        assertEquals(3, list.startNumber)
        assertEquals(3, list.items.size)
    }

    @Test
    fun listItemWithMultipleParagraphsPreservesBlocks() {
        val markdown = """
            - first paragraph
            
              second paragraph
        """.trimIndent()
        val result = parser.parse(markdown)
        val list = result.blocks.single() as OrcaBlock.ListBlock
        assertEquals(1, list.items.size)
        assertEquals(2, list.items[0].blocks.size)
    }
}
