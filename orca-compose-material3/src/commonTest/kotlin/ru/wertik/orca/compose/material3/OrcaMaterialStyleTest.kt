package ru.wertik.orca.compose.material3

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

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
        assertEquals(colors.surface, style.code.background)
        assertEquals(colors.surfaceVariant, style.table.headerBackground)
        assertEquals(colors.outlineVariant, style.table.outerBorderColor)
        assertEquals(colors.primary, style.admonition.noteColor)
    }
}
