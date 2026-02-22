package ru.wertik.orca.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OrcaSuperSubScriptToggleTest {

    @Test
    fun subscriptDisabledKeepsTildeAsText() {
        val parser = OrcaMarkdownParser(enableSubscript = false)
        val result = parser.parse("H~2~O")
        val paragraph = result.blocks.single() as OrcaBlock.Paragraph
        assertTrue(
            "subscript should not be parsed when disabled",
            paragraph.content.none { it is OrcaInline.Subscript },
        )
        val plainText = paragraph.content.filterIsInstance<OrcaInline.Text>()
            .joinToString("") { it.text }
        assertTrue("tilde should remain in text", plainText.contains("~"))
    }

    @Test
    fun superscriptDisabledKeepsCaretAsText() {
        val parser = OrcaMarkdownParser(enableSuperscript = false)
        val result = parser.parse("E = mc^2^")
        val paragraph = result.blocks.single() as OrcaBlock.Paragraph
        assertTrue(
            "superscript should not be parsed when disabled",
            paragraph.content.none { it is OrcaInline.Superscript },
        )
        val plainText = paragraph.content.filterIsInstance<OrcaInline.Text>()
            .joinToString("") { it.text }
        assertTrue("caret should remain in text", plainText.contains("^"))
    }

    @Test
    fun bothDisabledKeepsRawText() {
        val parser = OrcaMarkdownParser(enableSuperscript = false, enableSubscript = false)
        val result = parser.parse("x^2^ + y~i~")
        val paragraph = result.blocks.single() as OrcaBlock.Paragraph
        assertTrue(paragraph.content.none { it is OrcaInline.Superscript })
        assertTrue(paragraph.content.none { it is OrcaInline.Subscript })
    }

    @Test
    fun subscriptDisabledSuperscriptStillWorks() {
        val parser = OrcaMarkdownParser(enableSubscript = false)
        val result = parser.parse("x^2^ + y~i~")
        val paragraph = result.blocks.single() as OrcaBlock.Paragraph
        assertEquals(1, paragraph.content.filterIsInstance<OrcaInline.Superscript>().size)
        assertTrue(paragraph.content.none { it is OrcaInline.Subscript })
    }

    @Test
    fun superscriptDisabledSubscriptStillWorks() {
        val parser = OrcaMarkdownParser(enableSuperscript = false)
        val result = parser.parse("x^2^ + y~i~")
        val paragraph = result.blocks.single() as OrcaBlock.Paragraph
        assertTrue(paragraph.content.none { it is OrcaInline.Superscript })
        assertEquals(1, paragraph.content.filterIsInstance<OrcaInline.Subscript>().size)
    }

    @Test
    fun defaultBehaviorUnchanged() {
        val parser = OrcaMarkdownParser()
        val result = parser.parse("x^2^ + y~i~")
        val paragraph = result.blocks.single() as OrcaBlock.Paragraph
        assertEquals(1, paragraph.content.filterIsInstance<OrcaInline.Superscript>().size)
        assertEquals(1, paragraph.content.filterIsInstance<OrcaInline.Subscript>().size)
    }

    @Test
    fun strikethroughNotAffectedBySubscriptDisabled() {
        val parser = OrcaMarkdownParser(enableSubscript = false)
        val result = parser.parse("~~deleted~~")
        val paragraph = result.blocks.single() as OrcaBlock.Paragraph
        assertTrue(paragraph.content.any { it is OrcaInline.Strikethrough })
        assertTrue(paragraph.content.none { it is OrcaInline.Subscript })
    }
}
