package ru.wertik.orca.compose

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OrcaCodeBlockTest {

    @Test
    fun codeLanguageLabelTrimsValue() {
        assertEquals("kotlin", codeLanguageLabel(" kotlin "))
    }

    @Test
    fun codeLanguageLabelReturnsNullForBlankValue() {
        assertNull(codeLanguageLabel("   "))
        assertNull(codeLanguageLabel(null))
    }

    @Test
    fun codeLineNumbersAreRenderedForMultilineCode() {
        assertEquals(
            "1\n2\n3",
            codeLineNumbersText("first\nsecond\nthird"),
        )
    }

    @Test
    fun codeLineNumbersAreHiddenForSingleLineCode() {
        assertNull(codeLineNumbersText("single line"))
        assertNull(codeLineNumbersText(""))
    }

    @Test
    fun syntaxHighlightMarksKeywordAndComment() {
        val style = OrcaStyle()
        val highlighted = buildCodeAnnotatedString(
            code = "val x = 1 // comment",
            language = "kotlin",
            style = style,
        )

        assertEquals("val x = 1 // comment", highlighted.text)
        assertTrue(highlighted.spanStyles.any { span -> span.item.color == style.code.highlightKeyword.color })
        assertTrue(highlighted.spanStyles.any { span -> span.item.color == style.code.highlightComment.color })
    }

    @Test
    fun htmlBlockFallbackStripTagsAndKeepsLineBreaks() {
        assertEquals(
            "hello\nworld",
            htmlBlockFallbackText("<p>hello</p><div>world</div>"),
        )
    }

    @Test
    fun syntaxHighlightMarksStringLiteral() {
        val style = OrcaStyle()
        val highlighted = buildCodeAnnotatedString(
            code = """val msg = "hello world"""",
            language = "kotlin",
            style = style,
        )

        assertTrue(highlighted.spanStyles.any { span -> span.item.color == style.code.highlightString.color })
    }

    @Test
    fun syntaxHighlightMarksNumberLiteral() {
        val style = OrcaStyle()
        val highlighted = buildCodeAnnotatedString(
            code = "val x = 42",
            language = "kotlin",
            style = style,
        )

        assertTrue(highlighted.spanStyles.any { span -> span.item.color == style.code.highlightNumber.color })
    }

    @Test
    fun syntaxHighlightDisabledReturnsPlainAnnotatedString() {
        val style = OrcaStyle(
            code = OrcaCodeBlockStyle(syntaxHighlightingEnabled = false),
        )
        val highlighted = buildCodeAnnotatedString(
            code = "val x = 1 // comment",
            language = "kotlin",
            style = style,
        )

        assertEquals("val x = 1 // comment", highlighted.text)
        assertTrue(highlighted.spanStyles.isEmpty())
    }

    @Test
    fun syntaxHighlightUnknownLanguageProducesNoHighlights() {
        val style = OrcaStyle()
        val highlighted = buildCodeAnnotatedString(
            code = "some code here",
            language = "brainfuck",
            style = style,
        )

        assertEquals("some code here", highlighted.text)
        assertTrue(highlighted.spanStyles.isEmpty())
    }

    @Test
    fun syntaxHighlightNullLanguageProducesNoHighlights() {
        val style = OrcaStyle()
        val highlighted = buildCodeAnnotatedString(
            code = "val x = 1",
            language = null,
            style = style,
        )

        assertEquals("val x = 1", highlighted.text)
        assertTrue(highlighted.spanStyles.isEmpty())
    }

    @Test
    fun syntaxHighlightEmptyCodeProducesNoHighlights() {
        val style = OrcaStyle()
        val highlighted = buildCodeAnnotatedString(
            code = "",
            language = "kotlin",
            style = style,
        )

        assertEquals("", highlighted.text)
        assertTrue(highlighted.spanStyles.isEmpty())
    }

    @Test
    fun syntaxHighlightSqlKeywordsAreMarked() {
        val style = OrcaStyle()
        val highlighted = buildCodeAnnotatedString(
            code = "SELECT * FROM users WHERE id = 1",
            language = "sql",
            style = style,
        )

        assertTrue(highlighted.spanStyles.any { span -> span.item.color == style.code.highlightKeyword.color })
    }

    @Test
    fun syntaxHighlightJavascriptKeywordsAreMarked() {
        val style = OrcaStyle()
        val highlighted = buildCodeAnnotatedString(
            code = "const x = async function() { return null; }",
            language = "javascript",
            style = style,
        )

        assertTrue(highlighted.spanStyles.any { span -> span.item.color == style.code.highlightKeyword.color })
    }

    @Test
    fun syntaxHighlightHashCommentForBashIsMarked() {
        val style = OrcaStyle()
        val highlighted = buildCodeAnnotatedString(
            code = "echo hello # this is a comment",
            language = "bash",
            style = style,
        )

        assertTrue(highlighted.spanStyles.any { span -> span.item.color == style.code.highlightComment.color })
    }

    @Test
    fun syntaxHighlightSpansDoNotOverlap() {
        val style = OrcaStyle()
        val highlighted = buildCodeAnnotatedString(
            code = """fun greet() { val msg = "hello" // say hi""",
            language = "kotlin",
            style = style,
        )

        val spans = highlighted.spanStyles.sortedBy { it.start }
        for (i in 0 until spans.size - 1) {
            val current = spans[i]
            val next = spans[i + 1]
            assertTrue(
                current.end <= next.start,
                "spans overlap: [${current.start},${current.end}) and [${next.start},${next.end})",
            )
        }
    }
}
