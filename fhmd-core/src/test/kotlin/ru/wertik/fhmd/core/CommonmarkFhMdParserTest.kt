package ru.wertik.fhmd.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CommonmarkFhMdParserTest {

    private val parser: FhMdParser = CommonmarkFhMdParser()

    @Test
    fun `parse heading and inline formatting`() {
        val markdown = """
            # Title
            
            This is **bold** and *italic* and ~~strike~~ and `code` and [link](https://example.com).
        """.trimIndent()

        val result = parser.parse(markdown)

        val expected = FhMdDocument(
            blocks = listOf(
                FhMdBlock.Heading(
                    level = 1,
                    content = listOf(FhMdInline.Text("Title")),
                ),
                FhMdBlock.Paragraph(
                    content = listOf(
                        FhMdInline.Text("This is "),
                        FhMdInline.Bold(content = listOf(FhMdInline.Text("bold"))),
                        FhMdInline.Text(" and "),
                        FhMdInline.Italic(content = listOf(FhMdInline.Text("italic"))),
                        FhMdInline.Text(" and "),
                        FhMdInline.Strikethrough(content = listOf(FhMdInline.Text("strike"))),
                        FhMdInline.Text(" and "),
                        FhMdInline.InlineCode(code = "code"),
                        FhMdInline.Text(" and "),
                        FhMdInline.Link(
                            destination = "https://example.com",
                            content = listOf(FhMdInline.Text("link")),
                        ),
                        FhMdInline.Text("."),
                    ),
                ),
            ),
        )

        assertEquals(expected, result)
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

        val expected = FhMdDocument(
            blocks = listOf(
                FhMdBlock.Quote(
                    blocks = listOf(
                        FhMdBlock.Paragraph(
                            content = listOf(
                                FhMdInline.Text("quoted "),
                                FhMdInline.Bold(content = listOf(FhMdInline.Text("text"))),
                            ),
                        ),
                    ),
                ),
                FhMdBlock.ListBlock(
                    ordered = false,
                    items = listOf(
                        FhMdListItem(
                            blocks = listOf(
                                FhMdBlock.Paragraph(content = listOf(FhMdInline.Text("item one"))),
                            ),
                        ),
                        FhMdListItem(
                            blocks = listOf(
                                FhMdBlock.Paragraph(content = listOf(FhMdInline.Text("item two"))),
                            ),
                        ),
                    ),
                ),
                FhMdBlock.CodeBlock(
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

        val expected = FhMdDocument(
            blocks = listOf(
                FhMdBlock.ListBlock(
                    ordered = true,
                    items = listOf(
                        FhMdListItem(
                            blocks = listOf(
                                FhMdBlock.Paragraph(content = listOf(FhMdInline.Text("first"))),
                            ),
                        ),
                        FhMdListItem(
                            blocks = listOf(
                                FhMdBlock.Paragraph(content = listOf(FhMdInline.Text("second"))),
                            ),
                        ),
                    ),
                ),
                FhMdBlock.Paragraph(
                    content = listOf(
                        FhMdInline.Text("line one"),
                        FhMdInline.Text("\n"),
                        FhMdInline.Text("line two"),
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
        val list = result.blocks.single() as FhMdBlock.ListBlock

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
        val list = result.blocks.single() as FhMdBlock.ListBlock

        assertEquals(false, list.ordered)
        assertEquals(3, list.items.size)
        assertEquals(FhMdTaskState.CHECKED, list.items[0].taskState)
        assertEquals(FhMdTaskState.UNCHECKED, list.items[1].taskState)
        assertEquals(null, list.items[2].taskState)
        assertEquals(
            FhMdBlock.Paragraph(content = listOf(FhMdInline.Text("done"))),
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

        val code = result.blocks.single() as FhMdBlock.CodeBlock
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
        val code = result.blocks.single() as FhMdBlock.CodeBlock

        assertNull(code.language)
        assertEquals("plain", code.code)
    }

    @Test
    fun `parse hard line break maps to newline inline`() {
        val markdown = "line one  \nline two"
        val result = parser.parse(markdown)
        val paragraph = result.blocks.single() as FhMdBlock.Paragraph

        assertEquals(
            listOf(
                FhMdInline.Text("line one"),
                FhMdInline.Text("\n"),
                FhMdInline.Text("line two"),
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
        val quote = result.blocks.single() as FhMdBlock.Quote
        val list = quote.blocks.single() as FhMdBlock.ListBlock

        assertEquals(false, list.ordered)
        assertEquals(2, list.items.size)
        assertEquals(
            FhMdBlock.Paragraph(content = listOf(FhMdInline.Text("first"))),
            list.items[0].blocks.single(),
        )
        assertEquals(
            FhMdBlock.Paragraph(content = listOf(FhMdInline.Text("second"))),
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
    fun `parse thematic break between paragraphs`() {
        val markdown = """
            top
            
            ---
            
            bottom
        """.trimIndent()

        val result = parser.parse(markdown)

        assertEquals(3, result.blocks.size)
        assertEquals(
            FhMdBlock.Paragraph(content = listOf(FhMdInline.Text("top"))),
            result.blocks[0],
        )
        assertEquals(
            FhMdBlock.ThematicBreak,
            result.blocks[1],
        )
        assertEquals(
            FhMdBlock.Paragraph(content = listOf(FhMdInline.Text("bottom"))),
            result.blocks[2],
        )
    }

    @Test
    fun `parse standalone image paragraph as image block`() {
        val markdown = "![alt text](https://example.com/img.png \"logo\")"

        val result = parser.parse(markdown)

        assertEquals(
            listOf(
                FhMdBlock.Image(
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
        val paragraph = result.blocks.single() as FhMdBlock.Paragraph

        assertEquals(
            listOf(
                FhMdInline.Text("before "),
                FhMdInline.Image(
                    source = "https://example.com/icon.png",
                    alt = "icon",
                    title = null,
                ),
                FhMdInline.Text(" after"),
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
        val table = result.blocks.single() as FhMdBlock.Table

        assertEquals(
            listOf(
                FhMdTableCell(
                    content = listOf(FhMdInline.Text("left")),
                    alignment = FhMdTableAlignment.LEFT,
                ),
                FhMdTableCell(
                    content = listOf(FhMdInline.Text("center")),
                    alignment = FhMdTableAlignment.CENTER,
                ),
                FhMdTableCell(
                    content = listOf(FhMdInline.Text("right")),
                    alignment = FhMdTableAlignment.RIGHT,
                ),
            ),
            table.header,
        )

        assertEquals(
            listOf(
                listOf(
                    FhMdTableCell(
                        content = listOf(FhMdInline.Text("a")),
                        alignment = FhMdTableAlignment.LEFT,
                    ),
                    FhMdTableCell(
                        content = listOf(FhMdInline.Text("b")),
                        alignment = FhMdTableAlignment.CENTER,
                    ),
                    FhMdTableCell(
                        content = listOf(FhMdInline.Text("c")),
                        alignment = FhMdTableAlignment.RIGHT,
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
        val table = result.blocks.single() as FhMdBlock.Table

        assertEquals(
            FhMdTableCell(
                content = listOf(
                    FhMdInline.Bold(content = listOf(FhMdInline.Text("bold"))),
                ),
                alignment = FhMdTableAlignment.LEFT,
            ),
            table.rows[0][0],
        )
        assertEquals(
            FhMdTableCell(
                content = listOf(
                    FhMdInline.Link(
                        destination = "https://example.com",
                        content = listOf(FhMdInline.Text("docs")),
                    ),
                ),
                alignment = FhMdTableAlignment.LEFT,
            ),
            table.rows[0][1],
        )
    }
}
