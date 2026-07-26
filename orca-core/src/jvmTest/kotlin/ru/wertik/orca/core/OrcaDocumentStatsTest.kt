package ru.wertik.orca.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OrcaDocumentStatsTest {

    private val parser = OrcaMarkdownParser()

    @Test
    fun countsBlocksWordsAndCharacters() {
        val document = parser.parse(
            """
            # Title

            First paragraph with five words.
            """.trimIndent(),
        )

        val stats = document.stats()

        assertEquals(2, stats.blocks)
        assertEquals(1, stats.headings)
        assertEquals(1, stats.paragraphs)
        assertEquals(6, stats.words)
        assertEquals("TitleFirst paragraph with five words.".length, stats.characters)
    }

    @Test
    fun countsNestedContentThroughQuotesAndLists() {
        val document = parser.parse(
            """
            > Quoted text
            >
            > ```kotlin
            > val x = 1
            > ```

            - [x] done
            - [ ] open
            - plain item
            """.trimIndent(),
        )

        val stats = document.stats()

        assertEquals(1, stats.quotes)
        assertEquals(1, stats.codeBlocks)
        assertEquals(3, stats.listItems)
        assertEquals(2, stats.tasks)
        assertEquals(1, stats.completedTasks)
        assertEquals(1, stats.openTasks)
        assertEquals(0.5f, stats.taskProgress)
    }

    @Test
    fun countsLinksImagesTablesAndMath() {
        val document = parser.parse(
            """
            A [link](https://example.com) and ![alt](https://example.com/a.png) inline ${'$'}x^2${'$'}.

            | A | B |
            | - | - |
            | 1 | 2 |

            ${'$'}${'$'}
            E = mc^2
            ${'$'}${'$'}
            """.trimIndent(),
        )

        val stats = document.stats()

        assertEquals(1, stats.links)
        assertEquals(1, stats.images)
        assertEquals(1, stats.tables)
        assertEquals(1, stats.inlineMath)
        assertEquals(1, stats.mathBlocks)
    }

    @Test
    fun readingTimeRoundsUpAndStaysZeroForEmptyDocuments() {
        val words = (1..300).joinToString(" ") { "word" }
        val document = parser.parse(words)

        assertEquals(2, document.stats(wordsPerMinute = 220).readingMinutes)
        assertEquals(1, document.stats(wordsPerMinute = 1000).readingMinutes)
        assertEquals(0, OrcaDocument(emptyList()).stats().readingMinutes)
    }

    @Test
    fun taskProgressIsZeroWithoutTasks() {
        val stats = parser.parse("Plain paragraph.").stats()

        assertEquals(0, stats.tasks)
        assertEquals(0f, stats.taskProgress)
        assertTrue(stats.words > 0)
    }
}
