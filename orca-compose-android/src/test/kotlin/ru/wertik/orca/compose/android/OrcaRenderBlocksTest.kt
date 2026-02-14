package ru.wertik.orca.compose.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.wertik.orca.core.OrcaBlock
import ru.wertik.orca.core.OrcaFootnoteDefinition
import ru.wertik.orca.core.OrcaInline

class OrcaRenderBlocksTest {

    @Test
    fun `build render blocks creates unique keys for duplicate blocks`() {
        val blocks = listOf(
            OrcaBlock.Paragraph(content = listOf(OrcaInline.Text("same"))),
            OrcaBlock.Paragraph(content = listOf(OrcaInline.Text("same"))),
            OrcaBlock.Paragraph(content = listOf(OrcaInline.Text("same"))),
        )

        val rendered = buildRenderBlocks(blocks)
        val keys = rendered.map { it.key }

        assertEquals(3, rendered.size)
        assertEquals(3, keys.toSet().size)
    }

    @Test
    fun `build render blocks produces deterministic keys for same content`() {
        val first = listOf(
            OrcaBlock.Heading(level = 1, content = listOf(OrcaInline.Text("title"))),
            OrcaBlock.Paragraph(content = listOf(OrcaInline.Text("body"))),
            OrcaBlock.Paragraph(content = listOf(OrcaInline.Text("body"))),
        )
        val second = listOf(
            OrcaBlock.Heading(level = 1, content = listOf(OrcaInline.Text("title"))),
            OrcaBlock.Paragraph(content = listOf(OrcaInline.Text("body"))),
            OrcaBlock.Paragraph(content = listOf(OrcaInline.Text("body"))),
        )

        val firstKeys = buildRenderBlocks(first).map { it.key }
        val secondKeys = buildRenderBlocks(second).map { it.key }

        assertEquals(firstKeys, secondKeys)
        assertTrue(firstKeys.isNotEmpty())
    }

    @Test
    fun `build footnote numbers follows definition order and skips duplicates`() {
        val blocks = listOf(
            OrcaBlock.Paragraph(content = listOf(OrcaInline.Text("body"))),
            OrcaBlock.Footnotes(
                definitions = listOf(
                    OrcaFootnoteDefinition(
                        label = "a",
                        blocks = listOf(OrcaBlock.Paragraph(content = listOf(OrcaInline.Text("one")))),
                    ),
                    OrcaFootnoteDefinition(
                        label = "b",
                        blocks = listOf(OrcaBlock.Paragraph(content = listOf(OrcaInline.Text("two")))),
                    ),
                ),
            ),
            OrcaBlock.Footnotes(
                definitions = listOf(
                    OrcaFootnoteDefinition(
                        label = "a",
                        blocks = listOf(OrcaBlock.Paragraph(content = listOf(OrcaInline.Text("duplicate")))),
                    ),
                    OrcaFootnoteDefinition(
                        label = "c",
                        blocks = listOf(OrcaBlock.Paragraph(content = listOf(OrcaInline.Text("three")))),
                    ),
                ),
            ),
        )

        assertEquals(
            linkedMapOf("a" to 1, "b" to 2, "c" to 3),
            buildFootnoteNumbers(blocks),
        )
    }
}
