package ru.wertik.orca.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser

class OrcaMarkdownParserTest {

    private val parser: OrcaParser = OrcaMarkdownParser()

    @Test
    fun `parse heading and inline formatting`() {
        val markdown = """
            # Title
            
            This is **bold** and *italic* and ~~strike~~ and `code` and [link](https://example.com).
        """.trimIndent()

        val result = parser.parse(markdown)

        val expected = OrcaDocument(
            blocks = listOf(
                OrcaBlock.Heading(
                    level = 1,
                    content = listOf(OrcaInline.Text("Title")),
                ),
                OrcaBlock.Paragraph(
                    content = listOf(
                        OrcaInline.Text("This is "),
                        OrcaInline.Bold(content = listOf(OrcaInline.Text("bold"))),
                        OrcaInline.Text(" and "),
                        OrcaInline.Italic(content = listOf(OrcaInline.Text("italic"))),
                        OrcaInline.Text(" and "),
                        OrcaInline.Strikethrough(content = listOf(OrcaInline.Text("strike"))),
                        OrcaInline.Text(" and "),
                        OrcaInline.InlineCode(code = "code"),
                        OrcaInline.Text(" and "),
                        OrcaInline.Link(
                            destination = "https://example.com",
                            content = listOf(OrcaInline.Text("link")),
                        ),
                        OrcaInline.Text("."),
                    ),
                ),
            ),
        )

        assertEquals(expected, result)
    }

    @Test
    fun `parse bare url into link node`() {
        val markdown = "See https://example.com for docs"

        val result = parser.parse(markdown)
        val paragraph = result.blocks.single() as OrcaBlock.Paragraph
        val link = paragraph.content.filterIsInstance<OrcaInline.Link>().single()

        assertEquals("https://example.com", link.destination)
        assertEquals(listOf(OrcaInline.Text("https://example.com")), link.content)
    }

    @Test
    fun `parse quote list and code block`() {
        val markdown = """
            > quoted **text**
            
            - item one
            - item two
            
            ```kotlin
            println("hi")
            ```
        """.trimIndent()

        val result = parser.parse(markdown)

        val expected = OrcaDocument(
            blocks = listOf(
                OrcaBlock.Quote(
                    blocks = listOf(
                        OrcaBlock.Paragraph(
                            content = listOf(
                                OrcaInline.Text("quoted "),
                                OrcaInline.Bold(content = listOf(OrcaInline.Text("text"))),
                            ),
                        ),
                    ),
                ),
                OrcaBlock.ListBlock(
                    ordered = false,
                    items = listOf(
                        OrcaListItem(
                            blocks = listOf(
                                OrcaBlock.Paragraph(content = listOf(OrcaInline.Text("item one"))),
                            ),
                        ),
                        OrcaListItem(
                            blocks = listOf(
                                OrcaBlock.Paragraph(content = listOf(OrcaInline.Text("item two"))),
                            ),
                        ),
                    ),
                ),
                OrcaBlock.CodeBlock(
                    code = "println(\"hi\")",
                    language = "kotlin",
                ),
            ),
        )

        assertEquals(expected, result)
    }

    @Test
    fun `parse ordered list and inline line breaks`() {
        val markdown = """
            1. first
            2. second
            
            line one
            line two
        """.trimIndent()

        val result = parser.parse(markdown)

        val expected = OrcaDocument(
            blocks = listOf(
                OrcaBlock.ListBlock(
                    ordered = true,
                    items = listOf(
                        OrcaListItem(
                            blocks = listOf(
                                OrcaBlock.Paragraph(content = listOf(OrcaInline.Text("first"))),
                            ),
                        ),
                        OrcaListItem(
                            blocks = listOf(
                                OrcaBlock.Paragraph(content = listOf(OrcaInline.Text("second"))),
                            ),
                        ),
                    ),
                ),
                OrcaBlock.Paragraph(
                    content = listOf(
                        OrcaInline.Text("line one"),
                        OrcaInline.Text("\n"),
                        OrcaInline.Text("line two"),
                    ),
                ),
            ),
        )

        assertEquals(expected, result)
    }

    @Test
    fun `parse ordered list preserves start number`() {
        val markdown = """
            5. first
            6. second
        """.trimIndent()

        val result = parser.parse(markdown)
        val list = result.blocks.single() as OrcaBlock.ListBlock

        assertEquals(true, list.ordered)
        assertEquals(5, list.startNumber)
        assertEquals(2, list.items.size)
    }

    @Test
    fun `parse task list items with checked state`() {
        val markdown = """
            - [x] done
            - [ ] todo
            - plain
        """.trimIndent()

        val result = parser.parse(markdown)
        val list = result.blocks.single() as OrcaBlock.ListBlock

        assertEquals(false, list.ordered)
        assertEquals(3, list.items.size)
        assertEquals(OrcaTaskState.CHECKED, list.items[0].taskState)
        assertEquals(OrcaTaskState.UNCHECKED, list.items[1].taskState)
        assertEquals(null, list.items[2].taskState)
        assertEquals(
            OrcaBlock.Paragraph(content = listOf(OrcaInline.Text("done"))),
            list.items[0].blocks.single(),
        )
    }

    @Test
    fun `parse empty input produces empty document`() {
        val result = parser.parse("")
        assertTrue(result.blocks.isEmpty())
    }

    @Test
    fun `parse fenced code info keeps only first language token`() {
        val markdown = """
            ```kotlin linenums
            val x = 1
            ```
        """.trimIndent()

        val result = parser.parse(markdown)

        val code = result.blocks.single() as OrcaBlock.CodeBlock
        assertEquals("kotlin", code.language)
        assertEquals("val x = 1", code.code)
    }

    @Test
    fun `parse fenced code without info has null language`() {
        val markdown = """
            ```
            plain
            ```
        """.trimIndent()

        val result = parser.parse(markdown)
        val code = result.blocks.single() as OrcaBlock.CodeBlock

        assertNull(code.language)
        assertEquals("plain", code.code)
    }

    @Test
    fun `parse hard line break maps to newline inline`() {
        val markdown = "line one  \nline two"
        val result = parser.parse(markdown)
        val paragraph = result.blocks.single() as OrcaBlock.Paragraph

        assertEquals(
            listOf(
                OrcaInline.Text("line one"),
                OrcaInline.Text("\n"),
                OrcaInline.Text("line two"),
            ),
            paragraph.content,
        )
    }

    @Test
    fun `parse nested list inside quote preserves hierarchy`() {
        val markdown = """
            > - first
            > - second
        """.trimIndent()

        val result = parser.parse(markdown)
        val quote = result.blocks.single() as OrcaBlock.Quote
        val list = quote.blocks.single() as OrcaBlock.ListBlock

        assertEquals(false, list.ordered)
        assertEquals(2, list.items.size)
        assertEquals(
            OrcaBlock.Paragraph(content = listOf(OrcaInline.Text("first"))),
            list.items[0].blocks.single(),
        )
        assertEquals(
            OrcaBlock.Paragraph(content = listOf(OrcaInline.Text("second"))),
            list.items[1].blocks.single(),
        )
    }

    @Test
    fun `very deep nesting does not crash mapper`() {
        val markdown = buildString {
            repeat(400) { append("> ") }
            append("deep")
        }

        val result = parser.parse(markdown)

        assertTrue(result.blocks.isNotEmpty())
    }

    @Test
    fun `very deep nesting reports depth limit once`() {
        var callbackCount = 0
        var exceededDepth: Int? = null
        val parser = OrcaMarkdownParser(
            maxTreeDepth = 8,
            onDepthLimitExceeded = { depth ->
                callbackCount += 1
                exceededDepth = depth
            },
        )
        val markdown = buildString {
            repeat(64) { append("> ") }
            append("deep")
        }

        val result = parser.parse(markdown)

        assertTrue(result.blocks.isNotEmpty())
        assertEquals(1, callbackCount)
        assertTrue((exceededDepth ?: 0) > 8)
    }

    @Test
    fun `parseWithDiagnostics reports depth limit warning`() {
        val parser = OrcaMarkdownParser(maxTreeDepth = 8)
        val markdown = buildString {
            repeat(64) { append("> ") }
            append("deep")
        }

        val result = parser.parseWithDiagnostics(markdown)

        assertTrue(result.document.blocks.isNotEmpty())
        val warning = result.diagnostics.warnings.single() as OrcaParseWarning.DepthLimitExceeded
        assertEquals(8, warning.maxTreeDepth)
        assertTrue(warning.exceededDepth > 8)
        assertTrue(result.diagnostics.errors.isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parser requires positive max tree depth`() {
        OrcaMarkdownParser(maxTreeDepth = 0)
    }

    @Test
    fun `cache key includes parser identity and max tree depth`() {
        val parserA = MarkdownParser(GFMFlavourDescriptor())
        val parserB = MarkdownParser(GFMFlavourDescriptor())
        val orcaA = OrcaMarkdownParser(parser = parserA, maxTreeDepth = 64)
        val orcaB = OrcaMarkdownParser(parser = parserB, maxTreeDepth = 64)
        val orcaC = OrcaMarkdownParser(parser = parserA, maxTreeDepth = 16)

        assertTrue(orcaA.cacheKey() != orcaB.cacheKey())
        assertTrue(orcaA.cacheKey() != orcaC.cacheKey())
    }

    @Test
    fun `parseCached reuses document for same key and input`() {
        val parser = OrcaMarkdownParser()
        val key = "message-1"
        val markdown = "# Title"

        val first = parser.parseCached(
            key = key,
            input = markdown,
        )
        val second = parser.parseCached(
            key = key,
            input = markdown,
        )

        assertSame(first, second)
    }

    @Test
    fun `parseCached invalidates entry when input changes`() {
        val parser = OrcaMarkdownParser()
        val key = "message-1"

        val first = parser.parseCached(
            key = key,
            input = "# First",
        )
        val second = parser.parseCached(
            key = key,
            input = "# Second",
        )

        assertFalse(first === second)
        assertEquals(
            OrcaBlock.Heading(
                level = 1,
                content = listOf(OrcaInline.Text("Second")),
            ),
            second.blocks.single(),
        )
    }

    @Test
    fun `parse thematic break between paragraphs`() {
        val markdown = """
            top
            
            ---
            
            bottom
        """.trimIndent()

        val result = parser.parse(markdown)

        assertEquals(3, result.blocks.size)
        assertEquals(
            OrcaBlock.Paragraph(content = listOf(OrcaInline.Text("top"))),
            result.blocks[0],
        )
        assertEquals(
            OrcaBlock.ThematicBreak,
            result.blocks[1],
        )
        assertEquals(
            OrcaBlock.Paragraph(content = listOf(OrcaInline.Text("bottom"))),
            result.blocks[2],
        )
    }

    @Test
    fun `parse standalone image paragraph as image block`() {
        val markdown = "![alt text](https://example.com/img.png \"logo\")"

        val result = parser.parse(markdown)

        assertEquals(
            listOf(
                OrcaBlock.Image(
                    source = "https://example.com/img.png",
                    alt = "alt text",
                    title = "logo",
                ),
            ),
            result.blocks,
        )
    }

    @Test
    fun `parse inline image in paragraph`() {
        val markdown = "before ![icon](https://example.com/icon.png) after"
        val result = parser.parse(markdown)
        val paragraph = result.blocks.single() as OrcaBlock.Paragraph

        assertEquals(
            listOf(
                OrcaInline.Text("before "),
                OrcaInline.Image(
                    source = "https://example.com/icon.png",
                    alt = "icon",
                    title = null,
                ),
                OrcaInline.Text(" after"),
            ),
            paragraph.content,
        )
    }

    @Test
    fun `parse gfm table with header body and alignment`() {
        val markdown = """
            | left | center | right |
            |:-----|:------:|------:|
            | a    | b      | c     |
        """.trimIndent()

        val result = parser.parse(markdown)
        val table = result.blocks.single() as OrcaBlock.Table

        assertEquals(
            listOf(
                OrcaTableCell(
                    content = listOf(OrcaInline.Text("left")),
                    alignment = OrcaTableAlignment.LEFT,
                ),
                OrcaTableCell(
                    content = listOf(OrcaInline.Text("center")),
                    alignment = OrcaTableAlignment.CENTER,
                ),
                OrcaTableCell(
                    content = listOf(OrcaInline.Text("right")),
                    alignment = OrcaTableAlignment.RIGHT,
                ),
            ),
            table.header,
        )

        assertEquals(
            listOf(
                listOf(
                    OrcaTableCell(
                        content = listOf(OrcaInline.Text("a")),
                        alignment = OrcaTableAlignment.LEFT,
                    ),
                    OrcaTableCell(
                        content = listOf(OrcaInline.Text("b")),
                        alignment = OrcaTableAlignment.CENTER,
                    ),
                    OrcaTableCell(
                        content = listOf(OrcaInline.Text("c")),
                        alignment = OrcaTableAlignment.RIGHT,
                    ),
                ),
            ),
            table.rows,
        )
    }

    @Test
    fun `parse table cells with inline formatting`() {
        val markdown = """
            | feature | value |
            |:--------|:------|
            | **bold** | [docs](https://example.com) |
        """.trimIndent()

        val result = parser.parse(markdown)
        val table = result.blocks.single() as OrcaBlock.Table

        assertEquals(
            OrcaTableCell(
                content = listOf(
                    OrcaInline.Bold(content = listOf(OrcaInline.Text("bold"))),
                ),
                alignment = OrcaTableAlignment.LEFT,
            ),
            table.rows[0][0],
        )
        assertEquals(
            OrcaTableCell(
                content = listOf(
                    OrcaInline.Link(
                        destination = "https://example.com",
                        content = listOf(OrcaInline.Text("docs")),
                    ),
                ),
                alignment = OrcaTableAlignment.LEFT,
            ),
            table.rows[0][1],
        )
    }

    @Test
    fun `parse footnote references and definitions`() {
        val markdown = """
            See note[^note].

            [^note]: Foot **value**
        """.trimIndent()

        val result = parser.parse(markdown)

        assertEquals(
            OrcaDocument(
                blocks = listOf(
                    OrcaBlock.Paragraph(
                        content = listOf(
                            OrcaInline.Text("See note"),
                            OrcaInline.FootnoteReference(label = "note"),
                            OrcaInline.Text("."),
                        ),
                    ),
                    OrcaBlock.Footnotes(
                        definitions = listOf(
                            OrcaFootnoteDefinition(
                                label = "note",
                                blocks = listOf(
                                    OrcaBlock.Paragraph(
                                        content = listOf(
                                            OrcaInline.Text("Foot "),
                                            OrcaInline.Bold(content = listOf(OrcaInline.Text("value"))),
                                        ),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
            result,
        )
    }

    @Test
    fun `parse inline footnote generates reference and definition`() {
        val markdown = "Inline ^[alpha *beta*] footnote."

        val result = parser.parse(markdown)

        assertEquals(2, result.blocks.size)
        val paragraph = result.blocks[0] as OrcaBlock.Paragraph
        val reference = paragraph.content.filterIsInstance<OrcaInline.FootnoteReference>().single()
        assertTrue(reference.label.startsWith("__inline_footnote_"))

        val footnotes = result.blocks[1] as OrcaBlock.Footnotes
        val definition = footnotes.definitions.single()
        assertEquals(reference.label, definition.label)

        val definitionParagraph = definition.blocks.single() as OrcaBlock.Paragraph
        assertEquals(
            listOf(
                OrcaInline.Text("alpha "),
                OrcaInline.Italic(content = listOf(OrcaInline.Text("beta"))),
            ),
            definitionParagraph.content,
        )
    }

    @Test
    fun `parse html block and inline html nodes`() {
        val markdown = """
            <div>hello <b>world</b></div>
            
            A <span>small</span> test
        """.trimIndent()

        val result = parser.parse(markdown)

        assertEquals(
            OrcaBlock.HtmlBlock("<div>hello <b>world</b></div>"),
            result.blocks[0],
        )
        val paragraph = result.blocks[1] as OrcaBlock.Paragraph
        assertEquals(
            listOf(
                OrcaInline.Text("A "),
                OrcaInline.HtmlInline("<span>"),
                OrcaInline.Text("small"),
                OrcaInline.HtmlInline("</span>"),
                OrcaInline.Text(" test"),
            ),
            paragraph.content,
        )
    }

    @Test
    fun `parse yaml front matter into document metadata`() {
        val markdown = """
            ---
            title: hello
            tags: kotlin
            ---
            
            # Heading
        """.trimIndent()

        val result = parser.parse(markdown)

        val frontMatter = result.frontMatter as OrcaFrontMatter.Yaml
        assertEquals("hello", frontMatter.entries["title"])
        assertEquals("kotlin", frontMatter.entries["tags"])
        assertEquals(
            OrcaBlock.Heading(
                level = 1,
                content = listOf(OrcaInline.Text("Heading")),
            ),
            result.blocks.single(),
        )
    }

    @Test
    fun `parse toml front matter into document metadata`() {
        val markdown = """
            +++
            title = "hello"
            draft = "false"
            +++
            
            body
        """.trimIndent()

        val result = parser.parse(markdown)

        val frontMatter = result.frontMatter as OrcaFrontMatter.Toml
        assertEquals("hello", frontMatter.entries["title"])
        assertEquals("false", frontMatter.entries["draft"])
        assertEquals(
            OrcaBlock.Paragraph(content = listOf(OrcaInline.Text("body"))),
            result.blocks.single(),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun parserRequiresPositiveCacheSize() {
        OrcaMarkdownParser(cacheSize = 0)
    }

    @Test
    fun clearCacheInvalidatesStoredEntries() {
        val parser = OrcaMarkdownParser()
        val key = "clear-test"
        val markdown = "# Hello"

        val first = parser.parseCached(key = key, input = markdown)
        parser.clearCache()
        val second = parser.parseCached(key = key, input = markdown)

        // After clearing, the second call re-parses — result is equal but not the same instance
        assertEquals(first, second)
        assertFalse(first === second)
    }

    @Test
    fun parseCachedWithDiagnosticsReturnsSameResultForSameKeyAndInput() {
        val parser = OrcaMarkdownParser()
        val key = "diag-cache-key"
        val markdown = "# Cached heading"

        val first = parser.parseCachedWithDiagnostics(key = key, input = markdown)
        val second = parser.parseCachedWithDiagnostics(key = key, input = markdown)

        assertSame(first, second)
    }

    @Test
    fun parseCachedWithDiagnosticsInvalidatesOnInputChange() {
        val parser = OrcaMarkdownParser()
        val key = "diag-change-key"

        val first = parser.parseCachedWithDiagnostics(key = key, input = "# First")
        val second = parser.parseCachedWithDiagnostics(key = key, input = "# Second")

        assertFalse(first === second)
        assertEquals(
            OrcaBlock.Heading(level = 1, content = listOf(OrcaInline.Text("Second"))),
            second.document.blocks.single(),
        )
    }

    @Test
    fun parseWhitespaceOnlyInputProducesEmptyDocument() {
        val result = parser.parse("   \n\n   ")
        assertTrue(result.blocks.isEmpty())
    }

    @Test
    fun parseSetextHeading() {
        val markdown = """
            Title
            =====
            
            Subtitle
            --------
        """.trimIndent()

        val result = parser.parse(markdown)

        assertEquals(2, result.blocks.size)
        assertEquals(
            OrcaBlock.Heading(level = 1, content = listOf(OrcaInline.Text("Title"))),
            result.blocks[0],
        )
        assertEquals(
            OrcaBlock.Heading(level = 2, content = listOf(OrcaInline.Text("Subtitle"))),
            result.blocks[1],
        )
    }

    @Test
    fun parseIndentedCodeBlock() {
        val markdown = "    val x = 1\n    val y = 2"

        val result = parser.parse(markdown)
        val code = result.blocks.single() as OrcaBlock.CodeBlock

        assertNull(code.language)
        assertEquals("val x = 1\nval y = 2", code.code)
    }

    @Test
    fun parseAutolinkAngleBracketSyntax() {
        val markdown = "Visit <https://example.com> now."

        val result = parser.parse(markdown)
        val paragraph = result.blocks.single() as OrcaBlock.Paragraph
        val link = paragraph.content.filterIsInstance<OrcaInline.Link>().single()

        assertEquals("https://example.com", link.destination)
        assertEquals(listOf(OrcaInline.Text("https://example.com")), link.content)
    }

    @Test
    fun parseFullReferenceLinkResolvesDestination() {
        val markdown = """
            See [docs][ref].
            
            [ref]: https://example.com "Title"
        """.trimIndent()

        val result = parser.parse(markdown)
        val paragraph = result.blocks.first() as OrcaBlock.Paragraph
        val link = paragraph.content.filterIsInstance<OrcaInline.Link>().single()

        assertEquals("https://example.com", link.destination)
        assertEquals(listOf(OrcaInline.Text("docs")), link.content)
    }

    @Test
    fun parseNestedOrderedList() {
        val markdown = """
            1. first
               1. nested one
               2. nested two
            2. second
        """.trimIndent()

        val result = parser.parse(markdown)
        val outer = result.blocks.single() as OrcaBlock.ListBlock

        assertTrue(outer.ordered)
        assertEquals(2, outer.items.size)

        val firstItemBlocks = outer.items[0].blocks
        val nestedList = firstItemBlocks.filterIsInstance<OrcaBlock.ListBlock>().single()
        assertTrue(nestedList.ordered)
        assertEquals(2, nestedList.items.size)
    }

    @Test
    fun parseImageWithNoAltText() {
        val markdown = "![](https://example.com/img.png)"

        val result = parser.parse(markdown)
        val image = result.blocks.single() as OrcaBlock.Image

        assertEquals("https://example.com/img.png", image.source)
        assertNull(image.alt)
        assertNull(image.title)
    }

    @Test
    fun parseMixedBoldItalicNesting() {
        val markdown = "***bold and italic***"

        val result = parser.parse(markdown)
        val paragraph = result.blocks.single() as OrcaBlock.Paragraph

        assertTrue(paragraph.content.isNotEmpty())
        // Should produce some nesting of Bold/Italic
        val hasBoldOrItalic = paragraph.content.any { it is OrcaInline.Bold || it is OrcaInline.Italic }
        assertTrue(hasBoldOrItalic)
    }
}
