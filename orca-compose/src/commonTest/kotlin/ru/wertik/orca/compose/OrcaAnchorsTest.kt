package ru.wertik.orca.compose

import ru.wertik.orca.core.OrcaMarkdownParser
import ru.wertik.orca.core.tableOfContents
import kotlin.test.Test
import kotlin.test.assertEquals

class OrcaAnchorsTest {

    private val parser = OrcaMarkdownParser()

    @Test
    fun mapsHeadingIdsToTopLevelBlockIndices() {
        val document = parser.parse("intro\n\n# First\n\ntext\n\n## Second\n\nmore\n\n## Second")
        val index = orcaHeadingBlockIndex(document)

        assertEquals(1, index["first"])
        assertEquals(3, index["second"])
        assertEquals(5, index["second-1"])
    }

    @Test
    fun tableOfContentsAlignsWithAnchorIndex() {
        val document = parser.parse("# Title\n\n## Section ==A==\n\n### Deep")
        val toc = document.tableOfContents(maxLevel = 2)
        val index = orcaHeadingBlockIndex(document)

        assertEquals(2, toc.size)
        assertEquals("Title", toc[0].title)
        assertEquals("Section A", toc[1].title)
        toc.forEach { entry ->
            assertEquals(true, index.containsKey(entry.id))
        }
    }
}
