package ru.wertik.orca.compose

import kotlin.test.Test
import kotlin.test.assertEquals

class OrcaStyleTest {

    @Test
    fun typographyHeadingLevelsAreClampedToSupportedRange() {
        val style = OrcaStyle()

        assertEquals(style.typography.heading1, style.heading(-10))
        assertEquals(style.typography.heading1, style.heading(1))
        assertEquals(style.typography.heading4, style.heading(4))
        assertEquals(style.typography.heading6, style.heading(6))
        assertEquals(style.typography.heading6, style.heading(42))
    }

    @Test
    fun compatibilityAccessorsStillMirrorGroupedStyles() {
        val style = OrcaStyle()

        assertEquals(style.typography.paragraph, style.paragraph)
        assertEquals(style.inline.inlineCode, style.inlineCode)
        assertEquals(style.inline.footnoteReference, style.footnoteReferenceStyle)
        assertEquals(style.code.text, style.codeBlock)
        assertEquals(style.code.lineNumber, style.codeBlockLineNumber)
        assertEquals(style.code.languageLabel, style.codeBlockLanguageLabel)
        assertEquals(style.code.showLineNumbers, style.codeBlockShowLineNumbers)
        assertEquals(style.code.syntaxHighlightingEnabled, style.codeBlockSyntaxHighlightingEnabled)
        assertEquals(style.code.highlightKeyword, style.codeBlockHighlightKeyword)
        assertEquals(style.code.highlightString, style.codeBlockHighlightString)
        assertEquals(style.code.highlightComment, style.codeBlockHighlightComment)
        assertEquals(style.code.highlightNumber, style.codeBlockHighlightNumber)
        assertEquals(style.code.borderColor, style.codeBlockBorderColor)
        assertEquals(style.code.borderWidth, style.codeBlockBorderWidth)
        assertEquals(style.table.headerText, style.tableHeaderText)
        assertEquals(style.table.layoutMode, style.tableLayoutMode)
        assertEquals(style.table.minColumnWidth, style.tableMinColumnWidth)
        assertEquals(style.table.maxColumnWidth, style.tableMaxColumnWidth)
        assertEquals(style.table.autoColumnCharacterWidth, style.tableAutoColumnCharacterWidth)
        assertEquals(style.table.fillAvailableWidth, style.tableFillAvailableWidth)
    }
}
