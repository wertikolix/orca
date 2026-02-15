package ru.wertik.orca.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class OrcaParserTest {

    @Test
    fun `default cache key uses parser class`() {
        val parser = OrcaParser { OrcaDocument(emptyList()) }

        assertEquals(parser::class, parser.cacheKey())
    }

    @Test
    fun `custom parser may provide custom cache key`() {
        val expectedKey = "custom-parser-v2"
        val parser = object : OrcaParser {
            override fun parse(input: String): OrcaDocument = OrcaDocument(emptyList())

            override fun cacheKey(): Any = expectedKey
        }

        assertSame(expectedKey, parser.cacheKey())
    }
}
