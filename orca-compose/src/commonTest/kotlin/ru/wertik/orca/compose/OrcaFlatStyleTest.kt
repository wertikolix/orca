package ru.wertik.orca.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class OrcaFlatStyleTest {

    @Test
    fun flatStyleMapsPaletteTokensOntoEveryContainer() {
        val palette = OrcaPalettes.FlatLight
        val style = orcaFlatStyle(palette)

        assertEquals(palette.text, style.typography.paragraph.color)
        assertEquals(palette.accent, style.inline.link.color)
        assertEquals(palette.surface, style.quote.background)
        assertEquals(palette.outlineMuted, style.quote.borderColor)
        assertEquals(palette.syntax.surface, style.code.background)
        assertEquals(palette.syntax.keyword, style.code.highlightKeyword.color)
        assertEquals(palette.surfaceMuted, style.table.headerBackground)
        assertEquals(palette.background, style.table.rowBackground)
        assertEquals(palette.surface, style.table.alternateRowBackground)
        assertEquals(palette.signal.note, style.admonition.noteColor)
        assertEquals(palette.accent, style.task.checkedBackground)
        assertEquals(palette.onAccent, style.task.checkColor)
        assertEquals(palette.searchMatch, style.inline.searchMatch.background)
    }

    @Test
    fun densityOnlyScalesSpacing() {
        val compact = orcaFlatStyle(OrcaPalettes.FlatLight, density = OrcaDensity.COMPACT)
        val comfortable = orcaFlatStyle(OrcaPalettes.FlatLight, density = OrcaDensity.COMFORTABLE)
        val spacious = orcaFlatStyle(OrcaPalettes.FlatLight, density = OrcaDensity.SPACIOUS)

        assertTrue(compact.layout.blockSpacing < comfortable.layout.blockSpacing)
        assertTrue(spacious.layout.blockSpacing > comfortable.layout.blockSpacing)
        assertEquals(comfortable.typography.paragraph, compact.typography.paragraph)
        assertEquals(comfortable.code.text, spacious.code.text)
    }

    @Test
    fun headingRulesAreEnabledForTopLevelsAndCanBeDisabled() {
        val withRules = orcaFlatStyle(OrcaPalettes.FlatLight)
        val withoutRules = orcaFlatStyle(OrcaPalettes.FlatLight, headingRuleLevels = emptySet())

        assertTrue(withRules.headingRule.hasRule(1))
        assertTrue(withRules.headingRule.hasRule(2))
        assertFalse(withRules.headingRule.hasRule(3))
        assertFalse(withoutRules.headingRule.hasRule(1))
        assertEquals(OrcaPalettes.FlatLight.outlineMuted, withRules.headingRule.color)
    }

    @Test
    fun zebraTablesCanBeDisabled() {
        val flat = orcaFlatStyle(OrcaPalettes.FlatLight, zebraTables = false)

        assertEquals(flat.table.rowBackground, flat.table.alternateRowBackground)
    }

    @Test
    fun contrastPalettesRaiseSeparationAgainstFlatPalettes() {
        assertNotEquals(OrcaPalettes.FlatLight.text, OrcaPalettes.ContrastLight.text)
        assertNotEquals(OrcaPalettes.FlatDark.background, OrcaPalettes.ContrastDark.background)
        assertEquals(Color(0xFF000000), OrcaPalettes.ContrastLight.text)
        assertEquals(Color(0xFFFFFFFF), OrcaPalettes.ContrastDark.text)
        assertTrue(OrcaPalettes.ContrastDark.isDark)
        assertFalse(OrcaPalettes.ContrastLight.isDark)
    }

    @Test
    fun defaultsExposeFlatAndLegacyStyles() {
        assertEquals(orcaFlatStyle(OrcaPalettes.FlatLight), OrcaDefaults.lightStyle())
        assertEquals(orcaFlatStyle(OrcaPalettes.FlatDark), OrcaDefaults.darkStyle())
        assertEquals(orcaFlatStyle(OrcaPalettes.ContrastLight), OrcaDefaults.contrastLightStyle())
        assertEquals(orcaFlatStyle(OrcaPalettes.ContrastDark), OrcaDefaults.contrastDarkStyle())
        assertEquals(OrcaStyle(), OrcaDefaults.legacyLightStyle())
        assertNotEquals(OrcaDefaults.legacyLightStyle(), OrcaDefaults.lightStyle())
        assertNotEquals(OrcaDefaults.legacyDarkStyle(), OrcaDefaults.darkStyle())
    }

    @Test
    fun flatStylesNeverUseTransparentTextOrElevationSurfaces() {
        listOf(
            OrcaDefaults.lightStyle(),
            OrcaDefaults.darkStyle(),
            OrcaDefaults.contrastLightStyle(),
            OrcaDefaults.contrastDarkStyle(),
        ).forEach { style ->
            assertNotEquals(Color.Transparent, style.typography.paragraph.color)
            assertNotEquals(Color.Transparent, style.code.background)
            assertNotEquals(Color.Unspecified, style.table.headerText.color)
            assertEquals(1.dp, style.quote.borderWidth)
            assertEquals(1.dp, style.code.borderWidth)
            assertEquals(1.dp, style.admonition.borderWidth)
        }
    }
}
