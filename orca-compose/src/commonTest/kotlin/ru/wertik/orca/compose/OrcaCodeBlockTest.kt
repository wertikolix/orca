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
}
