package ru.wertik.orca.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OrcaStreamingParserTest {

    private val parser = OrcaMarkdownParser()

    @Test
    fun incrementalTokensProduceValidDocuments() {
        // Simulate LLM streaming: tokens arrive one by one
        val tokens = listOf("# ", "Hello", " World", "\n\n", "This ", "is ", "a ", "test.")
        var markdown = ""

        for (token in tokens) {
            markdown += token
            val doc = parser.parse(markdown)
            assertTrue(doc.blocks.isNotEmpty() || markdown.isBlank())
        }
    }

    @Test
    fun streamingParagraphGrowsCorrectly() {
        val doc1 = parser.parse("Hello")
        assertEquals(1, doc1.blocks.size)
        assertTrue(doc1.blocks[0] is OrcaBlock.Paragraph)

        val doc2 = parser.parse("Hello world")
        assertEquals(1, doc2.blocks.size)

        val doc3 = parser.parse("Hello world\n\nSecond paragraph")
        assertEquals(2, doc3.blocks.size)
    }

    @Test
    fun streamingCodeBlockPartiallyFormed() {
        // Code fence opened but not closed yet
        val doc1 = parser.parse("```kotlin\nval x = 1")
        assertTrue(doc1.blocks.isNotEmpty())

        // Code fence closed
        val doc2 = parser.parse("```kotlin\nval x = 1\n```")
        val codeBlock = doc2.blocks.filterIsInstance<OrcaBlock.CodeBlock>()
        assertEquals(1, codeBlock.size)
        assertEquals("kotlin", codeBlock[0].language)
    }

    @Test
    fun streamingHeadingThenParagraph() {
        val doc1 = parser.parse("# Title")
        assertEquals(1, doc1.blocks.size)
        assertTrue(doc1.blocks[0] is OrcaBlock.Heading)

        val doc2 = parser.parse("# Title\n\nBody text")
        assertEquals(2, doc2.blocks.size)
        assertTrue(doc2.blocks[0] is OrcaBlock.Heading)
        assertTrue(doc2.blocks[1] is OrcaBlock.Paragraph)
    }

    @Test
    fun streamingListGrows() {
        val doc1 = parser.parse("- item 1")
        assertEquals(1, doc1.blocks.size)

        val doc2 = parser.parse("- item 1\n- item 2")
        val list = doc2.blocks.filterIsInstance<OrcaBlock.ListBlock>()
        assertEquals(1, list.size)
        assertEquals(2, list[0].items.size)

        val doc3 = parser.parse("- item 1\n- item 2\n- item 3")
        val list3 = doc3.blocks.filterIsInstance<OrcaBlock.ListBlock>()
        assertEquals(3, list3[0].items.size)
    }

    @Test
    fun cachedParseReturnsSameDocumentForSameInput() {
        val key = "test-key"
        val markdown = "# Hello\n\nWorld"

        val doc1 = parser.parseCached(key, markdown)
        val doc2 = parser.parseCached(key, markdown)

        assertEquals(doc1, doc2)
    }

    @Test
    fun cachedParseUpdatesOnInputChange() {
        val key = "streaming-key"

        val doc1 = parser.parseCached(key, "Hello")
        assertEquals(1, doc1.blocks.size)

        val doc2 = parser.parseCached(key, "Hello\n\nWorld")
        assertEquals(2, doc2.blocks.size)
    }

    @Test
    fun parseWithDiagnosticsNeverThrows() {
        val inputs = listOf(
            "",
            "   ",
            "\n\n\n",
            "```",
            "# ",
            "[broken link(",
            "| broken | table",
            "> > > deeply nested",
        )

        for (input in inputs) {
            val result = parser.parseWithDiagnostics(input)
            // Should never throw, always return a result
            assertTrue(result.document.blocks.size >= 0)
        }
    }
}
