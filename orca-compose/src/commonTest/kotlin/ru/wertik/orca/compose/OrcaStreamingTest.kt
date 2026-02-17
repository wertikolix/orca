package ru.wertik.orca.compose

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import ru.wertik.orca.core.OrcaBlock
import ru.wertik.orca.core.OrcaInline

class OrcaStreamingTest {

    @Test
    fun keysAreStableWhenLastBlockGrowsDuringStreaming() {
        // Simulate streaming: first 2 blocks exist, then last paragraph grows
        val blocks1 = listOf(
            OrcaBlock.Heading(level = 1, content = listOf(OrcaInline.Text("Title"))),
            OrcaBlock.Paragraph(content = listOf(OrcaInline.Text("Hello"))),
        )
        val blocks2 = listOf(
            OrcaBlock.Heading(level = 1, content = listOf(OrcaInline.Text("Title"))),
            OrcaBlock.Paragraph(content = listOf(OrcaInline.Text("Hello world"))),
        )

        val keys1 = buildRenderBlocks(blocks1).map { it.key }
        val keys2 = buildRenderBlocks(blocks2).map { it.key }

        // Heading key should be stable
        assertEquals(keys1[0], keys2[0])
        // Paragraph key should change because content changed
        assertNotEquals(keys1[1], keys2[1])
    }

    @Test
    fun keysAreStableWhenNewBlockAppendedDuringStreaming() {
        // Simulate streaming: new block appears at the end
        val blocks1 = listOf(
            OrcaBlock.Heading(level = 1, content = listOf(OrcaInline.Text("Title"))),
            OrcaBlock.Paragraph(content = listOf(OrcaInline.Text("First paragraph"))),
        )
        val blocks2 = listOf(
            OrcaBlock.Heading(level = 1, content = listOf(OrcaInline.Text("Title"))),
            OrcaBlock.Paragraph(content = listOf(OrcaInline.Text("First paragraph"))),
            OrcaBlock.Paragraph(content = listOf(OrcaInline.Text("Second paragraph"))),
        )

        val keys1 = buildRenderBlocks(blocks1).map { it.key }
        val keys2 = buildRenderBlocks(blocks2).map { it.key }

        // Existing blocks should keep their keys
        assertEquals(keys1[0], keys2[0])
        assertEquals(keys1[1], keys2[1])
        // New block gets a new key
        assertEquals(3, keys2.size)
    }

    @Test
    fun keysAreStableWhenCodeBlockAppendedAfterParagraph() {
        val blocks1 = listOf(
            OrcaBlock.Paragraph(content = listOf(OrcaInline.Text("Some text"))),
        )
        val blocks2 = listOf(
            OrcaBlock.Paragraph(content = listOf(OrcaInline.Text("Some text"))),
            OrcaBlock.CodeBlock(code = "val x = 1", language = "kotlin"),
        )

        val keys1 = buildRenderBlocks(blocks1).map { it.key }
        val keys2 = buildRenderBlocks(blocks2).map { it.key }

        assertEquals(keys1[0], keys2[0])
        assertEquals(2, keys2.size)
    }

    @Test
    fun duplicateBlocksGetUniqueKeysInStreamingContext() {
        val blocks = listOf(
            OrcaBlock.Paragraph(content = listOf(OrcaInline.Text("same"))),
            OrcaBlock.Paragraph(content = listOf(OrcaInline.Text("same"))),
            OrcaBlock.Paragraph(content = listOf(OrcaInline.Text("same"))),
        )

        val keys = buildRenderBlocks(blocks).map { it.key }
        assertEquals(3, keys.toSet().size)
    }

    @Test
    fun thematicBreakKeysAreStable() {
        val blocks1 = listOf(
            OrcaBlock.Paragraph(content = listOf(OrcaInline.Text("before"))),
            OrcaBlock.ThematicBreak,
            OrcaBlock.Paragraph(content = listOf(OrcaInline.Text("after"))),
        )
        val blocks2 = listOf(
            OrcaBlock.Paragraph(content = listOf(OrcaInline.Text("before"))),
            OrcaBlock.ThematicBreak,
            OrcaBlock.Paragraph(content = listOf(OrcaInline.Text("after"))),
        )

        val keys1 = buildRenderBlocks(blocks1).map { it.key }
        val keys2 = buildRenderBlocks(blocks2).map { it.key }

        assertEquals(keys1, keys2)
    }

    @Test
    fun mixedBlockTypesProduceUniqueKeys() {
        val blocks = listOf(
            OrcaBlock.Heading(level = 1, content = listOf(OrcaInline.Text("Title"))),
            OrcaBlock.Paragraph(content = listOf(OrcaInline.Text("text"))),
            OrcaBlock.CodeBlock(code = "code", language = null),
            OrcaBlock.ThematicBreak,
            OrcaBlock.Quote(blocks = listOf(
                OrcaBlock.Paragraph(content = listOf(OrcaInline.Text("quoted")))
            )),
        )

        val keys = buildRenderBlocks(blocks).map { it.key }
        assertEquals(5, keys.toSet().size)
    }
}
