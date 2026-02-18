package ru.wertik.orca.compose

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

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

class OrcaDefaultsTest {

    @Test
    fun lightStyleIsDefaultOrcaStyle() {
        assertEquals(OrcaStyle(), OrcaDefaults.lightStyle())
    }

    @Test
    fun darkStyleHasDifferentTypographyColorsThanLightStyle() {
        val light = OrcaDefaults.lightStyle()
        val dark = OrcaDefaults.darkStyle()

        assertNotEquals(light.typography.heading1.color, dark.typography.heading1.color)
        assertNotEquals(light.typography.paragraph.color, dark.typography.paragraph.color)
    }

    @Test
    fun darkStyleHasDifferentCodeBackgroundThanLightStyle() {
        val light = OrcaDefaults.lightStyle()
        val dark = OrcaDefaults.darkStyle()

        assertNotEquals(light.code.background, dark.code.background)
    }

    @Test
    fun darkStyleHasDifferentLinkColorThanLightStyle() {
        val light = OrcaDefaults.lightStyle()
        val dark = OrcaDefaults.darkStyle()

        assertNotEquals(light.inline.link.color, dark.inline.link.color)
    }

    @Test
    fun darkStyleHasDifferentTableBorderColorThanLightStyle() {
        val light = OrcaDefaults.lightStyle()
        val dark = OrcaDefaults.darkStyle()

        assertNotEquals(light.table.borderColor, dark.table.borderColor)
    }

    @Test
    fun darkStyleHeadingLevelClamping() {
        val dark = OrcaDefaults.darkStyle()

        assertEquals(dark.typography.heading1, dark.heading(1))
        assertEquals(dark.typography.heading6, dark.heading(6))
        assertEquals(dark.typography.heading1, dark.heading(-1))
        assertEquals(dark.typography.heading6, dark.heading(99))
    }

    @Test
    fun darkStyleCompatibilityAccessorsMirrorGroupedStyles() {
        val dark = OrcaDefaults.darkStyle()

        assertEquals(dark.typography.paragraph, dark.paragraph)
        assertEquals(dark.inline.inlineCode, dark.inlineCode)
        assertEquals(dark.inline.link, dark.linkStyle)
        assertEquals(dark.code.text, dark.codeBlock)
        assertEquals(dark.code.background, dark.codeBlockBackground)
        assertEquals(dark.code.borderColor, dark.codeBlockBorderColor)
        assertEquals(dark.quote.stripeColor, dark.quoteStripeColor)
        assertEquals(dark.table.borderColor, dark.tableBorderColor)
        assertEquals(dark.thematicBreak.color, dark.thematicBreakColor)
        assertEquals(dark.image.background, dark.imageBackground)
    }
}
