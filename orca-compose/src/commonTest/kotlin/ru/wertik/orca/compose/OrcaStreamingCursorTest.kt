package ru.wertik.orca.compose

import ru.wertik.orca.core.OrcaBlock
import ru.wertik.orca.core.OrcaDocument
import ru.wertik.orca.core.OrcaInline
import ru.wertik.orca.core.OrcaListItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OrcaStreamingCursorTest {

    private fun paragraph(text: String) = OrcaBlock.Paragraph(content = listOf(OrcaInline.Text(text)))

    @Test
    fun appendsCursorToLastParagraph() {
        val document = OrcaDocument(blocks = listOf(paragraph("Hello"), paragraph("World")))
        val result = document.withTrailingCursor("\u258D")

        val last = result.blocks.last() as OrcaBlock.Paragraph
        assertEquals(listOf(OrcaInline.Text("World"), OrcaInline.Text("\u258D")), last.content)
        assertEquals(paragraph("Hello"), result.blocks.first())
    }

    @Test
    fun appendsCursorInsideCodeBlock() {
        val document = OrcaDocument(blocks = listOf(OrcaBlock.CodeBlock(code = "val x = 1", language = "kotlin")))
        val result = document.withTrailingCursor("\u258D")

        assertEquals("val x = 1\u258D", (result.blocks.single() as OrcaBlock.CodeBlock).code)
    }

    @Test
    fun appendsCursorToLastListItem() {
        val document = OrcaDocument(
            blocks = listOf(
                OrcaBlock.ListBlock(
                    ordered = false,
                    items = listOf(
                        OrcaListItem(blocks = listOf(paragraph("one"))),
                        OrcaListItem(blocks = listOf(paragraph("two"))),
                    ),
                ),
            ),
        )
        val result = document.withTrailingCursor("_")

        val list = result.blocks.single() as OrcaBlock.ListBlock
        val lastItemParagraph = list.items.last().blocks.single() as OrcaBlock.Paragraph
        assertEquals(listOf(OrcaInline.Text("two"), OrcaInline.Text("_")), lastItemParagraph.content)
        assertEquals(paragraph("one"), list.items.first().blocks.single())
    }

    @Test
    fun appendsCursorInsideQuoteAndAdmonition() {
        val quote = OrcaBlock.Quote(blocks = listOf(paragraph("quoted")))
        val result = OrcaDocument(blocks = listOf(quote)).withTrailingCursor("|")
        val updatedQuote = result.blocks.single() as OrcaBlock.Quote
        assertEquals(
            listOf(OrcaInline.Text("quoted"), OrcaInline.Text("|")),
            (updatedQuote.blocks.single() as OrcaBlock.Paragraph).content,
        )
    }

    @Test
    fun unsupportedTrailingBlockGetsCursorParagraph() {
        val document = OrcaDocument(blocks = listOf(OrcaBlock.ThematicBreak))
        val result = document.withTrailingCursor("\u258D")

        assertEquals(2, result.blocks.size)
        assertEquals(paragraph("\u258D"), result.blocks.last())
    }

    @Test
    fun emptyDocumentShowsCursorParagraph() {
        val result = OrcaDocument(blocks = emptyList()).withTrailingCursor("\u258D")
        assertEquals(listOf(paragraph("\u258D")), result.blocks)
    }

    @Test
    fun emptyCursorIsANoOp() {
        val document = OrcaDocument(blocks = listOf(paragraph("Hello")))
        assertTrue(document === document.withTrailingCursor(""))
    }
}
