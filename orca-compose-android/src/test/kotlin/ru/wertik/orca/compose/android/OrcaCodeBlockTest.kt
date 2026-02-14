package ru.wertik.orca.compose.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test

class OrcaCodeBlockTest {

    @Test
    fun `code language label trims value`() {
        assertEquals("kotlin", codeLanguageLabel(" kotlin "))
    }

    @Test
    fun `code language label returns null for blank value`() {
        assertNull(codeLanguageLabel("   "))
        assertNull(codeLanguageLabel(null))
    }

    @Test
    fun `code line numbers are rendered for multiline code`() {
        assertEquals(
            "1\n2\n3",
            codeLineNumbersText("first\nsecond\nthird"),
        )
    }

    @Test
    fun `code line numbers are hidden for single line code`() {
        assertNull(codeLineNumbersText("single line"))
        assertNull(codeLineNumbersText(""))
    }

    @Test
    fun `syntax highlight marks keyword and comment`() {
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
    fun `html block fallback strips tags and keeps line breaks`() {
        assertEquals(
            "hello\nworld",
            htmlBlockFallbackText("<p>hello</p><div>world</div>"),
        )
    }
}
