package ru.wertik.orca.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class OrcaDefinitionListTest {

    private val parser = OrcaMarkdownParser()

    @Test
    fun `simple definition list`() {
        val markdown = """
            |Term
            |: Definition one
        """.trimMargin()

        val doc = parser.parse(markdown)
        assertEquals(1, doc.blocks.size)
        val dl = assertIs<OrcaBlock.DefinitionList>(doc.blocks[0])
        assertEquals(1, dl.items.size)
        assertEquals("Term", dl.items[0].term.toPlainText())
        assertEquals(1, dl.items[0].definitions.size)
    }

    @Test
    fun `multiple definitions for one term`() {
        val markdown = """
            |Term
            |: Definition one
            |: Definition two
            |: Definition three
        """.trimMargin()

        val doc = parser.parse(markdown)
        assertEquals(1, doc.blocks.size)
        val dl = assertIs<OrcaBlock.DefinitionList>(doc.blocks[0])
        assertEquals(1, dl.items.size)
        assertEquals(3, dl.items[0].definitions.size)
    }

    @Test
    fun `multiple terms with definitions`() {
        val markdown = """
            |Apple
            |: A fruit
            |
            |Banana
            |: Another fruit
        """.trimMargin()

        val doc = parser.parse(markdown)
        // May parse as one or two definition lists depending on blank line handling.
        val defLists = doc.blocks.filterIsInstance<OrcaBlock.DefinitionList>()
        assertTrue(defLists.isNotEmpty())
        val allItems = defLists.flatMap { it.items }
        assertEquals(2, allItems.size)
        assertEquals("Apple", allItems[0].term.toPlainText())
        assertEquals("Banana", allItems[1].term.toPlainText())
    }

    @Test
    fun `definition with inline formatting in term`() {
        val markdown = """
            |**Bold Term**
            |: Definition content
        """.trimMargin()

        val doc = parser.parse(markdown)
        assertEquals(1, doc.blocks.size)
        val dl = assertIs<OrcaBlock.DefinitionList>(doc.blocks[0])
        assertEquals(1, dl.items.size)
        // Term should contain bold inline.
        val termInlines = dl.items[0].term
        assertTrue(termInlines.any { it is OrcaInline.Bold })
    }

    @Test
    fun `definition list mixed with regular content`() {
        val markdown = """
            |# Heading
            |
            |Some paragraph.
            |
            |Term
            |: Definition
            |
            |Another paragraph.
        """.trimMargin()

        val doc = parser.parse(markdown)
        assertTrue(doc.blocks.size >= 4)
        assertIs<OrcaBlock.Heading>(doc.blocks[0])
        assertIs<OrcaBlock.Paragraph>(doc.blocks[1])
        assertIs<OrcaBlock.DefinitionList>(doc.blocks[2])
        assertIs<OrcaBlock.Paragraph>(doc.blocks[3])
    }

    @Test
    fun `non-definition content is not affected`() {
        val markdown = """
            |Just a paragraph.
            |
            |Another paragraph.
        """.trimMargin()

        val doc = parser.parse(markdown)
        assertTrue(doc.blocks.all { it !is OrcaBlock.DefinitionList })
    }

    private fun List<OrcaInline>.toPlainText(): String {
        return buildString {
            for (inline in this@toPlainText) {
                when (inline) {
                    is OrcaInline.Text -> append(inline.text)
                    is OrcaInline.Bold -> append(inline.content.toPlainText())
                    is OrcaInline.Italic -> append(inline.content.toPlainText())
                    else -> {}
                }
            }
        }
    }
}
