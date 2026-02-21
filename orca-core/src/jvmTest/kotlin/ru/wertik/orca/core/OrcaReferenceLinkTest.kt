package ru.wertik.orca.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class OrcaReferenceLinkTest {

    private val parser = OrcaMarkdownParser()

    @Test
    fun `full reference link`() {
        val markdown = """
            |[link text][ref]
            |
            |[ref]: https://example.com "Title"
        """.trimMargin()

        val doc = parser.parse(markdown)
        assertEquals(1, doc.blocks.size)
        val paragraph = assertIs<OrcaBlock.Paragraph>(doc.blocks[0])
        val link = paragraph.content.filterIsInstance<OrcaInline.Link>()
        assertEquals(1, link.size)
        assertEquals("https://example.com", link[0].destination)
        assertEquals("Title", link[0].title)
    }

    @Test
    fun `short reference link`() {
        val markdown = """
            |[example]
            |
            |[example]: https://example.com
        """.trimMargin()

        val doc = parser.parse(markdown)
        assertEquals(1, doc.blocks.size)
        val paragraph = assertIs<OrcaBlock.Paragraph>(doc.blocks[0])
        val link = paragraph.content.filterIsInstance<OrcaInline.Link>()
        assertEquals(1, link.size)
        assertEquals("https://example.com", link[0].destination)
    }

    @Test
    fun `reference image`() {
        val markdown = """
            |![alt text][img]
            |
            |[img]: https://example.com/image.png "Image Title"
        """.trimMargin()

        val doc = parser.parse(markdown)
        assertEquals(1, doc.blocks.size)
        // Standalone image should be promoted to block-level.
        val image = assertIs<OrcaBlock.Image>(doc.blocks[0])
        assertEquals("https://example.com/image.png", image.source)
        assertEquals("alt text", image.alt)
        assertEquals("Image Title", image.title)
    }

    @Test
    fun `multiple reference links`() {
        val markdown = """
            |See [Google][g] and [GitHub][gh].
            |
            |[g]: https://google.com
            |[gh]: https://github.com
        """.trimMargin()

        val doc = parser.parse(markdown)
        assertEquals(1, doc.blocks.size)
        val paragraph = assertIs<OrcaBlock.Paragraph>(doc.blocks[0])
        val links = paragraph.content.filterIsInstance<OrcaInline.Link>()
        assertEquals(2, links.size)
        assertEquals("https://google.com", links[0].destination)
        assertEquals("https://github.com", links[1].destination)
    }
}
