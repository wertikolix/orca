package ru.wertik.orca.compose.material3

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import ru.wertik.orca.compose.OrcaDensity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OrcaMaterialStyleTest {

    @Test
    fun materialStyleUsesColorSchemeTokens() {
        val colors = lightColorScheme(
            primary = Color(0xFF0057B8),
            surface = Color(0xFFFFFBFE),
            surfaceVariant = Color(0xFFE7E0EC),
            outlineVariant = Color(0xFFCAC4D0),
        )

        val style = ru.wertik.orca.compose.OrcaDefaults.materialStyle(colors)

        assertEquals(colors.primary, style.inline.link.color)
        assertEquals(colors.surfaceContainerLow, style.code.background)
        assertEquals(colors.surfaceContainerHigh, style.table.headerBackground)
        assertEquals(colors.outlineVariant, style.table.outerBorderColor)
        assertEquals(colors.primary, style.admonition.noteColor)
        assertEquals(colors.outlineVariant, style.quote.borderColor)
        assertEquals(colors.primary, style.task.checkedBackground)
        assertEquals(colors.primary, style.table.scrollIndicatorColor)
        assertEquals(colors.surfaceContainer, style.quote.background)
        assertEquals(colors.onSurface, style.typography.paragraph.color)
    }

    @Test
    fun materialStyleHonoursDensityAndHeadingRuleOptions() {
        val colors = lightColorScheme()

        val comfortable = ru.wertik.orca.compose.OrcaDefaults.materialStyle(colors)
        val compact = ru.wertik.orca.compose.OrcaDefaults.materialStyle(
            colorScheme = colors,
            density = OrcaDensity.COMPACT,
            headingRules = false,
        )

        assertTrue(compact.layout.blockSpacing < comfortable.layout.blockSpacing)
        assertTrue(comfortable.headingRule.hasRule(1))
        assertFalse(compact.headingRule.hasRule(1))
    }

    @Test
    fun materialPaletteFlagsDarkSchemesFromBackgroundLuminance() {
        val palette = ru.wertik.orca.compose.OrcaDefaults.materialPalette(darkColorScheme())

        assertTrue(palette.isDark)
        assertFalse(ru.wertik.orca.compose.OrcaDefaults.materialPalette(lightColorScheme()).isDark)
    }
}
