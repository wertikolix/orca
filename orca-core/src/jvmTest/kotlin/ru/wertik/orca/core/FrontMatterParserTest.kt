package ru.wertik.orca.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FrontMatterParserTest {

    private val parser = OrcaMarkdownParser()

    @Test
    fun noFrontMatterReturnsNullFrontMatter() {
        val result = parser.parseWithDiagnostics("# Hello\n\nSome paragraph.")
        assertNull(result.document.frontMatter)
    }

    @Test
    fun yamlFrontMatterIsExtracted() {
        val input = "---\ntitle: My Post\ntags: kotlin\n---\n\n# Body"
        val result = parser.parseWithDiagnostics(input)

        val fm = result.document.frontMatter as OrcaFrontMatter.Yaml
        assertEquals("My Post", fm.entries["title"])
        assertEquals("kotlin", fm.entries["tags"])
    }

    @Test
    fun tomlFrontMatterIsExtracted() {
        val input = "+++\ntitle = \"My Post\"\ndraft = \"false\"\n+++\n\nbody text"
        val result = parser.parseWithDiagnostics(input)

        val fm = result.document.frontMatter as OrcaFrontMatter.Toml
        assertEquals("My Post", fm.entries["title"])
        assertEquals("false", fm.entries["draft"])
    }

    @Test
    fun frontMatterNotAtStartIsIgnored() {
        val input = "Some text first.\n---\ntitle: Late\n---\n\nBody."
        val result = parser.parseWithDiagnostics(input)
        assertNull(result.document.frontMatter)
    }

    @Test
    fun emptyFrontMatterBodyProducesEmptyEntries() {
        val input = "---\n---\n\nBody text."
        val result = parser.parseWithDiagnostics(input)

        val fm = result.document.frontMatter as OrcaFrontMatter.Yaml
        assertTrue(fm.entries.isEmpty())
    }

    @Test
    fun yamlFrontMatterRawFieldContainsRawLines() {
        val input = "---\ntitle: Raw\ndate: 2024-01-01\n---\n\nbody"
        val result = parser.parseWithDiagnostics(input)

        val fm = result.document.frontMatter as OrcaFrontMatter.Yaml
        assertTrue(fm.raw.contains("title: Raw"))
        assertTrue(fm.raw.contains("date: 2024-01-01"))
    }

    @Test
    fun tomlFrontMatterRawFieldContainsRawLines() {
        val input = "+++\ntitle = \"TOML\"\n+++\n\nbody"
        val result = parser.parseWithDiagnostics(input)

        val fm = result.document.frontMatter as OrcaFrontMatter.Toml
        assertTrue(fm.raw.contains("title = \"TOML\""))
    }

    @Test
    fun frontMatterBodyIsNotIncludedInBlocks() {
        val input = "---\ntitle: Hello\n---\n\n# Heading"
        val result = parser.parseWithDiagnostics(input)

        val heading = result.document.blocks.filterIsInstance<OrcaBlock.Heading>().firstOrNull()
        assertTrue(heading != null)
        // front matter title should not appear as a paragraph block
        val paragraphs = result.document.blocks.filterIsInstance<OrcaBlock.Paragraph>()
        assertTrue(paragraphs.none { p -> p.content.any { it is OrcaInline.Text && it.text.contains("title") } })
    }
}
