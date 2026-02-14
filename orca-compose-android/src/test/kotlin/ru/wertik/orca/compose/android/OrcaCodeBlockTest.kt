package ru.wertik.orca.compose.android

import org.junit.Assert.assertEquals
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
}
