package ru.wertik.orca.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FrontMatterExtractionTest {

    @Test
    fun `no front matter returns markdown unchanged and null front matter`() {
        val input = "# Hello\n\nSome paragraph."
        val result = extractFrontMatter(input)

        assertEquals("# Hello\n\nSome paragraph.", result.markdown)
        assertNull(result.frontMatter)
    }

    @Test
    fun `yaml front matter with --- delimiter is extracted`() {
        val input = "---\ntitle: My Post\ntags: kotlin\n---\n\n# Body"
        val result = extractFrontMatter(input)

        val fm = result.frontMatter as OrcaFrontMatter.Yaml
        assertEquals("My Post", fm.entries["title"])
        assertEquals("kotlin", fm.entries["tags"])
        assertEquals("# Body", result.markdown)
    }

    @Test
    fun `yaml front matter closed with ... alternative delimiter`() {
        val input = "---\nauthor: Alice\n...\n\nContent here."
        val result = extractFrontMatter(input)

        val fm = result.frontMatter as OrcaFrontMatter.Yaml
        assertEquals("Alice", fm.entries["author"])
        assertEquals("Content here.", result.markdown)
    }

    @Test
    fun `toml front matter with +++ delimiter is extracted`() {
        val input = "+++\ntitle = \"My Post\"\ndraft = \"false\"\n+++\n\nbody text"
        val result = extractFrontMatter(input)

        val fm = result.frontMatter as OrcaFrontMatter.Toml
        assertEquals("My Post", fm.entries["title"])
        assertEquals("false", fm.entries["draft"])
        assertEquals("body text", result.markdown)
    }

    @Test
    fun `front matter with no closing delimiter treated as no front matter`() {
        val input = "---\ntitle: Unclosed\n\n# Body"
        val result = extractFrontMatter(input)

        assertNull(result.frontMatter)
        // markdown is returned normalized but unchanged
        assertTrue(result.markdown.contains("title: Unclosed"))
    }

    @Test
    fun `empty front matter body produces empty entries`() {
        val input = "---\n---\n\nBody text."
        val result = extractFrontMatter(input)

        val fm = result.frontMatter as OrcaFrontMatter.Yaml
        assertTrue(fm.entries.isEmpty())
        assertEquals("Body text.", result.markdown)
    }

    @Test
    fun `front matter with comment lines ignores comments in entries`() {
        val input = "---\n# This is a comment\ntitle: Hello\n---\n\nContent."
        val result = extractFrontMatter(input)

        val fm = result.frontMatter as OrcaFrontMatter.Yaml
        assertNull(fm.entries["# This is a comment"])
        assertEquals("Hello", fm.entries["title"])
        assertEquals("Content.", result.markdown)
    }

    @Test
    fun `front matter with quoted values strips quotes`() {
        val input = "---\ntitle: \"Quoted Title\"\nauthor: 'Alice'\n---\n\nbody"
        val result = extractFrontMatter(input)

        val fm = result.frontMatter as OrcaFrontMatter.Yaml
        assertEquals("Quoted Title", fm.entries["title"])
        assertEquals("Alice", fm.entries["author"])
    }

    @Test
    fun `input with crlf line endings is normalized correctly`() {
        val input = "---\r\ntitle: Windows\r\n---\r\n\r\nBody."
        val result = extractFrontMatter(input)

        val fm = result.frontMatter as OrcaFrontMatter.Yaml
        assertEquals("Windows", fm.entries["title"])
        assertEquals("Body.", result.markdown)
    }

    @Test
    fun `input with cr line endings is normalized correctly`() {
        val input = "---\rtitle: OldMac\r---\r\rBody."
        val result = extractFrontMatter(input)

        val fm = result.frontMatter as OrcaFrontMatter.Yaml
        assertEquals("OldMac", fm.entries["title"])
        assertEquals("Body.", result.markdown)
    }

    @Test
    fun `front matter not at start of document is treated as no front matter`() {
        val input = "Some text first.\n---\ntitle: Late\n---\n\nBody."
        val result = extractFrontMatter(input)

        assertNull(result.frontMatter)
        assertTrue(result.markdown.contains("Some text first."))
    }

    @Test
    fun `yaml front matter raw field contains raw lines`() {
        val input = "---\ntitle: Raw\ndate: 2024-01-01\n---\n\nbody"
        val result = extractFrontMatter(input)

        val fm = result.frontMatter as OrcaFrontMatter.Yaml
        assertTrue(fm.raw.contains("title: Raw"))
        assertTrue(fm.raw.contains("date: 2024-01-01"))
    }

    @Test
    fun `toml front matter raw field contains raw lines`() {
        val input = "+++\ntitle = \"TOML\"\n+++\n\nbody"
        val result = extractFrontMatter(input)

        val fm = result.frontMatter as OrcaFrontMatter.Toml
        assertTrue(fm.raw.contains("title = \"TOML\""))
    }

    @Test
    fun `front matter body has leading blank line stripped`() {
        val input = "---\ntitle: Hello\n---\n\n# Heading"
        val result = extractFrontMatter(input)

        // The blank line between closing delimiter and body should not be included
        assertEquals("# Heading", result.markdown)
    }

    @Test
    fun `front matter with multiple entries preserves order`() {
        val input = "---\nalpha: 1\nbeta: 2\ngamma: 3\n---\n\nbody"
        val result = extractFrontMatter(input)

        val fm = result.frontMatter as OrcaFrontMatter.Yaml
        val keys = fm.entries.keys.toList()
        assertEquals(listOf("alpha", "beta", "gamma"), keys)
    }
}
