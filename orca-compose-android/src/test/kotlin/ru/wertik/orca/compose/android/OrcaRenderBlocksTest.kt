package ru.wertik.orca.compose.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.wertik.orca.core.OrcaBlock
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
}
