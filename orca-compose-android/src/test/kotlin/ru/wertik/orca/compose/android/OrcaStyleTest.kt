package ru.wertik.orca.compose.android

import org.junit.Assert.assertEquals
import org.junit.Test

class OrcaStyleTest {

    @Test
    fun `typography heading levels are clamped to supported range`() {
        val style = OrcaStyle()

        assertEquals(style.typography.heading1, style.heading(-10))
        assertEquals(style.typography.heading1, style.heading(1))
        assertEquals(style.typography.heading4, style.heading(4))
        assertEquals(style.typography.heading6, style.heading(6))
        assertEquals(style.typography.heading6, style.heading(42))
    }

    @Test
    fun `compatibility accessors still mirror grouped styles`() {
        val style = OrcaStyle()

        assertEquals(style.typography.paragraph, style.paragraph)
        assertEquals(style.inline.inlineCode, style.inlineCode)
        assertEquals(style.code.text, style.codeBlock)
        assertEquals(style.code.languageLabel, style.codeBlockLanguageLabel)
        assertEquals(style.code.borderColor, style.codeBlockBorderColor)
        assertEquals(style.code.borderWidth, style.codeBlockBorderWidth)
        assertEquals(style.table.headerText, style.tableHeaderText)
    }
}
